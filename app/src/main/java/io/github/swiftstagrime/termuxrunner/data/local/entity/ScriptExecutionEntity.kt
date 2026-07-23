package io.github.swiftstagrime.termuxrunner.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.swiftstagrime.termuxrunner.domain.model.ExecutionSource as DomainExecutionSource
import io.github.swiftstagrime.termuxrunner.domain.model.ScriptExecution as DomainScriptExecution

@Entity(
    tableName = "script_executions",
    indices = [Index("scriptId"), Index("timestamp")],
)
data class ScriptExecutionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scriptId: Int,
    val scriptName: String,
    val timestamp: Long,
    val exitCode: Int,
    val durationMs: Long? = null,
    val runtimeArgs: String? = null,
    val source: ExecutionSource = ExecutionSource.MANUAL,
    val errorMessage: String? = null,
) {
    enum class ExecutionSource(
        val value: String,
    ) {
        MANUAL("MANUAL"),
        AUTOMATION("AUTOMATION"),
        TILE("TILE"),
        WIDGET("WIDGET"),
        SHORTCUT("SHORTCUT"),
    }

    fun toDomain() =
        DomainScriptExecution(
            id = id,
            scriptId = scriptId,
            scriptName = scriptName,
            timestamp = timestamp,
            exitCode = exitCode,
            durationMs = durationMs,
            runtimeArgs = runtimeArgs,
            source =
                when (source) {
                    ExecutionSource.MANUAL -> DomainExecutionSource.MANUAL
                    ExecutionSource.AUTOMATION -> DomainExecutionSource.AUTOMATION
                    ExecutionSource.TILE -> DomainExecutionSource.TILE
                    ExecutionSource.WIDGET -> DomainExecutionSource.WIDGET
                    ExecutionSource.SHORTCUT -> DomainExecutionSource.SHORTCUT
                },
            errorMessage = errorMessage,
        )
}

fun DomainScriptExecution.toEntity() =
    ScriptExecutionEntity(
        id = id,
        scriptId = scriptId,
        scriptName = scriptName,
        timestamp = timestamp,
        exitCode = exitCode,
        durationMs = durationMs,
        runtimeArgs = runtimeArgs,
        source =
            when (source) {
                DomainExecutionSource.MANUAL -> ScriptExecutionEntity.ExecutionSource.MANUAL
                DomainExecutionSource.AUTOMATION -> ScriptExecutionEntity.ExecutionSource.AUTOMATION
                DomainExecutionSource.TILE -> ScriptExecutionEntity.ExecutionSource.TILE
                DomainExecutionSource.WIDGET -> ScriptExecutionEntity.ExecutionSource.WIDGET
                DomainExecutionSource.SHORTCUT -> ScriptExecutionEntity.ExecutionSource.SHORTCUT
            },
        errorMessage = errorMessage,
    )
