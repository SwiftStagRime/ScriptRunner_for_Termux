package io.github.swiftstagrime.termuxrunner.ui.features.automation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import io.github.swiftstagrime.termuxrunner.domain.model.Automation
import io.github.swiftstagrime.termuxrunner.domain.model.AutomationType
import io.github.swiftstagrime.termuxrunner.domain.model.Script
import io.github.swiftstagrime.termuxrunner.domain.model.TriggerMode
import io.github.swiftstagrime.termuxrunner.domain.model.triggerMode
import java.util.Calendar

private const val MILLIS_IN_MINUTE = 60_000L
private const val DEFAULT_INTERVAL_MINUTES = 60L
private const val MILLIS_IN_SECOND = 1000L
private const val DEFAULT_END_HOUR = 23
private const val DEFAULT_END_MINUTE = 59

class AutomationConfigState(
    script: Script,
    initialAutomation: Automation? = null,
) {
    private val initialTimestamp =
        if (initialAutomation?.type?.triggerMode == TriggerMode.SCHEDULE) {
            initialAutomation.scheduledTimestamp
        } else {
            System.currentTimeMillis()
        }

    private val initialCalendar = Calendar.getInstance().apply { timeInMillis = initialTimestamp }

    var label by mutableStateOf(initialAutomation?.label ?: script.name)
    var type by mutableStateOf(initialAutomation?.type ?: AutomationType.ONE_TIME)
    var runIfMissed by mutableStateOf(initialAutomation?.runIfMissed ?: true)
    var selectedDate by mutableLongStateOf(initialTimestamp)
    var selectedHour by mutableIntStateOf(initialCalendar.get(Calendar.HOUR_OF_DAY))
    var selectedMinute by mutableIntStateOf(initialCalendar.get(Calendar.MINUTE))
    var selectedDays by mutableStateOf(initialAutomation?.daysOfWeek ?: emptyList())
    var intervalValue by mutableStateOf(
        (
            (initialAutomation?.intervalMillis ?: DEFAULT_INTERVAL_MINUTES * MILLIS_IN_MINUTE) /
                MILLIS_IN_MINUTE
        ).toString(),
    )
    var requireWifi by mutableStateOf(initialAutomation?.requireWifi ?: false)
    var requireCharging by mutableStateOf(initialAutomation?.requireCharging ?: false)
    var batteryThreshold by mutableIntStateOf(initialAutomation?.batteryThreshold ?: 0)
    var scheduledDayOfMonth by mutableStateOf(initialAutomation?.scheduledDayOfMonth?.toString() ?: "")
    var windowStartHour by mutableIntStateOf(initialAutomation?.windowStartHour ?: 0)
    var windowStartMinute by mutableIntStateOf(initialAutomation?.windowStartMinute ?: 0)
    var windowEndHour by mutableIntStateOf(initialAutomation?.windowEndHour ?: DEFAULT_END_HOUR)
    var windowEndMinute by mutableIntStateOf(initialAutomation?.windowEndMinute ?: DEFAULT_END_MINUTE)
    var randomDelayMinValue by mutableStateOf(
        initialAutomation?.randomDelayMinMillis?.div(MILLIS_IN_SECOND)?.toString() ?: "",
    )
    var randomDelayMaxValue by mutableStateOf(
        initialAutomation?.randomDelayMaxMillis?.div(MILLIS_IN_SECOND)?.toString() ?: "",
    )
    var automationCode by mutableStateOf(initialAutomation?.automationCode ?: "")

    fun toSaveParams(scriptId: Int): AutomationSaveParams {
        val calendar =
            Calendar.getInstance().apply {
                timeInMillis = selectedDate
                set(Calendar.HOUR_OF_DAY, selectedHour)
                set(Calendar.MINUTE, selectedMinute)
                set(Calendar.SECOND, 0)
            }
        return AutomationSaveParams(
            scriptId = scriptId,
            label = label.ifBlank { "Untitled" },
            type = type,
            timestamp = calendar.timeInMillis,
            interval =
                (
                    intervalValue.toLongOrNull()
                        ?: DEFAULT_INTERVAL_MINUTES
                ) * MILLIS_IN_MINUTE,
            days = selectedDays,
            runIfMissed = runIfMissed,
            requireWifi = requireWifi,
            requireCharging = requireCharging,
            batteryThreshold = batteryThreshold,
            scheduledDayOfMonth = scheduledDayOfMonth.toIntOrNull(),
            windowStartHour = windowStartHour,
            windowStartMinute = windowStartMinute,
            windowEndHour = windowEndHour,
            windowEndMinute = windowEndMinute,
            randomDelayMinMillis = (randomDelayMinValue.toLongOrNull() ?: 0L) * 1000,
            randomDelayMaxMillis = (randomDelayMaxValue.toLongOrNull() ?: 0L) * 1000,
            automationCode = automationCode.trim(),
        )
    }

    companion object {
        fun Saver(script: Script): Saver<AutomationConfigState, *> =
            Saver(
                save = { state ->
                    listOf(
                        state.label,
                        state.type.name,
                        state.runIfMissed,
                        state.selectedDate,
                        state.selectedHour,
                        state.selectedMinute,
                        state.selectedDays.toIntArray(),
                        state.intervalValue,
                        state.requireWifi,
                        state.requireCharging,
                        state.batteryThreshold,
                        state.scheduledDayOfMonth,
                        state.windowStartHour,
                        state.windowStartMinute,
                        state.windowEndHour,
                        state.windowEndMinute,
                        state.randomDelayMinValue,
                        state.randomDelayMaxValue,
                        state.automationCode,
                    )
                },
                restore = { saved ->
                    val list = saved as List<*>
                    val state = AutomationConfigState(script)
                    state.label = list[0] as String
                    state.type = AutomationType.valueOf(list[1] as String)
                    state.runIfMissed = list[2] as Boolean
                    state.selectedDate = list[3] as Long
                    state.selectedHour = list[4] as Int
                    state.selectedMinute = list[5] as Int
                    state.selectedDays = (list[6] as IntArray).toList()
                    state.intervalValue = list[7] as String
                    state.requireWifi = list[8] as Boolean
                    state.requireCharging = list[9] as Boolean
                    state.batteryThreshold = list[10] as Int
                    state.scheduledDayOfMonth = list[11] as String
                    state.windowStartHour = list[12] as Int
                    state.windowStartMinute = list[13] as Int
                    state.windowEndHour = list[14] as Int
                    state.windowEndMinute = list[15] as Int
                    state.randomDelayMinValue = list[16] as String
                    state.randomDelayMaxValue = list[17] as String
                    state.automationCode = list[18] as String
                    state
                },
            )
    }
}
