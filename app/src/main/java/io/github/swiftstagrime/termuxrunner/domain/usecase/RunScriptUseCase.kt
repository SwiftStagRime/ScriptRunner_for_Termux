package io.github.swiftstagrime.termuxrunner.domain.usecase

import android.util.Base64
import io.github.swiftstagrime.termuxrunner.domain.model.Script
import io.github.swiftstagrime.termuxrunner.domain.repository.MonitoringRepository
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
    private val scriptFileRepository: ScriptFileRepository,
    private val monitoringRepository: MonitoringRepository
) {
    suspend operator fun invoke(script: Script) = withContext(Dispatchers.IO) {
        // Sanitize and format environment variables
        val envVarString = StringBuilder()
        script.envVars.forEach { (key, value) ->
            if (key.matches(Regex("^[a-zA-Z_][a-zA-Z0-9_]*$"))) {
                val safeValue = value.replace("'", "'\\''")
                envVarString.append("export $key='$safeValue'; ")
            }
        }

        // Determine file extension
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

        // Execute in Termux
        termuxRepository.runCommand(
            command = finalCommand,
            runInBackground = script.runInBackground,
            sessionAction = "0"
        )

        // Manage Heartbeat Service
        if (script.useHeartbeat && monitoringRepository.hasNotificationPermission()) {
            monitoringRepository.startMonitoring(script)
        }
    }

    private fun prepareSmallScriptCommand(
        script: Script,
        fileName: String,
        envVars: String
    ): String {
        val tempDir = "~/scriptrunner_for_termux"
        val fullPath = "$tempDir/$fileName"
        val encodedCode = Base64.encodeToString(script.code.toByteArray(), Base64.NO_WRAP)

        // Construct the core execution line: [EnvVars] [Prefix] [Interpreter] [Path] [Args]
        val coreExecution = StringBuilder().apply {
            append(envVars)
            if (script.commandPrefix.isNotBlank()) append("${script.commandPrefix} ")
            append("${script.interpreter} ")
            append("$fullPath ")
            append(script.executionParams)
        }.toString()

        // Wrap with heartbeat if enabled
        val finalRunBlock = if (script.useHeartbeat) {
            wrapCommandWithHeartbeat(coreExecution, script.heartbeatInterval)
        } else {
            "($coreExecution)"
        }

        return StringBuilder()
            .append("mkdir -p $tempDir && ")
            .append("echo '$encodedCode' | base64 -d > $fullPath && ")
            .append("chmod +x $fullPath && ")
            .append(finalRunBlock)
            .append("; rm -f $fullPath")
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
        // Save script to a bridge directory that Termux can access via 'termux-setup-storage'
        val termuxSourcePath = try {
            scriptFileRepository.saveToBridge(fileName, script.code)
        } catch (_: Exception) {
            return "echo 'Error: Could not save script to device storage.'"
        }

        val termuxDestPath = "~/scriptrunner_for_termux/$fileName"

        val coreExecution = StringBuilder().apply {
            append(envVars)
            if (script.commandPrefix.isNotBlank()) append("${script.commandPrefix} ")
            append("${script.interpreter} ")
            append("$termuxDestPath ")
            append(script.executionParams)
        }.toString()

        val finalRunBlock = if (script.useHeartbeat) {
            wrapCommandWithHeartbeat(coreExecution, script.heartbeatInterval)
        } else {
            "($coreExecution)"
        }

        return StringBuilder()
            .append("mkdir -p ~/scriptrunner_for_termux && ")
            .append("cp -f $termuxSourcePath $termuxDestPath && ")
            .append("chmod +x $termuxDestPath && ")
            .append(finalRunBlock)
            .append("; rm -f $termuxDestPath")
            .apply {
                if (script.keepSessionOpen) {
                    append($$"; echo; echo '--- Finished (Press Enter) ---'; read; exec $SHELL")
                }
            }
            .toString()
    }

    // Another trick to try and force required behaviour, I do hope that passing as a wrapper will work
    // Does just fine with adb killing the process
    private fun wrapCommandWithHeartbeat(commandToRun: String, intervalMs: Long): String {
        val heartbeatAction = "io.github.swiftstagrime.HEARTBEAT"
        val finishedAction = "io.github.swiftstagrime.SCRIPT_FINISHED"
        val intervalSeconds = (intervalMs / 1000).coerceAtLeast(1)

        return $$"""
        (
          (
            while true; do
              am broadcast -a $$heartbeatAction > /dev/null 2>&1
              sleep $$intervalSeconds
            done
          ) &
          HEARTBEAT_PID=$!
          
          cleanup_heartbeat() {
            kill $HEARTBEAT_PID > /dev/null 2>&1
          }
          trap cleanup_heartbeat EXIT
          
          ( $$commandToRun )
          EXIT_CODE=$?
          
          cleanup_heartbeat
          am broadcast -a $$finishedAction --ei exit_code $EXIT_CODE > /dev/null 2>&1
        )
        """.trimIndent()
    }
}