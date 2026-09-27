package io.github.swiftstagrime.termuxrunner.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.github.swiftstagrime.termuxrunner.data.automation.AutomationScheduler
import io.github.swiftstagrime.termuxrunner.data.local.dao.AutomationDao
import io.github.swiftstagrime.termuxrunner.data.local.dao.ScriptDao
import io.github.swiftstagrime.termuxrunner.data.local.entity.AutomationEntity
import io.github.swiftstagrime.termuxrunner.domain.model.TriggerMode
import io.github.swiftstagrime.termuxrunner.domain.model.triggerMode
import io.github.swiftstagrime.termuxrunner.domain.usecase.RunScriptUseCase
import io.github.swiftstagrime.termuxrunner.domain.util.AutomationTimeCalculator
import io.github.swiftstagrime.termuxrunner.domain.util.DeviceConditions

@HiltWorker
class AutomationWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted workerParams: WorkerParameters,
        private val automationDao: AutomationDao,
        private val scriptDao: ScriptDao,
        private val runScriptUseCase: RunScriptUseCase,
        private val scheduler: AutomationScheduler,
    ) : CoroutineWorker(context, workerParams) {
        companion object {
            const val WORK_NAME_PREFIX = "automation_worker_"

            /**
             * Coarse safety-net interval for re-checking a run that is blocked by
             * unmet conditions (WiFi/charging/battery). The primary re-trigger is
             * event-driven (connectivity/charging broadcasts re-run overdue
             * automations immediately), so this fallback only covers the case
             * where those events are ever lost and stays hourly to keep
             * background wake-ups rare.
             */
            const val CONDITION_FALLBACK_RETRY_MS = 60 * 60 * 1000L
        }

        override suspend fun doWork(): Result {
            val id = inputData.getInt("automation_id", -1)
            val automation = automationDao.getAutomationById(id) ?: return Result.failure()

            if (
                !DeviceConditions.isConditionMet(
                    context = applicationContext,
                    requireWifi = automation.requireWifi,
                    requireCharging = automation.requireCharging,
                    batteryThreshold = automation.batteryThreshold,
                )
            ) {
                handleConditionBlocked(automation)
                return Result.success()
            }

            val nextRun = AutomationTimeCalculator.calculateNextRun(automation)

            val updatedAutomation =
                automation.copy(
                    lastRunTimestamp = System.currentTimeMillis(),
                    nextRunTimestamp = nextRun,
                    isEnabled = (nextRun != null),
                )
            automationDao.updateAutomation(updatedAutomation)

            val scriptEntity = scriptDao.getScriptById(automation.scriptId)
            if (scriptEntity != null) {
                val chainEnvVars =
                    inputData.keyValueMap
                        .filterKeys { it.startsWith("chain_step_env_") }
                        .mapKeys { it.key.removePrefix("chain_step_env_") }
                        .mapValues { it.value.toString() }

                val mergedEnv =
                    automation.runtimeEnv.toMutableMap().apply {
                        putAll(chainEnvVars)
                    }

                runScriptUseCase(
                    script = scriptEntity.toScriptDomain(),
                    runtimeArgs = automation.runtimeArgs,
                    runtimeEnv = mergedEnv,
                    runtimePrefix = automation.runtimePrefix,
                    automationId = automation.id,
                )
            }

            if (updatedAutomation.isEnabled && nextRun != null) {
                scheduler.schedule(updatedAutomation)
            }

            return Result.success()
        }

        /**
         * A scheduled run was attempted but a condition (WiFi/charging/battery)
         * is not met. Instead of polling every few minutes we:
         *
         * - with [AutomationEntity.runIfMissed] enabled, keep the missed slot as
         *   [AutomationEntity.nextRunTimestamp] (still in the past), so the
         *   catch-up stays pending: the moment WiFi comes back or the charger is
         *   plugged in, the connectivity/charging event receivers re-trigger it
         *   and it fires exactly once (never once per missed slot);
         * - set a coarse fallback alarm as a safety net in case those system
         *   events are ever lost.
         *
         * With [AutomationEntity.runIfMissed] disabled the missed slot is
         * skipped outright and the schedule resumes at the next regular slot.
         */
        private suspend fun handleConditionBlocked(automation: AutomationEntity) {
            if (automation.type.triggerMode != TriggerMode.SCHEDULE) return

            val now = System.currentTimeMillis()

            if (!automation.runIfMissed) {
                val nextRun = AutomationTimeCalculator.calculateNextRun(automation, now)
                val updated = automation.copy(nextRunTimestamp = nextRun, isEnabled = nextRun != null)
                automationDao.updateAutomation(updated)
                if (nextRun != null) {
                    scheduler.schedule(updated)
                }
                return
            }

            // Catch-up stays pending (nextRunTimestamp untouched): schedule the
            // fallback re-check at the later of the next regular slot and
            // now + cap, so short-interval schedules never poll more often
            // than the cap and long-interval ones wake at their own cadence.
            // ONE_TIME has no next slot, so it only gets the cap.
            val nextRegular = AutomationTimeCalculator.calculateNextRun(automation, now)
            val fallbackAt =
                nextRegular?.let { maxOf(it, now + CONDITION_FALLBACK_RETRY_MS) }
                    ?: now + CONDITION_FALLBACK_RETRY_MS
            scheduler.scheduleConditionRetry(automation, fallbackAt)
        }
    }
