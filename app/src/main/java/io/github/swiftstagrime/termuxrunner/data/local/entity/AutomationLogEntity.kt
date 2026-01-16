package io.github.swiftstagrime.termuxrunner.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.swiftstagrime.termuxrunner.domain.model.AutomationLog

@Entity(
    tableName = "automation_logs",
    foreignKeys = [
        ForeignKey(
            entity = AutomationEntity::class,
            parentColumns = ["id"],
            childColumns = ["automationId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("automationId")],
)
data class AutomationLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val automationId: Int,
    val timestamp: Long,
    val exitCode: Int,
    val message: String? = null,
)

fun AutomationLogEntity.toDomain() =
    AutomationLog(
        id = id,
        automationId = automationId,
        timestamp = timestamp,
        exitCode = exitCode,
        message = message,
    )

fun AutomationLog.toEntity() =
    AutomationLogEntity(
        id = id,
        automationId = automationId,
        timestamp = timestamp,
        exitCode = exitCode,
        message = message,
    )
