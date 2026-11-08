package io.github.swiftstagrime.termuxrunner.data.service

import android.app.Notification
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
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import io.github.swiftstagrime.termuxrunner.R
import io.github.swiftstagrime.termuxrunner.data.receiver.EventReceiver
import io.github.swiftstagrime.termuxrunner.ui.MainActivity

/**
 * Hosts the runtime receiver for every event trigger: SCREEN_ON, SCREEN_OFF,
 * USER_PRESENT and ACTION_POWER_CONNECTED.
 *
 * None of these can be received through a manifest-declared receiver. The screen events
 * are flagged registered-receiver-only by the system and USER_PRESENT is refused to
 * receivers outside the system, while the power broadcasts are implicit and not exempted
 * from the API 26 manifest-receiver ban. Receiving them therefore requires a live process
 * with a runtime registration, and a foreground service is the only way this companion
 * app can stay resident for it. The ongoing notification is the price of that, so the
 * automation editor warns about it before the user picks an event trigger.
 */
@AndroidEntryPoint
class AutomationEventService : Service() {
    private var eventReceiver: BroadcastReceiver? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        registerEventReceiver()

        return START_STICKY
    }

    override fun onDestroy() {
        eventReceiver?.let { runCatching { unregisterReceiver(it) } }
        eventReceiver = null
        super.onDestroy()
    }

    private fun registerEventReceiver() {
        if (eventReceiver != null) return

        val receiver = EventReceiver()
        val filter =
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
                addAction(Intent.ACTION_POWER_CONNECTED)
            }

        // Only protected system broadcasts, so the receiver does not have to be exported
        ContextCompat.registerReceiver(
            this,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        eventReceiver = receiver
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.event_watcher_notification_title),
                    NotificationManager.IMPORTANCE_LOW,
                )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openAppIntent =
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        return NotificationCompat
            .Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.event_watcher_notification_title))
            .setContentText(getString(R.string.event_watcher_notification_text))
            .setSmallIcon(R.drawable.ic_terminal_tile)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val TAG = "AutomationEventService"
        private const val NOTIFICATION_ID = 4202
        private const val CHANNEL_ID = "automation_event_service"
        const val ACTION_STOP = "ACTION_STOP_EVENT_WATCHER"

        /**
         * Best effort: starting a foreground service from the background can be refused on
         * Android 12+. The watcher is reconciled whenever the app is opened, an automation
         * changes and at boot, and the periodic sweep retries, so a refused start is
         * retried rather than lost.
         */
        fun start(context: Context) {
            runCatching {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, AutomationEventService::class.java),
                )
            }.onFailure {
                Log.w(TAG, "Event watcher could not be started now; will retry", it)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AutomationEventService::class.java))
        }
    }
}
