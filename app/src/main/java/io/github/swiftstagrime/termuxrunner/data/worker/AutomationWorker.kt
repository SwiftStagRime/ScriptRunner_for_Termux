package io.github.swiftstagrime.termuxrunner.data.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.github.swiftstagrime.termuxrunner.R
import io.github.swiftstagrime.termuxrunner.data.automation.AutomationScheduler
import io.github.swiftstagrime.termuxrunner.data.automation.recordDispatchFailure
import io.github.swiftstagrime.termuxrunner.data.local.dao.AutomationDao
import io.github.swiftstagrime.termuxrunner.data.local.dao.AutomationLogDao
import io.github.swiftstagrime.termuxrunner.data.local.dao.ScriptDao
import io.github.swiftstagrime.termuxrunner.data.local.entity.AutomationEntity
import io.github.swiftstagrime.termuxrunner.domain.model.AutomationType
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
        private val automationLogDao: AutomationLogDao,
        private val scriptDao: ScriptDao,
        private val runScriptUseCase: RunScriptUseCase,
        private val scheduler: AutomationScheduler,
    ) : CoroutineWorker(context, workerParams) {
        companion object {
            const val WORK_NAME_PREFIX = "automation_worker_"
            private const val TAG = "AutomationWorker"
            private const val FOREGROUND_NOTIFICATION_ID = 4203

            /**
             * Exit code recorded in the automation log when a run could not even be
             * dispatched to Termux (Termux not installed, permission missing,
             * Android 12+ background restriction, ...). Distinct from real script
             * exit codes so the UI can tell "never ran" apart from "ran, failed".
             */
            const val DISPATCH_FAILED_EXIT_CODE = -1

            /**
             * Coarse safety-net interval for re-checking a run that is blocked by
             * unmet conditions or a failed dispatch (WiFi/charging/battery/Termux
             * availability). The primary re-trigger is event-driven (charging
             * broadcasts and the periodic maintenance sweep re-run overdue
             * automations), so this fallback only covers the case where those are
             * ever lost and stays hourly to keep background wake-ups rare.
             */
            const val CONDITION_FALLBACK_RETRY_MS = 60 * 60 * 1000L
        }

        override suspend fun doWork(): Result {
            val id = inputData.getInt("automation_id", -1)
            val automation = automationDao.getAutomationById(id) ?: return Result.failure()

            if (automation.type == AutomationType.UNKNOWN) {
                Log.w(TAG, "Automation $id has an unrecognized trigger type; skipping")
                return Result.success()
            }

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

            // Event- and boot-based automations have no next scheduled slot; they
            // must stay armed for their next trigger. Only single-shot or
            // misconfigured schedules (nextRun == null) are retired here.
            val isEnabled =
                when (automation.type.triggerMode) {
                    TriggerMode.EVENT, TriggerMode.BOOT -> automation.isEnabled
                    TriggerMode.SCHEDULE -> nextRun != null
                }

            val scriptEntity = scriptDao.getScriptById(automation.scriptId)

            val dispatchError =
                if (scriptEntity != null) {
                    try {
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
                        null
                    } catch (e: Exception) {
                        e
                    }
                } else {
                    null
                }

            if (dispatchError != null) {
                handleExecutionFailure(automation, dispatchError)
                return Result.success()
            }

            // The dispatch is fire-and-forget: startForegroundService() returning only
            // proves the system accepted the request, not that Termux ran anything (a
            // force-stopped Termux silently swallows it). The run itself is recorded by
            // the result broadcast, which stores exit code and timestamp together, so a
            // command that never reached Termux cannot masquerade as a completed run.
            val updatedAutomation =
                automation.copy(
                    nextRunTimestamp = nextRun,
                    isEnabled = isEnabled,
                )
            automationDao.updateAutomation(updatedAutomation)

            if (isEnabled && nextRun != null) {
                scheduler.schedule(updatedAutomation)
            }

            return Result.success()
        }

        /**
         * The alarm path enqueues this worker expedited so a scheduled run starts
         * promptly. WorkManager then requires a foreground notification, and without
         * this override CoroutineWorker throws IllegalStateException("Not implemented")
         * and the whole run fails before the script is ever dispatched.
         */
        override suspend fun getForegroundInfo(): ForegroundInfo {
            val channelId = "automation_worker"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel =
                    NotificationChannel(
                        channelId,
                        applicationContext.getString(R.string.automation_worker_channel),
                        NotificationManager.IMPORTANCE_LOW,
                    )
                applicationContext
                    .getSystemService(NotificationManager::class.java)
                    .createNotificationChannel(channel)
            }

            val notification =
                NotificationCompat
                    .Builder(applicationContext, channelId)
                    .setSmallIcon(R.drawable.ic_terminal_tile)
                    .setContentTitle(applicationContext.getString(R.string.automation_worker_notification_title))
                    .setContentText(applicationContext.getString(R.string.automation_worker_notification_text))
                    .setOngoing(true)
                    .build()

            return ForegroundInfo(FOREGROUND_NOTIFICATION_ID, notification)
        }

        /**
         * A scheduled run was attempted but a condition (WiFi/charging/battery)
         * is not met. Instead of polling every few minutes we:
         *
         * - with [AutomationEntity.runIfMissed] enabled, keep the missed slot as
         *   [AutomationEntity.nextRunTimestamp] (still in the past), so the
         *   catch-up stays pending: the charging event receiver and the periodic
         *   maintenance sweep re-trigger it and it fires exactly once (never
         *   once per missed slot) as soon as the condition becomes met;
         * - set a coarse fallback alarm as a safety net in case those are ever
         *   lost.
         *
         * With [AutomationEntity.runIfMissed] disabled the missed slot is
         * skipped outright and the schedule resumes at the next regular slot.
         */
        private suspend fun handleConditionBlocked(automation: AutomationEntity) {
            if (automation.type.triggerMode != TriggerMode.SCHEDULE) return

            if (!automation.runIfMissed) {
                skipToNextSlot(automation)
                return
            }

            scheduleFallbackRecheck(automation)
        }

        /**
         * A run passed all condition checks but Termux refused to accept the
         * command (Termux not installed, permission missing, Android 12+
         * background start restriction, ...). The schedule must never be left
         * stranded with a past [AutomationEntity.nextRunTimestamp] and no
         * pending alarm:
         *
         * - with [AutomationEntity.runIfMissed] enabled, the missed slot stays
         *   pending and a fallback re-check alarm is set, so the run is
         *   attempted again as soon as Termux is reachable (the periodic
         *   maintenance sweep re-checks it as a backstop);
         * - otherwise the missed slot is skipped and the schedule resumes at
         *   the next regular slot.
         *
         * [AutomationEntity.lastRunTimestamp] is deliberately not touched: the
         * script never actually ran. The failure is recorded in the automation
         * log so the user can see why the run did not happen.
         */
        private suspend fun handleExecutionFailure(
            automation: AutomationEntity,
            error: Exception,
        ) {
            Log.e(
                TAG,
                "Automation ${automation.id} (${automation.label}) failed to dispatch",
                error,
            )

            automationLogDao.recordDispatchFailure(
                automationId = automation.id,
                error = error,
                context = applicationContext,
                tag = TAG,
            )

            if (automation.type.triggerMode != TriggerMode.SCHEDULE) return

            if (automation.runIfMissed) {
                scheduleFallbackRecheck(automation)
            } else {
                skipToNextSlot(automation)
            }
        }

        /**
         * Keeps the missed slot pending (nextRunTimestamp untouched) and
         * schedules a fallback re-check alarm at the later of the next regular
         * slot and now + [CONDITION_FALLBACK_RETRY_MS], so short-interval
         * schedules never poll more often than the cap and long-interval ones
         * wake at their own cadence. ONE_TIME has no next slot, so it only
         * gets the cap.
         */
        private suspend fun scheduleFallbackRecheck(automation: AutomationEntity) {
            val now = System.currentTimeMillis()
            val nextRegular = AutomationTimeCalculator.calculateNextRun(automation, now)
            val fallbackAt =
                nextRegular?.let { maxOf(it, now + CONDITION_FALLBACK_RETRY_MS) }
                    ?: now + CONDITION_FALLBACK_RETRY_MS
            scheduler.scheduleConditionRetry(automation, fallbackAt)
        }

        /**
         * Skips the current (missed) slot: advances the schedule to the next
         * future run and re-arms the alarm. If no future run exists (expired
         * one-shot, misconfigured interval) the automation is retired.
         */
        private suspend fun skipToNextSlot(automation: AutomationEntity) {
            val now = System.currentTimeMillis()
            val nextRun = AutomationTimeCalculator.calculateNextRun(automation, now)
            val updated = automation.copy(nextRunTimestamp = nextRun, isEnabled = nextRun != null)
            automationDao.updateAutomation(updated)
            if (nextRun != null) {
                scheduler.schedule(updated)
            }
        }
    }
