package io.github.swiftstagrime.termuxrunner.data.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException

/**
 * TCP-based monitor for script execution status.
 * Listens on a localhost TCP socket for connections from the Termux script wrapper.
 * When the script finishes normally, it sends "EXIT_OK" over the connection.
 * If the connection is closed without "EXIT_OK", the script was killed/OOM'd.
 */
class TcpMonitor(
    private val onScriptFinishedNormally: () -> Unit,
    private val onScriptKilled: () -> Unit,
) {
    private var serverSocket: ServerSocket? = null
    private var monitorJob: Job? = null
    private val supervisorJob = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + supervisorJob)

    fun startListening(): Int {
        // Bind to 127.0.0.1 specifically for security - prevents external access
        val server = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))
        serverSocket = server

        monitorJob =
            scope.launch {
                try {
                    val client = server.accept()
                    handleClient(client)
                } catch (e: Exception) {
                    if (e !is SocketException) {
                        onScriptKilled()
                    }
                } finally {
                    stop()
                }
            }
        return server.localPort
    }

    private suspend fun handleClient(client: Socket) {
        withContext(Dispatchers.IO) {
            client.use { socket ->
                val reader = socket.getInputStream().bufferedReader()
                val line =
                    try {
                        reader.readLine()
                    } catch (e: Exception) {
                        null
                    }

                if (line == "EXIT_OK") {
                    onScriptFinishedNormally()
                } else {
                    onScriptKilled()
                }
            }
        }
    }

    fun stop() {
        monitorJob?.cancel()
        supervisorJob.cancel()
        try {
            serverSocket?.close()
        } catch (_: Exception) {
        }
        serverSocket = null
    }
}
