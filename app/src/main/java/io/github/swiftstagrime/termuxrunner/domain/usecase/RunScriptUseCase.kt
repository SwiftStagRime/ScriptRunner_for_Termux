package io.github.swiftstagrime.termuxrunner.domain.usecase

import android.util.Base64
import io.github.swiftstagrime.termuxrunner.di.PackageName
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
class RunScriptUseCase
    @Inject
    constructor(
        @PackageName private val packageName: String,
        private val termuxRepository: TermuxRepository,
        private val scriptFileRepository: ScriptFileRepository,
        private val monitoringRepository: MonitoringRepository,
    ) {

    companion object {
        private val VALID_ENV_KEY_PATTERN = Regex("^[a-zA-Z_][a-zA-Z0-9_]*$")
        private val SAFE_INTERPRETER_PATTERN = Regex("^[a-z0-9_/.-]+$", RegexOption.IGNORE_CASE)
        private val SAFE_PREFIX_PATTERN = Regex("^[a-zA-Z0-9_./\\s:;@|&<>()\\[\\]{}'\"$`,!%^#=-+*?~]*$")

        /** Escapes a string for safe placement inside bash -c "..." double-quoted context. */
        private fun escapeForBashDoubleQuotes(input: String): String = input
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("$", "\\$")
            .replace("`", "\\`")
    }
        suspend operator fun invoke(
            script: Script,
            runtimeArgs: String? = null,
            runtimeEnv: Map<String, String>? = null,
            runtimePrefix: String? = null,
            automationId: Int? = null,
        ) = withContext(Dispatchers.IO) {
            // Sanitize and format environment variables
            val combinedEnv = script.envVars + (runtimeEnv ?: emptyMap())

            val envVarString = StringBuilder()
            combinedEnv.forEach { (key, value) ->
                if (key.matches(VALID_ENV_KEY_PATTERN)) {
                    val safeValue = value.replace("\\", "\\\\").replace("'", "'\\''").replace("$", "\\$").replace("`", "\\`")
                    envVarString.append("export $key='$safeValue'; ")
                }
            }

            // Determine file extension
            val extension =
                if (script.fileExtension.isNotBlank()) {
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
            val finalArgs =
                listOfNotNull(
                    script.executionParams.takeIf { it.isNotBlank() },
                    runtimeArgs?.trim()?.takeIf { it.isNotBlank() },
                ).joinToString(" ")

            // Use a 4KB threshold to decide between Base64 embedding or external file bridging
            // to avoid Binder transaction size limits in Android Intents.
            val isLargeScript = script.code.length > 4000

            val finalCommand =
                if (isLargeScript) {
                    prepareLargeScriptCommand(
                        script,
                        fileName,
                        envVarString.toString(),
                        finalArgs,
                        finalPrefix,
                    )
                } else {
                    prepareSmallScriptCommand(
                        script,
                        fileName,
                        envVarString.toString(),
                        finalArgs,
                        finalPrefix,
                    )
                }

            val port: Int? =
                if (script.useHeartbeat && monitoringRepository.hasNotificationPermission()) {
                    monitoringRepository.startMonitoring(script)
                } else {
                    null
                }

            val wrappedCommand =
                if (port != null) {
                    wrapCommandWithTcpMonitor(finalCommand, port)
                } else if (script.useHeartbeat) {
                    wrapCommandWithHeartbeat(finalCommand, script.heartbeatInterval, script.id)
                } else {
                    finalCommand
                }

            // Execute in Termux with wrapped command
            termuxRepository.runCommand(
                command = wrappedCommand,
                runInBackground = script.runInBackground,
                sessionAction = "1",
                scriptId = script.id,
                scriptName = script.name,
                notifyOnResult = script.notifyOnResult,
                automationId = automationId,
            )
        }

        private fun wrapCommandWithTcpMonitor(
            commandToRun: String,
            port: Int,
        ): String {
            // TCP wrapper using Python to hold a connection open.
            // Python connects to the Android app's TCP server and waits for stdin to close.
            // When the script finishes normally, we close the pipe (fd 3),
            // which causes Python's sys.stdin.read() to return, sending "EXIT_OK".
            // If the process is killed (SIGKILL/OOM), the TCP connection drops without EXIT_OK.
            val dollar = "$"
            return """
    (
      # Create a pipe for signaling script completion to Python
      exec 3< <(sleep infinity)

      # Python TCP holder - connects to Android app and waits for fd 3 to close
      python3 -c "
import socket, sys
try:
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.connect(('127.0.0.1', $port))
    sys.stdin.read()  # Wait for pipe to close
    s.sendall(b'EXIT_OK\\n')
    s.close()
except:
    pass
" <&3 &
      PYTHON_PID=$dollar!

      # Run the actual script
      ( $commandToRun )

      # Close fd 3 to signal Python that we finished normally
      exec 3>&-
      wait ${dollar}PYTHON_PID 2>/dev/null
    )
                """.trimIndent()
        }

        private fun prepareSmallScriptCommand(
            script: Script,
            fileName: String,
            envVars: String,
            combinedArgs: String,
            actualPrefix: String,
        ): String {
            val tempDir = "~/scriptrunner_for_termux"
            val fullPath = "$tempDir/$fileName"
            val encodedCode = Base64.encodeToString(script.code.toByteArray(), Base64.NO_WRAP)

            if (!script.interpreter.matches(SAFE_INTERPRETER_PATTERN)) {
                return "echo 'Error: Invalid interpreter specified.'"
            }

            val escapedInterpreter = escapeForBashDoubleQuotes(script.interpreter)
            val escapedPrefix = if (actualPrefix.isNotBlank()) escapeForBashDoubleQuotes(actualPrefix) + " " else ""
            val escapedArgs = escapeForBashDoubleQuotes(combinedArgs)
            val escapedEnvVars = escapeForBashDoubleQuotes(envVars)

            val coreExecution = "$escapedEnvVars$escapedPrefix$escapedInterpreter $fullPath $escapedArgs"

            return StringBuilder()
                .append("mkdir -p $tempDir && ")
                .append("echo '$encodedCode' | base64 -d > $fullPath && ")
                .append("chmod +x $fullPath && ")
                .append("bash -c \"")
                .append("trap 'rm -f $fullPath' EXIT; ")
                .append(coreExecution)
                .append("\"")
                .apply {
                    if (script.keepSessionOpen) {
                        append($$"; echo; echo '--- Finished (Press Enter) ---'; read; exec $SHELL")
                    }
                }.toString()
        }

        private fun prepareLargeScriptCommand(
            script: Script,
            fileName: String,
            envVars: String,
            combinedArgs: String,
            actualPrefix: String,
        ): String {
            val termuxSourcePath =
                try {
                    scriptFileRepository.saveToBridge(fileName, script.code)
                } catch (_: Exception) {
                    return "echo 'Error: Could not save script to device storage.'"
                }
            val termuxDestPath = "~/scriptrunner_for_termux/$fileName"

            if (!script.interpreter.matches(SAFE_INTERPRETER_PATTERN)) {
                return "echo 'Error: Invalid interpreter specified.'"
            }

            val escapedInterpreter = escapeForBashDoubleQuotes(script.interpreter)
            val escapedPrefix = if (actualPrefix.isNotBlank()) escapeForBashDoubleQuotes(actualPrefix) + " " else ""
            val escapedArgs = escapeForBashDoubleQuotes(combinedArgs)
            val escapedEnvVars = escapeForBashDoubleQuotes(envVars)

            val coreExecution = "$escapedEnvVars$escapedPrefix$escapedInterpreter $termuxDestPath $escapedArgs"

            return StringBuilder()
                .append("mkdir -p ~/scriptrunner_for_termux && ")
                .append("cp -f $termuxSourcePath $termuxDestPath && ")
                .append("{ rm -f \"$termuxSourcePath\" || true; } && ")
                .append("chmod +x $termuxDestPath && ")
                .append("bash -c \"")
                .append("trap 'rm -f $termuxDestPath' EXIT; ")
                .append(coreExecution)
                .append("\"")
                .apply {
                    if (script.keepSessionOpen) {
                        append($$"; echo; echo '--- Finished (Press Enter) ---'; read; exec $SHELL")
                    }
                }.toString()
        }

        // Another trick to try and force required behaviour, I do hope that passing as a wrapper will work
        // Does just fine with adb killing the process
        private fun wrapCommandWithHeartbeat(
            commandToRun: String,
            intervalMs: Long,
            scriptId: Int,
        ): String {
            val heartbeatAction = "$packageName.HEARTBEAT"
            val finishedAction = "$packageName.SCRIPT_FINISHED"
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
