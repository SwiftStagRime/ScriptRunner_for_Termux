package io.github.swiftstagrime.termuxrunner.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
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
 * A foreground service that monitors running scripts for crashes.
 *
 * Supports two monitoring modes:
 * 1. **TCP Monitoring** (primary): Uses [TcpMonitor] instances per script for event-driven
 *    monitoring with zero CPU usage while waiting.
 * 2. **Broadcast Monitoring** (fallback): Uses heartbeat broadcasts with periodic health checks
 *    for scripts running without python3 in Termux.
 *
 * Runs as a separate process to minimize RAM usage and risk of being killed.
 */
@AndroidEntryPoint
class HeartbeatService : Service() {
    @Inject
    lateinit var scriptRepository: ScriptRepository

    @Inject
    lateinit var runScriptUseCase: RunScriptUseCase

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var wakeLock: PowerManager.WakeLock? = null
    private var isServiceRunning = false
    private val monitoredScripts = mutableMapOf<Int, ScriptMonitorState>()
    private val tcpMonitors = mutableMapOf<Int, TcpMonitor>()
    private val scriptPorts = mutableMapOf<Int, Int>()
    private val actionHeartbeat by lazy { "$packageName.HEARTBEAT" }
    private val actionFinished by lazy { "$packageName.SCRIPT_FINISHED" }
    private val binder = ScriptPortBinder()

    /**
     * Listens for heartbeat signals and finish signals from broadcast-based monitoring.
     * Expects 'script_id' extra in the broadcast to identify the script.
     */
    private val heartbeatReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?,
            ) {
                val scriptId = intent?.getIntExtra("script_id", -1) ?: return

                when (intent.action) {
                    actionHeartbeat -> {
                        val state = monitoredScripts[scriptId] ?: return
                        state.lastHeartbeatTime = System.currentTimeMillis()
                        state.status = UiText.StringResource(R.string.notif_status_active)
                    }

                    actionFinished -> {
                        val exitCode = intent.getIntExtra(EXTRA_EXIT_CODE, 0)
                        handleScriptFinished(scriptId, exitCode)
                    }
                }
            }
        }

    /**
     * Initializes the service, creates notification channel, registers receiver, and acquires a wakelock.
     */
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val filter =
            IntentFilter().apply {
                addAction(actionHeartbeat)
                addAction(actionFinished)
            }
        ContextCompat.registerReceiver(
            this,
            heartbeatReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED,
        )

        // Acquire a wakelock to ensure the service can run even when the screen is off
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG)
    }

    /**
     * Handles service start commands.
     */
    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        if (intent == null) {
            // Service restarted by system. If we have no data, stop.
            if (monitoredScripts.isEmpty() && tcpMonitors.isEmpty()) stopSelf()
            return START_STICKY
        }

        when (intent.action) {
            ACTION_START -> {
                val id = intent.getIntExtra(EXTRA_SCRIPT_ID, -1)
                val name = intent.getStringExtra(EXTRA_SCRIPT_NAME) ?: "Unknown"
                val timeout = intent.getLongExtra(EXTRA_TIMEOUT_MS, DEFAULT_TIMEOUT_MS)

                if (id != -1) {
                    startMonitoringForScript(id, name, timeout)
                }
            }

            ACTION_STOP -> {
                val id = intent.getIntExtra(EXTRA_SCRIPT_ID, -1)
                if (id != -1) {
                    stopMonitoringForScript(id)
                } else {
                    stopAll()
                }
            }

            ACTION_STOP_ALL -> stopAll()
        }
        return START_STICKY
    }

    /**
     * Starts TCP or broadcast monitoring for a single script.
     */
    private fun startMonitoringForScript(
        id: Int,
        name: String,
        timeout: Long,
    ) {
        val newState =
            ScriptMonitorState(
                id = id,
                name = name,
                timeoutLimit = timeout,
            )
        monitoredScripts[id] = newState

        try {
            val monitor =
                TcpMonitor(
                    onScriptFinishedNormally = {
                        handleTcpScriptFinished(id, normal = true)
                    },
                    onScriptKilled = {
                        handleTcpScriptFinished(id, normal = false)
                    },
                )

            val port = monitor.startListening()
            tcpMonitors[id] = monitor
            scriptPorts[id] = port
        } catch (e: Exception) {
            // TCP monitor failed (e.g., permission denied) - fall back to broadcast monitoring
            // Script remains in monitoredScripts for notification tracking
            // but without a TCP port, so RunScriptUseCase will use broadcast heartbeat
        }

        startServiceLoopIfNeeded()
        updateNotification()
    }

    /**
     * Stops monitoring for a specific script.
     */
    private fun stopMonitoringForScript(id: Int) {
        tcpMonitors.remove(id)?.stop()
        scriptPorts.remove(id)
        monitoredScripts.remove(id)
        updateNotification()
        if (monitoredScripts.isEmpty()) stopAll()
    }

    private fun handleTcpScriptFinished(
        scriptId: Int,
        normal: Boolean,
    ) {
        tcpMonitors.remove(scriptId)?.stop()
        scriptPorts.remove(scriptId)

        if (normal) {
            handleScriptFinished(scriptId, exitCode = 0)
        } else {
            val state = monitoredScripts[scriptId] ?: return
            attemptRestart(state)
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        if (wakeLock?.isHeld == true) wakeLock?.release()
        serviceScope.cancel()

        tcpMonitors.values.forEach { it.stop() }
        tcpMonitors.clear()
        scriptPorts.clear()

        try {
            unregisterReceiver(heartbeatReceiver)
        } catch (e: Exception) {
            android.util.Log.d(
                "HeartbeatService",
                "Failed to unregister receiver (may already be unregistered)",
                e,
            )
        }
    }

    /**
     * Starts the monitoring loop for broadcast-based health checks.
     */
    private fun startServiceLoopIfNeeded() {
        if (isServiceRunning) return
        isServiceRunning = true

        if (wakeLock?.isHeld == false) wakeLock?.acquire()

        serviceScope.launch {
            while (isServiceRunning) {
                delay(CHECK_INTERVAL_MS)
                val allTcp = monitoredScripts.keys.all { tcpMonitors.containsKey(it) }
                if (!allTcp) checkHealth()
                if (isServiceRunning) updateNotification()
            }
        }
    }

    /**
     * Iterates through all monitored scripts to check for timeouts (broadcast mode only).
     */
    private fun checkHealth() {
        val currentTime = System.currentTimeMillis()
        // Iterate over a copy of the keys to avoid concurrent modification issues
        val scriptIds = monitoredScripts.keys.toList()

        for (id in scriptIds) {
            // Skip scripts being monitored via TCP
            if (tcpMonitors.containsKey(id)) continue

            val state = monitoredScripts[id] ?: continue
            val timeSincePulse = currentTime - state.lastHeartbeatTime

            if (timeSincePulse > state.timeoutLimit) {
                attemptRestart(state)
            } else {
                state.status = UiText.StringResource(R.string.notif_status_monitoring)
            }
        }
    }

    private fun attemptRestart(state: ScriptMonitorState) {
        state.restartCount++
        state.status =
            UiText.StringResource(
                R.string.notif_status_resurrecting,
                state.restartCount,
                MAX_RESTARTS,
            )
        state.lastHeartbeatTime = System.currentTimeMillis()

        if (state.restartCount >= MAX_RESTARTS) {
            monitoredScripts.remove(state.id)
            updateNotification()
            return
        }

        // Relaunch
        serviceScope.launch {
            val script = scriptRepository.getScriptById(state.id)
            if (script != null) {
                runScriptUseCase(script)
            } else {
                monitoredScripts.remove(state.id)
            }
        }
        updateNotification()
    }

    private fun handleScriptFinished(
        scriptId: Int,
        exitCode: Int,
    ) {
        val state = monitoredScripts.remove(scriptId) ?: return

        val finalMsg =
            if (exitCode == 0) {
                UiText.StringResource(R.string.notif_finished_success)
            } else {
                UiText.StringResource(R.string.notif_finished_error, exitCode)
            }

        showFinalNotification(state.name, finalMsg)

        updateNotification()
        if (monitoredScripts.isEmpty()) stopAll()
    }

    private fun stopAll() {
        isServiceRunning = false
        monitoredScripts.clear()

        tcpMonitors.values.forEach { it.stop() }
        tcpMonitors.clear()
        scriptPorts.clear()

        if (wakeLock?.isHeld == true) wakeLock?.release()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * Updates the notification based on how many scripts are running.
     */
    private fun updateNotification() {
        if (monitoredScripts.isEmpty()) return

        if (monitoredScripts.size == 1) {
            val state = monitoredScripts.values.first()
            showSingleScriptNotification(state)
        } else {
            showSummaryNotification()
        }
    }

    private fun showSingleScriptNotification(state: ScriptMonitorState) {
        val currentTime = System.currentTimeMillis()
        val uptimeMins = (currentTime - state.serviceStartTime) / 60_000
        val secondsSincePulse = (currentTime - state.lastHeartbeatTime) / 1000

        val restartText =
            if (state.restartCount > 0) {
                UiText
                    .StringResource(R.string.notif_restart_count, state.restartCount)
                    .asString(this)
            } else {
                ""
            }

        val contentText =
            UiText
                .StringResource(
                    R.string.notif_details_format,
                    state.status.asString(this),
                    restartText,
                    uptimeMins,
                    secondsSincePulse,
                ).asString(this)

        buildAndNotify(
            title =
                UiText
                    .StringResource(R.string.notif_monitoring_title, state.name)
                    .asString(this),
            text = contentText,
        )
    }

    private fun showSummaryNotification() {
        val count = monitoredScripts.size
        val scriptNames = monitoredScripts.values.joinToString(", ") { it.name }

        buildAndNotify(
            title = UiText.StringResource(R.string.notif_summary_title, count).asString(this),
            text = UiText.StringResource(R.string.notif_summary_desc, scriptNames).asString(this),
        )
    }

    private fun buildAndNotify(
        title: String,
        text: String,
    ) {
        val contentIntent =
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE,
            )

        // "Stop All" action
        val stopIntent =
            Intent(this, HeartbeatService::class.java).apply {
                action = ACTION_STOP_ALL
            }
        val stopPendingIntent =
            PendingIntent.getService(
                this,
                2,
                stopIntent,
                PendingIntent.FLAG_IMMUTABLE,
            )

        val notification =
            NotificationCompat
                .Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(contentIntent)
                .addAction(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    UiText.StringResource(R.string.notif_stop_all).asString(this),
                    stopPendingIntent,
                ).setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    /**
     * Shows a non-ongoing notification when the script finishes.
     */
    private fun showFinalNotification(
        name: String,
        text: UiText,
    ) {
        // We use a unique ID based on name hash to prevent overwriting if multiple finish at once
        val notifId = name.hashCode()

        val notification =
            NotificationCompat
                .Builder(this, CHANNEL_ID)
                .setContentTitle(name)
                .setContentText(text.asString(this))
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setOngoing(false)
                .setAutoCancel(true)
                .build()

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notifId, notification)
    }

    /**
     * Creates the notification channel for Android O and above.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    UiText.StringResource(R.string.channel_monitor_name).asString(this),
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description =
                        UiText
                            .StringResource(R.string.channel_monitor_desc)
                            .asString(this@HeartbeatService)
                }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    data class ScriptMonitorState(
        val id: Int,
        val name: String,
        val timeoutLimit: Long,
        var lastHeartbeatTime: Long = System.currentTimeMillis(),
        var serviceStartTime: Long = System.currentTimeMillis(),
        var restartCount: Int = 0,
        var status: UiText = UiText.StringResource(R.string.notif_status_watching),
    )

    /**
     * Binder exposed to callers for retrieving TCP ports after starting monitoring.
     */
    inner class ScriptPortBinder : Binder() {
        fun getPort(scriptId: Int): Int? = scriptPorts[scriptId]

        fun getService(): HeartbeatService = this@HeartbeatService
    }

    companion object {
        // Intent actions
        const val ACTION_START = "ACTION_START_MONITORING"
        const val ACTION_STOP =
            "ACTION_STOP_MONITORING" // Stops specific script if ID provided, or all
        const val ACTION_STOP_ALL = "ACTION_STOP_ALL_MONITORING" // Explicit stop all

        // Intent extras
        const val EXTRA_SCRIPT_ID = "EXTRA_SCRIPT_ID" // Used in broadcasts and start commands
        const val EXTRA_SCRIPT_NAME = "EXTRA_SCRIPT_NAME"
        const val EXTRA_TIMEOUT_MS = "EXTRA_TIMEOUT_MS"
        const val EXTRA_EXIT_CODE = "exit_code" // From Bash wrapper

        // Service constants
        private const val NOTIFICATION_ID = 999
        private const val CHANNEL_ID = "termux_monitor_channel"
        private const val DEFAULT_TIMEOUT_MS = 30_000L
        private const val CHECK_INTERVAL_MS = 10_000L
        private const val WAKELOCK_TAG = "TermuxRunner:HeartbeatWakeLock"
        private const val MAX_RESTARTS = 5
    }
}
