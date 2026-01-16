package io.github.swiftstagrime.termuxrunner.domain.util

import android.content.Context
import android.text.format.DateUtils
import io.github.swiftstagrime.termuxrunner.R

object AutomationFormatter {
    fun formatNextRun(
        context: Context,
        nextRun: Long?,
    ): String {
        if (nextRun == null) return context.getString(R.string.automation_not_scheduled)

        val now = System.currentTimeMillis()
        val duration = nextRun - now

        return if (duration <= 0) {
            context.getString(R.string.automation_running_soon)
        } else {
            val relative =
                DateUtils.getRelativeTimeSpanString(
                    nextRun,
                    now,
                    DateUtils.MINUTE_IN_MILLIS,
                )
            context.getString(R.string.automation_next_run_format, relative)
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
