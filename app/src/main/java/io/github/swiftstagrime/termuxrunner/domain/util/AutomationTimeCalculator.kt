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
                if (automation.intervalMillis <= 0) return null
                var next = automation.nextRunTimestamp ?: baseTime
                if (next <= fromTime) {
                    val diff = fromTime - next + 1
                    next += (diff / automation.intervalMillis + 1) * automation.intervalMillis
                }
                next
            }

            AutomationType.WEEKLY -> {
                calculateNextWeeklyTimestamp(automation.daysOfWeek, baseTime, fromTime)
            }

            AutomationType.BOOT -> {
                null
            }

            AutomationType.MONTHLY -> {
                val dayOfMonth = automation.scheduledDayOfMonth ?: return null
                if (dayOfMonth < 1 || dayOfMonth > 31) return null
                calculateNextMonthlyTimestamp(dayOfMonth, baseTime, fromTime)
            }

            AutomationType.TIME_WINDOW -> {
                calculateRandomTimeInWindow(
                    automation.windowStartHour,
                    automation.windowStartMinute,
                    automation.windowEndHour,
                    automation.windowEndMinute,
                    fromTime,
                )
            }

            AutomationType.RANDOM_DELAY -> {
                val minDelay = automation.randomDelayMinMillis ?: 0L
                val maxDelay = automation.randomDelayMaxMillis ?: (minDelay * 2)
                if (maxDelay <= minDelay) return null
                val randomDelay = (minDelay..maxDelay).random()
                fromTime + randomDelay
            }

            // Event-based types don't use scheduled timestamps
            AutomationType.SCREEN_ON,
            AutomationType.SCREEN_OFF,
            AutomationType.NETWORK_CONNECTED,
            AutomationType.NETWORK_DISCONNECTED,
            AutomationType.USB_CONNECTED,
            AutomationType.USB_DISCONNECTED,
            -> {
                null
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
            } else {
                return@repeat
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

    private fun calculateNextMonthlyTimestamp(
        dayOfMonth: Int,
        scheduledTime: Long,
        fromTime: Long,
    ): Long? {
        val target =
            Calendar.getInstance().apply {
                timeInMillis = fromTime
                val calScheduled = Calendar.getInstance().apply { timeInMillis = scheduledTime }
                set(Calendar.HOUR_OF_DAY, calScheduled.get(Calendar.HOUR_OF_DAY))
                set(Calendar.MINUTE, calScheduled.get(Calendar.MINUTE))
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

        for (i in 0..24) {
            val maxDay = target.getActualMaximum(Calendar.DAY_OF_MONTH)
            if (dayOfMonth <= maxDay) {
                target.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            } else {
                target.add(Calendar.MONTH, 1)
                continue
            }

            if (target.timeInMillis > fromTime) {
                return target.timeInMillis
            }
            target.add(Calendar.MONTH, 1)
        }
        return null
    }

    private fun calculateRandomTimeInWindow(
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
        fromTime: Long,
    ): Long? {
        val target =
            Calendar.getInstance().apply {
                timeInMillis = fromTime
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

        // Calculate window boundaries in minutes from midnight
        var startMinutes = startHour * 60 + startMinute
        var endMinutes = endHour * 60 + endMinute

        if (endMinutes <= startMinutes) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }

        val range = endMinutes - startMinutes
        if (range < 5) return null

        val randomOffset = (0 until range).random()
        val selectedMinute = startMinutes + randomOffset

        target.set(Calendar.HOUR_OF_DAY, selectedMinute / 60)
        target.set(Calendar.MINUTE, selectedMinute % 60)

        if (target.timeInMillis <= fromTime) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }

        return target.timeInMillis
    }
}
