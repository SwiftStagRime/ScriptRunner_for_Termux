package io.github.swiftstagrime.termuxrunner.data.local.dto

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
    val batteryThreshold: Int
)

fun Automation.toExportDto() = AutomationExportDto(
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
    batteryThreshold = batteryThreshold
)