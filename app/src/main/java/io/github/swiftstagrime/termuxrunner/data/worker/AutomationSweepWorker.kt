package io.github.swiftstagrime.termuxrunner.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.github.swiftstagrime.termuxrunner.data.automation.AutomationScheduler
import io.github.swiftstagrime.termuxrunner.data.local.dao.AutomationDao
import io.github.swiftstagrime.termuxrunner.data.local.entity.AutomationEntity
import io.github.swiftstagrime.termuxrunner.domain.model.TriggerMode
import io.github.swiftstagrime.termuxrunner.domain.model.triggerMode
import io.github.swiftstagrime.termuxrunner.domain.util.AutomationTimeCalculator

/**
 * Periodic safety-net sweep.
 *
 * The normal lifecycle of a scheduled automation is a chain of
 * [android.app.AlarmManager] alarms, each re-armed by the
 * [AutomationWorker] that the previous alarm started. If any link in that
 * chain breaks — a worker that crashed before re-arming, an alarm cancelled by
 * the OS/OEM, a WorkManager reset — the automation is left with a past
 * [AutomationEntity.nextRunTimestamp] and no pending alarm, i.e. permanently
 * stale.
 *
 * This worker runs as a [androidx.work.PeriodicWorkRequest] (minimum interval
 * 15 minutes, inside maintenance windows) and scans for that exact state:
 *
 * - [AutomationEntity.runIfMissed] enabled → the missed slot is re-triggered
 *   via [AutomationScheduler.triggerImmediate]; the re-triggered
 *   [AutomationWorker] re-checks its conditions and fires exactly once when
 *   they are met (never once per missed slot);
 * - [AutomationEntity.runIfMissed] disabled → the missed slot is skipped and
 *   the schedule is advanced to the next future run with its alarm re-armed.
 *
 * This bounds any stale window to about one sweep interval even if the alarm
 * chain is broken.
 */
@HiltWorker
class AutomationSweepWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted workerParams: WorkerParameters,
        private val automationDao: AutomationDao,
        private val scheduler: AutomationScheduler,
    ) : CoroutineWorker(context, workerParams) {
        companion object {
            const val UNIQUE_WORK_NAME = "automation_sweep"

            /** WorkManager's minimum interval for periodic work. */
            const val SWEEP_INTERVAL_MS = 15 * 60 * 1000L

            private const val TAG = "AutomationSweepWorker"
        }

        override suspend fun doWork(): Result {
            val now = System.currentTimeMillis()
            val overdue =
                automationDao.getEnabledAutomations().filter {
                    it.type.triggerMode == TriggerMode.SCHEDULE &&
                        (it.nextRunTimestamp ?: it.scheduledTimestamp) < now
                }

            for (automation in overdue) {
                try {
                    if (automation.runIfMissed) {
                        scheduler.triggerImmediate(automation.id)
                    } else {
                        skipToNextSlot(automation, now)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Sweep failed for automation ${automation.id}", e)
                }
            }
            return Result.success()
        }

        /**
         * Advances a skipped slot to the next future run and re-arms the
         * alarm, mirroring the missed branch of [AutomationScheduler.schedule].
         */
        private suspend fun skipToNextSlot(
            automation: AutomationEntity,
            now: Long,
        ) {
            val nextRun = AutomationTimeCalculator.calculateNextRun(automation, now)
            val updated = automation.copy(nextRunTimestamp = nextRun, isEnabled = nextRun != null)
            automationDao.updateAutomation(updated)
            if (nextRun != null) {
                scheduler.schedule(updated)
            }
        }
    }
