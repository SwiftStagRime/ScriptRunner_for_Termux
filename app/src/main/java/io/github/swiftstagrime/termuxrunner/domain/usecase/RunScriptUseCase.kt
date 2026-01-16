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
    suspend operator fun invoke(
        script: Script,
        runtimeArgs: String? = null,
        runtimeEnv: Map<String, String>? = null,
        runtimePrefix: String? = null,
        automationId: Int? = null
    ) = withContext(Dispatchers.IO) {
        // Sanitize and format environment variables
        val combinedEnv = script.envVars + (runtimeEnv ?: emptyMap())

        val envVarString = StringBuilder()
        combinedEnv.forEach { (key, value) ->
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

        val finalPrefix = (runtimePrefix ?: script.commandPrefix).trim()
        val finalArgs = listOfNotNull(
            script.executionParams.takeIf { it.isNotBlank() },
            runtimeArgs?.trim()?.takeIf { it.isNotBlank() }
        ).joinToString(" ")

        // Use a 4KB threshold to decide between Base64 embedding or external file bridging
        // to avoid Binder transaction size limits in Android Intents.
        val isLargeScript = script.code.length > 4000

        val finalCommand = if (isLargeScript) {
            prepareLargeScriptCommand(
                script,
                fileName,
                envVarString.toString(),
                finalArgs,
                finalPrefix
            )
        } else {
            prepareSmallScriptCommand(
                script,
                fileName,
                envVarString.toString(),
                finalArgs,
                finalPrefix
            )
        }

        // Execute in Termux
        termuxRepository.runCommand(
            command = finalCommand,
            runInBackground = script.runInBackground,
            sessionAction = "1",
            scriptId = script.id,
            scriptName = script.name,
            notifyOnResult = script.notifyOnResult,
            automationId = automationId
        )

        // Manage Heartbeat Service
        if (script.useHeartbeat && monitoringRepository.hasNotificationPermission()) {
            monitoringRepository.startMonitoring(script)
        }
    }

    private fun prepareSmallScriptCommand(
        script: Script,
        fileName: String,
        envVars: String,
        combinedArgs: String,
        actualPrefix: String
    ): String {
        val tempDir = "~/scriptrunner_for_termux"
        val fullPath = "$tempDir/$fileName"
        val encodedCode = Base64.encodeToString(script.code.toByteArray(), Base64.NO_WRAP)

        val coreExecution = StringBuilder().apply {
            append(envVars)
            if (actualPrefix.isNotBlank()) append("$actualPrefix ")
            append("${script.interpreter} ")
            append("$fullPath ")
            append(combinedArgs)
        }.toString()

        // Wrap with heartbeat if enabled
        val runBlock = if (script.useHeartbeat) {
            wrapCommandWithHeartbeat(coreExecution, script.heartbeatInterval, script.id)
        } else {
            coreExecution
        }

        return StringBuilder()
            .append("mkdir -p $tempDir && ")
            .append("echo '$encodedCode' | base64 -d > $fullPath && ")
            .append("chmod +x $fullPath && ")
            .append("bash -c \"")
            .append("trap 'rm -f $fullPath' EXIT; ")
            .append(runBlock.replace("\"", "\\\""))
            .append("\"")
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
        envVars: String,
        combinedArgs: String,
        actualPrefix: String
    ): String {
        val termuxSourcePath = try {
            scriptFileRepository.saveToBridge(fileName, script.code)
        } catch (_: Exception) {
            return "echo 'Error: Could not save script to device storage.'"
        }
        val termuxDestPath = "~/scriptrunner_for_termux/$fileName"

        val coreExecution = StringBuilder().apply {
            append(envVars)
            if (actualPrefix.isNotBlank()) append("$actualPrefix ")
            append("${script.interpreter} ")
            append("$termuxDestPath ")
            append(combinedArgs)
        }.toString()

        // Wrap with heartbeat if enabled
        val runBlock = if (script.useHeartbeat) {
            wrapCommandWithHeartbeat(coreExecution, script.heartbeatInterval, script.id)
        } else {
            "($coreExecution)"
        }

        return StringBuilder()
            .append("mkdir -p ~/scriptrunner_for_termux && ")
            .append("cp -f $termuxSourcePath $termuxDestPath && ")
            .append("{ rm -f \"$termuxSourcePath\" || true; } && ")
            .append("chmod +x $termuxDestPath && ")
            .append("bash -c \"")
            .append("trap 'rm -f $termuxDestPath' EXIT; ")
            .append(runBlock.replace("\"", "\\\""))
            .append("\"")
            .apply {
                if (script.keepSessionOpen) {
                    append($$"; echo; echo '--- Finished (Press Enter) ---'; read; exec $SHELL")
                }
            }
            .toString()
    }

    // Another trick to try and force required behaviour, I do hope that passing as a wrapper will work
    // Does just fine with adb killing the process
    private fun wrapCommandWithHeartbeat(
        commandToRun: String,
        intervalMs: Long,
        scriptId: Int
    ): String {
        val heartbeatAction = "io.github.swiftstagrime.HEARTBEAT"
        val finishedAction = "io.github.swiftstagrime.SCRIPT_FINISHED"
        val intervalSeconds = (intervalMs / 1000).coerceAtLeast(5)

        return $$"""
    (
      (
        while true; do
          # ADDED: --ei script_id to identify which script is pulsing
          am broadcast -a $$heartbeatAction --ei script_id $$scriptId > /dev/null 2>&1
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
      # ADDED: --ei script_id here as well so the service knows which one finished
      am broadcast -a $$finishedAction --ei exit_code $EXIT_CODE --ei script_id $$scriptId > /dev/null 2>&1
    )
    """.trimIndent()
    }
}