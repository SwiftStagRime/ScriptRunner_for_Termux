package io.github.swiftstagrime.termuxrunner.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.swiftstagrime.termuxrunner.domain.model.Automation
import io.github.swiftstagrime.termuxrunner.domain.model.AutomationType

@Entity(
    tableName = "automations",
    foreignKeys = [
        ForeignKey(
            entity = ScriptEntity::class,
            parentColumns = ["id"],
            childColumns = ["scriptId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("scriptId")],
)
data class AutomationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val scriptId: Int,
    val label: String,
    val type: AutomationType,
    val scheduledTimestamp: Long,
    val intervalMillis: Long = 0L,
    val daysOfWeek: List<Int>,
    val isEnabled: Boolean = true,
    val runIfMissed: Boolean = true,
    val lastRunTimestamp: Long? = null,
    val lastExitCode: Int? = null,
    val nextRunTimestamp: Long? = null,
    val runtimeArgs: String? = null,
    val runtimeEnv: Map<String, String> = emptyMap(),
    val runtimePrefix: String? = null,
    val requireWifi: Boolean = false,
    val requireCharging: Boolean = false,
    val batteryThreshold: Int = 0,
)

fun AutomationEntity.toAutomationDomain() =
    Automation(
        id = id,
        scriptId = scriptId,
        type = type,
        scheduledTimestamp = scheduledTimestamp,
        intervalMillis = intervalMillis,
        daysOfWeek = daysOfWeek,
        isEnabled = isEnabled,
        lastRunTimestamp = lastRunTimestamp,
        runtimeArgs = runtimeArgs,
        runtimeEnv = runtimeEnv,
        runtimePrefix = runtimePrefix,
        label = label,
        runIfMissed = runIfMissed,
        lastExitCode = lastExitCode,
        nextRunTimestamp = nextRunTimestamp,
        requireWifi = requireWifi,
        requireCharging = requireCharging,
        batteryThreshold = batteryThreshold,
    )

fun Automation.toEntity() =
    AutomationEntity(
        id = id,
        scriptId = scriptId,
        type = type,
        scheduledTimestamp = scheduledTimestamp,
        intervalMillis = intervalMillis,
        daysOfWeek = daysOfWeek,
        isEnabled = isEnabled,
        lastRunTimestamp = lastRunTimestamp,
        nextRunTimestamp = nextRunTimestamp,
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
