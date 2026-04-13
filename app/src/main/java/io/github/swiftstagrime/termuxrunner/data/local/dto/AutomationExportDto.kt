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
)
fun AutomationExportDto.toEntity(newScriptId: Int): AutomationEntity {
    return AutomationEntity(
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
        lastRunTimestamp = null,
        nextRunTimestamp = null
    )
}

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
    )
