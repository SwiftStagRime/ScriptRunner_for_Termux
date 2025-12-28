package io.github.swiftstagrime.termuxrunner.domain.usecase

import android.util.Base64
import io.github.swiftstagrime.termuxrunner.domain.model.Script
import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptFileRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.TermuxRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Use case to prepare and execute a script within the Termux environment.
 */
class RunScriptUseCase @Inject constructor(
    private val termuxRepository: TermuxRepository,
    private val scriptFileRepository: ScriptFileRepository
) {
    suspend operator fun invoke(script: Script) = withContext(Dispatchers.IO) {
        // Sanitize and format environment variables for shell export
        val envVarString = StringBuilder()
        script.envVars.forEach { (key, value) ->
            if (key.matches(Regex("^[a-zA-Z_][a-zA-Z0-9_]*$"))) {
                val safeValue = value.replace("'", "'\\''")
                envVarString.append("export $key='$safeValue'; ")
            }
        }

        // Determine file extension based on the selected interpreter
        val extension = if (script.fileExtension.isNotBlank()) {
            script.fileExtension.trim().removePrefix(".")
        } else {
            when (script.interpreter) {
                "python", "python3" -> "py"
                "node", "nodejs" -> "js"
                "perl" -> "pl"
                "ruby" -> "rb"
                else -> "sh"
            }
        }

        val uniqueId = "${script.id}_${System.currentTimeMillis()}"
        val fileName = "script_$uniqueId.$extension"

        // Use a 4KB threshold to decide between Base64 embedding or external file bridging
        // to avoid Binder transaction size limits in Android Intents.
        val isLargeScript = script.code.length > 4000

        val finalCommand = if (isLargeScript) {
            prepareLargeScriptCommand(script, fileName, envVarString.toString())
        } else {
            prepareSmallScriptCommand(script, fileName, envVarString.toString())
        }

        termuxRepository.runCommand(
            command = finalCommand,
            runInBackground = script.runInBackground,
            sessionAction = "0"
        )
    }

    private fun prepareSmallScriptCommand(
        script: Script,
        fileName: String,
        envVars: String
    ): String {
        val tempDir = "~/scriptrunner_for_termux"
        val fullPath = "$tempDir/$fileName"
        // Embed the script directly in the command as a Base64 string
        val encodedCode = Base64.encodeToString(script.code.toByteArray(), Base64.NO_WRAP)

        val runCmd = StringBuilder()
            .append(if (script.commandPrefix.isNotBlank()) "${script.commandPrefix} " else "")
            .append("${script.interpreter} ")
            .append("$fullPath ")
            .append(script.executionParams)
            .toString()

        return StringBuilder()
            .append("mkdir -p $tempDir && ")
            .append("echo '$encodedCode' | base64 -d > $fullPath && ")
            .append("chmod +x $fullPath && ")
            .append("(")
            .append(envVars)
            .append(runCmd)
            .append("); ")
            .append("rm -f $fullPath")
            .apply {
                if (script.keepSessionOpen) {
                    // Prevent session closure by waiting for input and restarting the shell
                    append($$"; echo; echo '--- Finished (Press Enter) ---'; read; exec $SHELL")
                }
            }
            .toString()
    }

    private fun prepareLargeScriptCommand(
        script: Script,
        fileName: String,
        envVars: String
    ): String {
        // Save script to a bridge directory that Termux can access via 'termux-setup-storage'
        val termuxSourcePath = try {
            scriptFileRepository.saveToBridge(fileName, script.code)
        } catch (e: Exception) {
            e.printStackTrace()
            return "echo 'Error: Could not save script to device storage.'"
        }

        val termuxDestPath = "~/scriptrunner_for_termux/$fileName"

        val runCmd = StringBuilder()
            .append(if (script.commandPrefix.isNotBlank()) "${script.commandPrefix} " else "")
            .append("${script.interpreter} ")
            .append("$termuxDestPath ")
            .append(script.executionParams)
            .toString()

        return StringBuilder()
            .append("mkdir -p ~/scriptrunner_for_termux && ")
            .append("cp -f $termuxSourcePath $termuxDestPath && ")
            .append("chmod +x $termuxDestPath && ")
            .append("(")
            .append(envVars)
            .append(runCmd)
            .append("); ")
            .append("rm -f $termuxDestPath")
            .apply {
                if (script.keepSessionOpen) {
                    append($$"; echo; echo '--- Finished (Press Enter) ---'; read; exec $SHELL")
                }
            }
            .toString()
    }
}