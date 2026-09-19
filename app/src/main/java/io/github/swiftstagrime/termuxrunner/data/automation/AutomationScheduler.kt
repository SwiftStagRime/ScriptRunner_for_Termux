package io.github.swiftstagrime.termuxrunner.data.automation

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.net.toUri
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.swiftstagrime.termuxrunner.data.local.dao.AutomationDao
import io.github.swiftstagrime.termuxrunner.data.local.entity.AutomationEntity
import io.github.swiftstagrime.termuxrunner.data.receiver.AutomationReceiver
import io.github.swiftstagrime.termuxrunner.data.worker.AutomationWorker
import io.github.swiftstagrime.termuxrunner.domain.model.AutomationType
import io.github.swiftstagrime.termuxrunner.domain.util.AutomationTimeCalculator
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutomationScheduler
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val automationDao: AutomationDao,
    ) {
        private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        suspend fun schedule(automation: AutomationEntity) {
            if (!automation.isEnabled) return

            val now = System.currentTimeMillis()
            var triggerTime = automation.nextRunTimestamp ?: automation.scheduledTimestamp

            // Don't schedule AlarmManager for BOOT or event-based types
            if (automation.type == AutomationType.BOOT || automation.type.isEventBased) {
                return
            }

            if (triggerTime < now) {
                if (automation.runIfMissed) {
                    triggerImmediate(automation.id)
                    return
                }

                // Missed slots are skipped: advance to the next future run so the
                // automation is not left stranded with a stale past timestamp.
                val nextRun = AutomationTimeCalculator.calculateNextRun(automation, now)
                val updated = automation.copy(nextRunTimestamp = nextRun, isEnabled = nextRun != null)
                automationDao.updateAutomation(updated)
                if (nextRun == null) return
                triggerTime = nextRun
            }

            val intent =
                Intent(context, AutomationReceiver::class.java).apply {
                    putExtra("automation_id", automation.id)
                    data = "automation://${automation.id}".toUri()
                }

            val pendingIntent =
                PendingIntent.getBroadcast(
                    context,
                    automation.id,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )

            try {
                if (canScheduleExact()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent,
                    )
                } else {
                    // Fallback to inexact if permission is missing
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent,
                    )
                }
            } catch (_: SecurityException) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent,
                )
            }
        }

        private fun canScheduleExact(): Boolean =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }

        fun cancel(automation: AutomationEntity) {
            val intent =
                Intent(context, AutomationReceiver::class.java).apply {
                    data = "automation://${automation.id}".toUri()
                }
            val pendingIntent =
                PendingIntent.getBroadcast(
                    context,
                    automation.id,
                    intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
                )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
            }
        }

        fun triggerImmediate(automationId: Int) {
            val workRequest =
                OneTimeWorkRequestBuilder<AutomationWorker>()
                    .setInputData(workDataOf("automation_id" to automationId))
                    .build()
            // Unique name + KEEP: at most one pending/running worker per automation,
            // so catch-up fires exactly once even if scheduling is re-entered.
            WorkManager.getInstance(context).enqueueUniqueWork(
                AutomationWorker.WORK_NAME_PREFIX + automationId,
                ExistingWorkPolicy.KEEP,
                workRequest,
            )
        }
    }
