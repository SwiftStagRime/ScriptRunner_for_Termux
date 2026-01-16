package io.github.swiftstagrime.termuxrunner.data.automation

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.net.toUri
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.swiftstagrime.termuxrunner.data.local.entity.AutomationEntity
import io.github.swiftstagrime.termuxrunner.data.receiver.AutomationReceiver
import io.github.swiftstagrime.termuxrunner.data.worker.AutomationWorker
import io.github.swiftstagrime.termuxrunner.domain.model.AutomationType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutomationScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(automation: AutomationEntity) {
        if (!automation.isEnabled) return

        val now = System.currentTimeMillis()
        val triggerTime = automation.nextRunTimestamp ?: automation.scheduledTimestamp

        if (triggerTime < now) {
            if (automation.runIfMissed) {
                triggerImmediate(automation.id)
                return
            } else {
                return
            }
        }


        // If it's a one-time script and the time has already passed, don't schedule
        if (automation.type == AutomationType.ONE_TIME && triggerTime < System.currentTimeMillis()) {
            return
        }

        val intent = Intent(context, AutomationReceiver::class.java).apply {
            putExtra("automation_id", automation.id)
            data = "automation://${automation.id}".toUri()
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            automation.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (canScheduleExact()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                // Fallback to inexact if permission is missing
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    private fun canScheduleExact(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun cancel(automation: AutomationEntity) {
        val intent = Intent(context, AutomationReceiver::class.java).apply {
            data = "automation://${automation.id}".toUri()
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            automation.id,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }

    private fun triggerImmediate(automationId: Int) {
        val workRequest = OneTimeWorkRequestBuilder<AutomationWorker>()
            .setInputData(workDataOf("automation_id" to automationId))
            .build()
        WorkManager.getInstance(context).enqueue(workRequest)
    }
}