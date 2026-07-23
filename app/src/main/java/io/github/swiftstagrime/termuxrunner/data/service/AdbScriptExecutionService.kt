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
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.AndroidEntryPoint
import io.github.swiftstagrime.termuxrunner.R
import io.github.swiftstagrime.termuxrunner.data.repository.AutomationNotFoundException
import io.github.swiftstagrime.termuxrunner.data.repository.ScriptNotFoundException
import io.github.swiftstagrime.termuxrunner.data.service.AdbScriptExecutionService.Companion.EXTRA_ADB_CODE
import io.github.swiftstagrime.termuxrunner.data.service.AdbScriptExecutionService.Companion.EXTRA_TARGET_TYPE
import io.github.swiftstagrime.termuxrunner.data.worker.AutomationWorker
import io.github.swiftstagrime.termuxrunner.domain.repository.AutomationRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptRepository
import io.github.swiftstagrime.termuxrunner.domain.usecase.RunScriptUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * A foreground service responsible for executing a script or automation identified by its trigger code.
 *
 * This service is designed to be started via an [Intent] that contains the
 * `adbCode` as an extra under the key [EXTRA_ADB_CODE].
 * Use [EXTRA_TARGET_TYPE] to specify "script" (default) or "automation".
 */
@AndroidEntryPoint
class AdbScriptExecutionService : Service() {
    companion object {
        private const val CHANNEL_ID = "adb_script_execution_channel"
        private const val NOTIFICATION_ID = 1001
        const val EXTRA_ADB_CODE = "io.github.swiftstagrime.termuxrunner.adb_code"
        const val EXTRA_TARGET_TYPE = "io.github.swiftstagrime.termuxrunner.target_type"
        const val TARGET_SCRIPT = "script"
        const val TARGET_AUTOMATION = "automation"

        fun newIntent(
            context: Context,
            code: String,
            targetType: String = TARGET_SCRIPT,
        ): Intent =
            Intent(context, AdbScriptExecutionService::class.java).apply {
                putExtra(EXTRA_ADB_CODE, code)
                putExtra(EXTRA_TARGET_TYPE, targetType)
            }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Inject
    lateinit var scriptRepository: ScriptRepository

    @Inject
    lateinit var automationRepository: AutomationRepository

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
        val code = intent?.getStringExtra(EXTRA_ADB_CODE)
        val targetType = intent?.getStringExtra(EXTRA_TARGET_TYPE) ?: TARGET_SCRIPT

        if (code == null) {
            cleanupAndStop(startId)
            return START_NOT_STICKY
        }

        serviceScope.launch {
            try {
                when (targetType) {
                    TARGET_AUTOMATION -> executeAutomation(code, startId)
                    else -> executeScript(code, startId)
                }
            } catch (e: Exception) {
                val errorMessage = e.message ?: "Unknown execution error"
                updateNotification("Error: $errorMessage", isError = true)
                delay(3000)
                cleanupAndStop(startId)
            }
        }

        return START_NOT_STICKY
    }

    private suspend fun executeScript(
        code: String,
        startId: Int,
    ) {
        try {
            updateNotification("Searching for script: $code")
            val script = scriptRepository.getScriptByAdbCode(code).getOrThrow()

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

    private suspend fun executeAutomation(
        code: String,
        startId: Int,
    ) {
        try {
            updateNotification("Searching for automation: $code")
            val automation = automationRepository.getAutomationByAdbCode(code).getOrThrow()

            updateNotification("Triggering: ${automation.label}")
            triggerAutomationViaWorkManager(automation.id)
            updateNotification("Automation triggered successfully.")
        } catch (e: Exception) {
            val errorMessage =
                when (e) {
                    is AutomationNotFoundException -> e.message ?: "Automation not found."
                    else -> "Error: ${e.localizedMessage ?: "Unknown execution error"}"
                }
            updateNotification(errorMessage, isError = true)
        } finally {
            delay(3000)
            cleanupAndStop(startId)
        }
    }

    private fun triggerAutomationViaWorkManager(automationId: Int) {
        val workRequest =
            OneTimeWorkRequestBuilder<AutomationWorker>()
                .setInputData(workDataOf("automation_id" to automationId))
                .build()
        WorkManager
            .getInstance(this)
            .enqueue(workRequest)
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
                    "ADB Execution",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "Shows progress of scripts and automations executed via ADB"
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
