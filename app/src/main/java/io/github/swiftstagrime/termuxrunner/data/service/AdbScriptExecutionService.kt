package io.github.swiftstagrime.termuxrunner.data.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import io.github.swiftstagrime.termuxrunner.R
import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptRepository
import io.github.swiftstagrime.termuxrunner.domain.usecase.RunScriptUseCase
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * A foreground service responsible for executing a script identified by its `adbCode`.
 *
 * This service is designed to be started via an [Intent] that contains the
 * `adbCode` as an extra under the key [EXTRA_ADB_CODE].
 * It's called by special activity, as receivers work like shit for this specific purpose in modern android
 */
@AndroidEntryPoint
class AdbScriptExecutionService : Service() {
    companion object {
        private const val CHANNEL_ID = "adb_script_execution_channel"
        private const val NOTIFICATION_ID = 1001
        const val EXTRA_ADB_CODE = "io.github.swiftstagrime.termuxrunner.adb_code"

        fun newIntent(
            context: Context,
            adbCode: String,
        ): Intent =
            Intent(context, AdbScriptExecutionService::class.java).apply {
                putExtra(EXTRA_ADB_CODE, adbCode)
            }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Inject
    lateinit var scriptRepository: ScriptRepository

    @Inject
    lateinit var runScriptUseCase: RunScriptUseCase

    private val notificationManager by lazy {
        getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                buildNotification("Initializing..."),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(NOTIFICATION_ID, buildNotification("Initializing..."))
        }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        val adbCode = intent?.getStringExtra(EXTRA_ADB_CODE)

        if (adbCode == null) {
            cleanupAndStop(startId)
            return START_NOT_STICKY
        }

        serviceScope.launch {
            try {
                updateNotification("Searching for script: $adbCode")
                val script = scriptRepository.getScriptByAdbCode(adbCode).getOrThrow()

                updateNotification("Executing: ${script.name}")
                runScriptUseCase(script)

                updateNotification("Execution finished successfully.")
            } catch (e: Exception) {
                val errorMessage =
                    when (e) {
                        is ScriptNotFoundException -> e.message ?: "Script not found."
                        else -> "Error: ${e.localizedMessage ?: "Unknown execution error"}"
                    }
                updateNotification(errorMessage, isError = true)
            } finally {
                delay(3000)
                cleanupAndStop(startId)
            }
        }

        return START_NOT_STICKY
    }

    private fun cleanupAndStop(startId: Int) {
        stopForeground(STOP_FOREGROUND_DETACH)
        stopSelf(startId)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "ADB Script Execution",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "Shows progress of scripts executed via ADB"
                }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(
        content: String,
        isError: Boolean = false,
    ): Notification {
        val builder =
            NotificationCompat
                .Builder(this, CHANNEL_ID)
                .setContentTitle("Termux Script Runner")
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(if (isError) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(true)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)

        return builder.build()
    }

    private fun updateNotification(
        text: String,
        isError: Boolean = false,
    ) {
        val notification = buildNotification(text, isError)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}

class ScriptNotFoundException(
    adbCode: String,
) : Exception("Script with code '$adbCode' not found.")
