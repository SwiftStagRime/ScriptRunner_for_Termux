package io.github.swiftstagrime.termuxrunner.data.automation

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.swiftstagrime.termuxrunner.R
import io.github.swiftstagrime.termuxrunner.data.receiver.AutomationReceiver
import io.github.swiftstagrime.termuxrunner.domain.model.NotificationAction
import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptResultNotificator
import io.github.swiftstagrime.termuxrunner.ui.extensions.UiText
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutomationNotificationHelper
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : ScriptResultNotificator {
        override fun showResultNotification(
            scriptId: Int,
            name: String,
            exitCode: Int,
            internalError: String?,
            actions: List<NotificationAction>,
        ) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "script_results"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channelName =
                    UiText.StringResource(R.string.channel_script_results).asString(context)
                val channelDesc =
                    UiText.StringResource(R.string.channel_script_results_desc).asString(context)

                val channel =
                    NotificationChannel(
                        channelId,
                        channelName,
                        NotificationManager.IMPORTANCE_HIGH,
                    ).apply { description = channelDesc }
                notificationManager.createNotificationChannel(channel)
            }

            val isSuccess = exitCode == 0
            val title =
                when {
                    exitCode == -1337 -> UiText.StringResource(R.string.notif_title_unknown, name)
                    isSuccess -> UiText.StringResource(R.string.notif_title_finished, name)
                    else -> UiText.StringResource(R.string.notif_title_failed, name)
                }.asString(context)

            val content =
                when {
                    exitCode == -1337 -> UiText.StringResource(R.string.notif_msg_no_result)
                    !internalError.isNullOrBlank() ->
                        UiText.StringResource(
                            R.string.notif_msg_error,
                            internalError,
                        )

                    isSuccess -> UiText.StringResource(R.string.notif_msg_success)
                    else -> UiText.StringResource(R.string.notif_msg_failed_code, exitCode)
                }.asString(context)

            val builder =
                NotificationCompat
                    .Builder(context, channelId)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(title)
                    .setContentText(content)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)

            for (action in actions.take(3)) {
                val actionIntent =
                    Intent(context, AutomationReceiver::class.java).apply {
                        putExtra("automation_id", action.targetAutomationId)
                        data = android.net.Uri.parse("automation://${action.targetAutomationId}")
                    }
                val pendingIntent =
                    android.app.PendingIntent.getBroadcast(
                        context,
                        action.targetAutomationId + scriptId * 1000,
                        actionIntent,
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
                    )
                builder.addAction(0, action.label, pendingIntent)
            }

            notificationManager.notify(scriptId, builder.build())
        }
    }
