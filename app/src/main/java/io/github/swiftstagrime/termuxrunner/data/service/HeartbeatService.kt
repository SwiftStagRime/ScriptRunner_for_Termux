package io.github.swiftstagrime.termuxrunner.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import io.github.swiftstagrime.termuxrunner.R
import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptRepository
import io.github.swiftstagrime.termuxrunner.domain.usecase.RunScriptUseCase
import io.github.swiftstagrime.termuxrunner.ui.MainActivity
import io.github.swiftstagrime.termuxrunner.ui.extensions.UiText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * A foreground service that monitors a running script for crashes.
 * It expects a "heartbeat" broadcast from the script to know it's alive.
 * If the heartbeat is not received within a timeout period, it attempts to restart the script.
 * Also runs as a separate process to minimize ram usage, and thus risk of being killed
 */
@AndroidEntryPoint
class HeartbeatService : Service() {

    companion object {
        // Intent actions
        const val ACTION_START = "ACTION_START_MONITORING"
        const val ACTION_STOP = "ACTION_STOP_MONITORING"
        const val ACTION_HEARTBEAT = "io.github.swiftstagrime.HEARTBEAT"
        const val ACTION_SCRIPT_FINISHED = "io.github.swiftstagrime.SCRIPT_FINISHED"

        // Intent extras
        const val EXTRA_SCRIPT_ID = "EXTRA_SCRIPT_ID"
        const val EXTRA_SCRIPT_NAME = "EXTRA_SCRIPT_NAME"
        const val EXTRA_TIMEOUT_MS = "EXTRA_TIMEOUT_MS"

        // Service constants
        private const val NOTIFICATION_ID = 999
        private const val CHANNEL_ID = "termux_monitor_channel"
        private const val DEFAULT_TIMEOUT_MS = 30_000L // Default time to wait for a heartbeat
        private const val CHECK_INTERVAL_MS = 10_000L // How often to check for a heartbeat
        private const val MAX_RETRY_COUNT = 3 // Max number of restart attempts
        private const val WAKELOCK_TAG = "TermuxRunner:HeartbeatWakeLock"
    }

    @Inject
    lateinit var scriptRepository: ScriptRepository

    @Inject
    lateinit var runScriptUseCase: RunScriptUseCase

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var wakeLock: PowerManager.WakeLock? = null

    // --- Monitoring State ---
    private var lastHeartbeatTime = 0L // Timestamp of the last received heartbeat
    private var serviceStartTime = 0L // When the monitoring actually started
    private var isMonitoring = false // Flag to indicate if the service is actively monitoring
    private var currentScriptId = -1 // ID of the script being monitored
    private var currentScriptName = "" // Name of the script for notifications
    private var timeoutLimit = DEFAULT_TIMEOUT_MS // Custom timeout for the heartbeat
    private var restartCount = 0 // Counter for restart attempts

    /**
     * Listens for heartbeat signals from the script and for script finish signals.
     * Now captures exit_code to report accurate final status.
     */
    private val heartbeatReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_HEARTBEAT -> {
                    lastHeartbeatTime = System.currentTimeMillis()
                    updateStatusNotification(UiText.StringResource(R.string.notif_status_active))
                }
                ACTION_SCRIPT_FINISHED -> {
                    val exitCode = intent.getIntExtra("exit_code", 0)
                    isMonitoring = false

                    val finalMsg = if (exitCode == 0) {
                        UiText.StringResource(R.string.notif_finished_success)
                    } else {
                        UiText.StringResource(R.string.notif_finished_error, exitCode)
                    }

                    showFinalNotification(finalMsg)

                    serviceScope.launch {
                        delay(2000) // Brief delay so user can see the status before service closes
                        stopSelf()
                    }
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Initializes the service, creates notification channel, registers receiver, and acquires a wakelock.
     */
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val filter = IntentFilter().apply {
            addAction(ACTION_HEARTBEAT)
            addAction(ACTION_SCRIPT_FINISHED)
        }
        ContextCompat.registerReceiver(
            this,
            heartbeatReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )

        // Acquire a wakelock to ensure the service can run even when the screen is off
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG)
    }

    /**
     * Handles service start commands.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            if (currentScriptId == -1) stopSelf()
            return START_STICKY
        }

        when (intent.action) {
            ACTION_START -> {
                currentScriptId = intent.getIntExtra(EXTRA_SCRIPT_ID, -1)
                currentScriptName = intent.getStringExtra(EXTRA_SCRIPT_NAME) ?: "Unknown"
                timeoutLimit = intent.getLongExtra(EXTRA_TIMEOUT_MS, DEFAULT_TIMEOUT_MS)
                restartCount = 0
                serviceStartTime = System.currentTimeMillis()

                if (wakeLock?.isHeld == false) wakeLock?.acquire()

                startMonitoring()
            }

            ACTION_STOP -> stopSelf()
        }
        return START_STICKY
    }

    /**
     * Starts the monitoring loop.
     */
    private fun startMonitoring() {
        if (isMonitoring) return
        isMonitoring = true
        lastHeartbeatTime = System.currentTimeMillis()
        updateStatusNotification(UiText.StringResource(R.string.notif_status_watching))

        serviceScope.launch {
            while (isMonitoring) {
                delay(CHECK_INTERVAL_MS)
                checkHealth()
                // Periodic update to refresh "time since pulse" in notification
                if (isMonitoring) updateStatusNotification(UiText.StringResource(R.string.notif_status_monitoring))
            }
        }
    }

    /**
     * Checks if the script has timed out.
     */
    private fun checkHealth() {
        val timeSincePulse = System.currentTimeMillis() - lastHeartbeatTime
        if (timeSincePulse > timeoutLimit) {
            attemptRestart()
        }
    }

    /**
     * Restarts the script if retry limit is not exceeded.
     */
    private fun attemptRestart() {
        if (restartCount >= MAX_RETRY_COUNT) {
            updateStatusNotification(UiText.StringResource(R.string.notif_status_failed_unstable))
            isMonitoring = false
            stopSelf()
            return
        }
        restartCount++
        updateStatusNotification(UiText.StringResource(R.string.notif_status_resurrecting, restartCount, MAX_RETRY_COUNT))
        lastHeartbeatTime = System.currentTimeMillis()

        serviceScope.launch {
            val script = scriptRepository.getScriptById(currentScriptId)
            script?.let { runScriptUseCase(it) } ?: stopSelf()
        }
    }

    /**
     * Cleans up resources when the service is destroyed.
     */
    override fun onDestroy() {
        super.onDestroy()
        isMonitoring = false
        if (wakeLock?.isHeld == true) wakeLock?.release()
        serviceScope.cancel()
        try {
            unregisterReceiver(heartbeatReceiver)
        } catch (_: Exception) {
        }
    }

    /**
     * Builds and updates the foreground notification with detailed status.
     */
    private fun updateStatusNotification(status: UiText) {
        val uptimeMins = (System.currentTimeMillis() - serviceStartTime) / 60_000
        val secondsSincePulse = (System.currentTimeMillis() - lastHeartbeatTime) / 1000

        val restartText = if (restartCount > 0) {
            UiText.StringResource(R.string.notif_restart_count, restartCount).asString(this)
        } else ""

        val contentText = UiText.StringResource(
            R.string.notif_details_format,
            status.asString(this),
            restartText,
            uptimeMins,
            secondsSincePulse
        ).asString(this)

        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, HeartbeatService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            2,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(UiText.StringResource(R.string.notif_monitoring_title, currentScriptName).asString(this))
            .setContentText(status.asString(this))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                UiText.StringResource(R.string.notif_stop_monitoring).asString(this),
                stopPendingIntent
            )
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    /**
     * Shows a non-ongoing notification when the script finishes.
     */
    private fun showFinalNotification(text: UiText) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(currentScriptName)
            .setContentText(text.asString(this))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(false)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Creates the notification channel for Android O and above.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                UiText.StringResource(R.string.channel_monitor_name).asString(this),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = UiText.StringResource(R.string.channel_monitor_desc).asString(this@HeartbeatService)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}