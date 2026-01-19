package io.github.swiftstagrime.termuxrunner.domain.util

import io.github.swiftstagrime.termuxrunner.data.local.entity.AutomationEntity
import io.github.swiftstagrime.termuxrunner.domain.model.AutomationType
import java.util.Calendar

object AutomationTimeCalculator {
    fun calculateNextRun(
        automation: AutomationEntity,
        fromTime: Long = System.currentTimeMillis(),
    ): Long? {
        val baseTime = automation.scheduledTimestamp

        return when (automation.type) {
            AutomationType.ONE_TIME -> {
                if (automation.scheduledTimestamp > fromTime) automation.scheduledTimestamp else null
            }

            AutomationType.PERIODIC -> {
                var next = automation.nextRunTimestamp ?: baseTime
                while (next <= fromTime) {
                    next += automation.intervalMillis
                }
                next
            }

            AutomationType.WEEKLY -> {
                calculateNextWeeklyTimestamp(automation.daysOfWeek, baseTime, fromTime)
            }
        }
    }

    fun getNextRuns(
        automation: AutomationEntity,
        count: Int = 3,
    ): List<Long> {
        val runs = mutableListOf<Long>()
        var lastFoundTime = System.currentTimeMillis()

        repeat(count) {
            val next = calculateNextRun(automation, lastFoundTime)
            if (next != null) {
                runs.add(next)
                lastFoundTime = next
            }
        }
        return runs
    }

    private fun calculateNextWeeklyTimestamp(
        allowedDays: List<Int>,
        scheduledTime: Long,
        fromTime: Long,
    ): Long? {
        if (allowedDays.isEmpty()) return null

        val target =
            Calendar.getInstance().apply {
                val calScheduled = Calendar.getInstance().apply { timeInMillis = scheduledTime }
                timeInMillis = fromTime
                set(Calendar.HOUR_OF_DAY, calScheduled.get(Calendar.HOUR_OF_DAY))
                set(Calendar.MINUTE, calScheduled.get(Calendar.MINUTE))
                set(Calendar.SECOND, calScheduled.get(Calendar.SECOND))
                set(Calendar.MILLISECOND, 0)
            }

        for (i in 0..14) {
            val currentDayOfWeek = target.get(Calendar.DAY_OF_WEEK)
            if (allowedDays.contains(currentDayOfWeek) && target.timeInMillis > fromTime) {
                return target.timeInMillis
            }
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return null
    }
}
