package io.github.swiftstagrime.termuxrunner.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Automation(
    val id: Int,
    val scriptId: Int,
    val label: String,
    val type: AutomationType,
    val scheduledTimestamp: Long,
    val intervalMillis: Long,
    val daysOfWeek: List<Int>,
    val isEnabled: Boolean,
    val lastRunTimestamp: Long?,
    val nextRunTimestamp: Long?,
    val runtimeArgs: String? = null,
    val runtimeEnv: Map<String, String> = emptyMap(),
    val runtimePrefix: String? = null,
    val runIfMissed: Boolean = true,
    val lastExitCode: Int? = null,
    val requireWifi: Boolean = false,
    val requireCharging: Boolean = false,
    val batteryThreshold: Int = 0
) : Parcelable