package io.github.swiftstagrime.termuxrunner.data.local.dto

import io.github.swiftstagrime.termuxrunner.data.local.entity.AutomationEntity
import io.github.swiftstagrime.termuxrunner.domain.model.Automation
import io.github.swiftstagrime.termuxrunner.domain.model.AutomationType
import kotlinx.serialization.Serializable

@Serializable
data class AutomationExportDto(
    val scriptId: Int,
    val type: AutomationType,
    val scheduledTimestamp: Long,
    val intervalMillis: Long,
    val daysOfWeek: List<Int>,
    val isEnabled: Boolean,
    val runtimeArgs: String?,
    val runtimeEnv: Map<String, String>?,
    val runtimePrefix: String?,
    val label: String,
    val runIfMissed: Boolean,
    val lastExitCode: Int?,
    val requireWifi: Boolean,
    val requireCharging: Boolean,
    val batteryThreshold: Int,
    val lastRunTimestamp: Long? = null,
    val scheduledDayOfMonth: Int? = null,
    val windowStartHour: Int = 0,
    val windowStartMinute: Int = 0,
    val windowEndHour: Int = 23,
    val windowEndMinute: Int = 59,
    val randomDelayMinMillis: Long? = null,
    val randomDelayMaxMillis: Long? = null,
    val automationCode: String? = null,
)

fun AutomationExportDto.toEntity(newScriptId: Int): AutomationEntity =
    AutomationEntity(
        scriptId = newScriptId,
        label = this.label,
        type = this.type,
        scheduledTimestamp = this.scheduledTimestamp,
        intervalMillis = this.intervalMillis,
        daysOfWeek = this.daysOfWeek,
        isEnabled = false,
        runIfMissed = this.runIfMissed,
        lastExitCode = this.lastExitCode,
        runtimeArgs = this.runtimeArgs,
        runtimeEnv = this.runtimeEnv ?: emptyMap(),
        runtimePrefix = this.runtimePrefix,
        requireWifi = this.requireWifi,
        requireCharging = this.requireCharging,
        batteryThreshold = this.batteryThreshold,
        lastRunTimestamp = this.lastRunTimestamp,
        nextRunTimestamp = null,
        scheduledDayOfMonth = this.scheduledDayOfMonth,
        windowStartHour = this.windowStartHour,
        windowStartMinute = this.windowStartMinute,
        windowEndHour = this.windowEndHour,
        windowEndMinute = this.windowEndMinute,
        randomDelayMinMillis = this.randomDelayMinMillis,
        randomDelayMaxMillis = this.randomDelayMaxMillis,
        automationCode = this.automationCode,
    )

fun Automation.toExportDto() =
    AutomationExportDto(
        scriptId = scriptId,
        type = type,
        scheduledTimestamp = scheduledTimestamp,
        intervalMillis = intervalMillis,
        daysOfWeek = daysOfWeek,
        isEnabled = isEnabled,
        runtimeArgs = runtimeArgs,
        runtimeEnv = runtimeEnv,
        runtimePrefix = runtimePrefix,
        label = label,
        runIfMissed = runIfMissed,
        lastExitCode = lastExitCode,
        requireWifi = requireWifi,
        requireCharging = requireCharging,
        batteryThreshold = batteryThreshold,
        lastRunTimestamp = lastRunTimestamp,
        scheduledDayOfMonth = scheduledDayOfMonth,
        windowStartHour = windowStartHour,
        windowStartMinute = windowStartMinute,
        windowEndHour = windowEndHour,
        windowEndMinute = windowEndMinute,
        randomDelayMinMillis = randomDelayMinMillis,
        randomDelayMaxMillis = randomDelayMaxMillis,
        automationCode = automationCode,
    )
