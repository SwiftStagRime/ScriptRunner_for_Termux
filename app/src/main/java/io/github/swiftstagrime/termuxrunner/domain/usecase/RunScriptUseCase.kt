package io.github.swiftstagrime.termuxrunner.domain.usecase

import android.util.Base64
import io.github.swiftstagrime.termuxrunner.domain.model.Script
import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptFileRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.TermuxRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class RunScriptUseCase @Inject constructor(
    private val termuxRepository: TermuxRepository,
    private val scriptFileRepository: ScriptFileRepository
) {
    suspend operator fun invoke(script: Script) = withContext(Dispatchers.IO) {
        val envVarString = StringBuilder()
        script.envVars.forEach { (key, value) ->
            if (key.matches(Regex("^[a-zA-Z_][a-zA-Z0-9_]*$"))) {
                val safeValue = value.replace("'", "'\\''")
                envVarString.append("export $key='$safeValue'; ")
            }
        }

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
        val tempDir = "~/termux_runner_scripts"
        val fullPath = "$tempDir/$fileName"
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
        val termuxSourcePath = try {
            scriptFileRepository.saveToBridge(fileName, script.code)
        } catch (e: Exception) {
            e.printStackTrace()
            return "echo 'Error: Could not save script to device storage.'"
        }

        val termuxDestPath = "~/termux_runner_scripts/$fileName"

        val runCmd = StringBuilder()
            .append(if (script.commandPrefix.isNotBlank()) "${script.commandPrefix} " else "")
            .append("${script.interpreter} ")
            .append("$termuxDestPath ")
            .append(script.executionParams)
            .toString()

        return StringBuilder()
            .append("mkdir -p ~/termux_runner_scripts && ")
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