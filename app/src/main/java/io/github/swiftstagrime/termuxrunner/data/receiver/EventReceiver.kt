package io.github.swiftstagrime.termuxrunner.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.github.swiftstagrime.termuxrunner.data.automation.AutomationScheduler
import io.github.swiftstagrime.termuxrunner.data.local.dao.AutomationDao
import io.github.swiftstagrime.termuxrunner.data.worker.AutomationWorker
import io.github.swiftstagrime.termuxrunner.domain.model.AutomationType
import io.github.swiftstagrime.termuxrunner.domain.model.TriggerMode
import io.github.swiftstagrime.termuxrunner.domain.model.triggerMode

/**
 * Receives every event trigger: the screen events, the unlock broadcast and the
 * power-connected broadcast.
 *
 * This receiver is registered at runtime by AutomationEventService rather than declared in
 * the manifest. The system flags SCREEN_ON and SCREEN_OFF as registered-receiver-only and
 * refuses USER_PRESENT to receivers outside the system, while the power broadcasts are
 * implicit and not exempted from the API 26 manifest-receiver ban. A manifest entry for
 * them would be dead configuration that silently never fires, which is exactly how this
 * used to fail.
 */
class EventReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action.isNullOrEmpty()) return

        // Charger plugged in: automations blocked on the charging condition may
        // now be able to run, so re-check overdue catch-ups right away.
        if (intent.action == Intent.ACTION_POWER_CONNECTED) {
            EventAutomationDispatcher.enqueueOverdueCatchUp(context)
            EventAutomationDispatcher.enqueueForType(context, AutomationType.CHARGING_STARTED)
            return
        }

        val eventAutomationType = EventAutomationDispatcher.mapActionToType(intent) ?: return

        EventAutomationDispatcher.enqueueForType(context, eventAutomationType)
    }
}

@HiltWorker
class EventAutomationWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted workerParams: WorkerParameters,
        private val automationDao: AutomationDao,
    ) : CoroutineWorker(context, workerParams) {
        override suspend fun doWork(): Result {
            val eventTypeName = inputData.getString("event_type") ?: return Result.failure()
            val eventType =
                runCatching { AutomationType.valueOf(eventTypeName) }.getOrNull()
                    ?: return Result.failure()

            val enabledAutomations =
                automationDao.getEnabledAutomations().filter { it.type == eventType }

            for (automation in enabledAutomations) {
                val workRequest =
                    androidx.work
                        .OneTimeWorkRequestBuilder<AutomationWorker>()
                        .setInputData(workDataOf("automation_id" to automation.id))
                        .build()
                WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                    AutomationWorker.WORK_NAME_PREFIX + automation.id,
                    ExistingWorkPolicy.KEEP,
                    workRequest,
                )
            }

            return Result.success()
        }
    }

/**
 * Re-triggers overdue scheduled automations when a relevant system event arrives
 * (e.g. charger plugged in). Each re-triggered worker re-checks its conditions,
 * so an automation only runs once its conditions are actually met — and the
 * unique work name guarantees the catch-up fires at most once.
 */
@HiltWorker
class OverdueAutomationWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted workerParams: WorkerParameters,
        private val automationDao: AutomationDao,
        private val scheduler: AutomationScheduler,
    ) : CoroutineWorker(context, workerParams) {
        override suspend fun doWork(): Result {
            val now = System.currentTimeMillis()
            val overdue =
                automationDao.getEnabledAutomations().filter {
                    it.type.triggerMode == TriggerMode.SCHEDULE &&
                        it.runIfMissed &&
                        (it.nextRunTimestamp ?: it.scheduledTimestamp) < now
                }
            overdue.forEach { scheduler.triggerImmediate(it.id) }
            return Result.success()
        }
    }
