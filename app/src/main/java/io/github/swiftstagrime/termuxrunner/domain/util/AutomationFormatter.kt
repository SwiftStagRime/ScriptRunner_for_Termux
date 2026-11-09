package io.github.swiftstagrime.termuxrunner.domain.util

import android.content.Context
import android.text.format.DateUtils
import io.github.swiftstagrime.termuxrunner.R
import io.github.swiftstagrime.termuxrunner.domain.model.Automation

object AutomationFormatter {
    fun formatNextRun(
        context: Context,
        automation: Automation,
        now: Long = System.currentTimeMillis(),
    ): String {
        val nextRun =
            automation.nextRunTimestamp
                ?: return context.getString(R.string.automation_not_scheduled)

        if (nextRun > now) {
            val relative =
                DateUtils.getRelativeTimeSpanString(
                    nextRun,
                    now,
                    DateUtils.MINUTE_IN_MILLIS,
                )
            return context.getString(R.string.automation_next_run_format, relative)
        }

        // The scheduled slot has passed and the run is still pending. If a
        // condition is currently unmet, say exactly what we are waiting for
        // instead of a stale "Running soon": the automation will fire once,
        // as soon as the condition becomes met.
        val appContext = context.applicationContext
        val missing = mutableListOf<String>()
        if (automation.requireWifi && !DeviceConditions.isWifiConnected(appContext)) {
            missing += context.getString(R.string.automation_wait_wifi)
        }
        if (automation.requireCharging && !DeviceConditions.isCharging(appContext)) {
            missing += context.getString(R.string.automation_wait_charging)
        }
        if (automation.batteryThreshold > 0) {
            val level = DeviceConditions.getBatteryLevel(appContext)
            if (level in 0..100 && level < automation.batteryThreshold) {
                missing += context.getString(R.string.automation_wait_battery)
            }
        }

        return if (missing.isEmpty()) {
            context.getString(R.string.automation_running_soon)
        } else {
            context.getString(R.string.automation_waiting_for, missing.joinToString(", "))
        }
    }

    fun formatLastRun(
        context: Context,
        lastRun: Long?,
        exitCode: Int?,
    ): String {
        if (lastRun == null) return context.getString(R.string.automation_never_run)

        val relative =
            DateUtils.getRelativeTimeSpanString(
                lastRun,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
            )

        val status =
            when (exitCode) {
                null -> ""
                0 -> " (${context.getString(R.string.status_success)})"
                else -> " (${context.getString(R.string.status_failed)}: $exitCode)"
            }

        return context.getString(R.string.automation_last_run_format, relative) + status
    }
}
