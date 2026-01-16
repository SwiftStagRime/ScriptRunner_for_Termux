package io.github.swiftstagrime.termuxrunner.data.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import io.github.swiftstagrime.termuxrunner.R
import io.github.swiftstagrime.termuxrunner.data.local.dao.AutomationDao
import io.github.swiftstagrime.termuxrunner.domain.model.AutomationLog
import io.github.swiftstagrime.termuxrunner.domain.repository.AutomationLogRepository
import io.github.swiftstagrime.termuxrunner.ui.extensions.UiText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TermuxResultReceiver : BroadcastReceiver() {

    @Inject
    lateinit var automationDao: AutomationDao

    @Inject
    lateinit var logRepository: AutomationLogRepository

    override fun onReceive(context: Context, intent: Intent) {
        val scriptName = intent.getStringExtra("script_name") ?: "Script"
        val scriptId = intent.getIntExtra("script_id", -1)
        val automationId = intent.getIntExtra("automation_id", -1)

        var exitCode = -1337
        var internalError: String? = null

        val bundle = intent.getBundleExtra("result")
        if (bundle != null) {
            exitCode = bundle.getInt("exitCode", -1337)
            internalError = bundle.getString("errmsg")
        } else if (intent.hasExtra("com.termux.RUN_COMMAND_RESULT_CODE")) {
            exitCode = intent.getIntExtra("com.termux.RUN_COMMAND_RESULT_CODE", -1337)
            internalError = intent.getStringExtra("com.termux.RUN_COMMAND_ERRMSG")
        }

        if (automationId != -1) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val timestamp = System.currentTimeMillis()

                    automationDao.updateLastResult(automationId, exitCode, timestamp)

                    logRepository.insertLog(
                        AutomationLog(
                            automationId = automationId,
                            timestamp = timestamp,
                            exitCode = exitCode
                        )
                    )
                } finally {
                    pendingResult.finish()
                }
            }
        }

        showResultNotification(context, scriptId, scriptName, exitCode, internalError)
    }


    private fun showResultNotification(
        context: Context,
        scriptId: Int,
        name: String,
        exitCode: Int,
        internalError: String?
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "script_results"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName =
                UiText.StringResource(R.string.channel_script_results).asString(context)
            val channelDesc =
                UiText.StringResource(R.string.channel_script_results_desc).asString(context)

            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = channelDesc
            }
            notificationManager.createNotificationChannel(channel)
        }

        val isSuccess = exitCode == 0

        val title = when {
            exitCode == -1337 -> UiText.StringResource(R.string.notif_title_unknown, name)
            isSuccess -> UiText.StringResource(R.string.notif_title_finished, name)
            else -> UiText.StringResource(R.string.notif_title_failed, name)
        }.asString(context)

        val content = when {
            exitCode == -1337 -> UiText.StringResource(R.string.notif_msg_no_result)
            !internalError.isNullOrBlank() -> UiText.StringResource(
                R.string.notif_msg_error,
                internalError
            )

            isSuccess -> UiText.StringResource(R.string.notif_msg_success)
            else -> UiText.StringResource(R.string.notif_msg_failed_code, exitCode)
        }.asString(context)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager.notify(scriptId, builder.build())
    }
}