package io.github.swiftstagrime.termuxrunner.ui.features.automation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import io.github.swiftstagrime.termuxrunner.domain.model.AutomationType
import io.github.swiftstagrime.termuxrunner.domain.model.Script
import java.util.Calendar

private const val MILLIS_IN_MINUTE = 60_000L
private const val DEFAULT_INTERVAL_MINUTES = 60L
class AutomationConfigState(
    script: Script,
    initialLabel: String = script.name,
    initialType: AutomationType = AutomationType.ONE_TIME,
    initialRunIfMissed: Boolean = true,
    initialDate: Long = System.currentTimeMillis(),
    initialHour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
    initialMinute: Int = Calendar.getInstance().get(Calendar.MINUTE),
    initialDays: List<Int> = emptyList(),
    initialInterval: String = DEFAULT_INTERVAL_MINUTES.toString(),
    initialWifi: Boolean = false,
    initialCharging: Boolean = false,
    initialBattery: Int = 0
) {
    var label by mutableStateOf(initialLabel)
    var type by mutableStateOf(initialType)
    var runIfMissed by mutableStateOf(initialRunIfMissed)
    var selectedDate by mutableLongStateOf(initialDate)
    var selectedHour by mutableIntStateOf(initialHour)
    var selectedMinute by mutableIntStateOf(initialMinute)
    var selectedDays by mutableStateOf(initialDays)
    var intervalValue by mutableStateOf(initialInterval)
    var requireWifi by mutableStateOf(initialWifi)
    var requireCharging by mutableStateOf(initialCharging)
    var batteryThreshold by mutableIntStateOf(initialBattery)

    fun toSaveParams(scriptId: Int): AutomationSaveParams {
        val calendar = Calendar.getInstance().apply {
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
            interval = (intervalValue.toLongOrNull() ?: DEFAULT_INTERVAL_MINUTES) * MILLIS_IN_MINUTE,
            days = selectedDays,
            runIfMissed = runIfMissed,
            requireWifi = requireWifi,
            requireCharging = requireCharging,
            batteryThreshold = batteryThreshold
        )
    }

    companion object {
        fun Saver(script: Script): Saver<AutomationConfigState, *> = Saver(
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
                    state.batteryThreshold
                )
            },
            restore = { saved ->
                val list = saved as List<*>
                AutomationConfigState(
                    script = script,
                    initialLabel = list[0] as String,
                    initialType = AutomationType.valueOf(list[1] as String),
                    initialRunIfMissed = list[2] as Boolean,
                    initialDate = list[3] as Long,
                    initialHour = list[4] as Int,
                    initialMinute = list[5] as Int,
                    initialDays = (list[6] as IntArray).toList(),
                    initialInterval = list[7] as String,
                    initialWifi = list[8] as Boolean,
                    initialCharging = list[9] as Boolean,
                    initialBattery = list[10] as Int
                )
            }
        )
    }
}