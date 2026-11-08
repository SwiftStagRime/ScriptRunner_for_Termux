package io.github.swiftstagrime.termuxrunner.data.receiver

import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import io.github.swiftstagrime.termuxrunner.domain.model.AutomationType

/**
 * Turns a system event into the worker that runs the automations listening for it.
 *
 * Two entry points share this logic: the manifest-registered [EventReceiver], which can
 * only ever see the power broadcast, and the runtime receiver in ScreenEventService,
 * which is the only way SCREEN_ON/SCREEN_OFF/USER_PRESENT arrive at all — the system
 * sends them with FLAG_RECEIVER_REGISTERED_ONLY and refuses to deliver them to a
 * manifest-declared receiver.
 */
object EventAutomationDispatcher {
    private const val OVERDUE_CATCH_UP_WORK_NAME = "overdue_automation_catch_up"

    fun mapActionToType(intent: Intent): AutomationType? =
        when (intent.action) {
            Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> AutomationType.SCREEN_ON
            Intent.ACTION_SCREEN_OFF -> AutomationType.SCREEN_OFF

            else -> null
        }

    /**
     * Automations blocked on a condition (charging, WiFi, battery) are re-checked when a
     * relevant event arrives instead of waiting for their fallback alarm.
     */
    fun enqueueOverdueCatchUp(context: Context) {
        val workRequest = OneTimeWorkRequestBuilder<OverdueAutomationWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            OVERDUE_CATCH_UP_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            workRequest,
        )
    }

    fun enqueueForType(
        context: Context,
        automationType: AutomationType,
    ) {
        val workRequest =
            OneTimeWorkRequestBuilder<EventAutomationWorker>()
                .setInputData(workDataOf("event_type" to automationType.name))
                .build()

        // A damaged charger can flap POWER_CONNECTED several times in seconds;
        // a fixed name + REPLACE collapses that burst into one pending run.
        // Screen events stay per-occurrence so quick on/off sequences all fire.
        val unique = automationType == AutomationType.CHARGING_STARTED
        val name =
            if (unique) {
                "event_${automationType.name}"
            } else {
                "event_${automationType.name}_${System.currentTimeMillis()}"
            }
        val policy =
            if (unique) {
                ExistingWorkPolicy.REPLACE
            } else {
                ExistingWorkPolicy.APPEND
            }

        WorkManager.getInstance(context).enqueueUniqueWork(
            name,
            policy,
            workRequest,
        )
    }
}
