package io.github.swiftstagrime.termuxrunner.data.repository

import io.github.swiftstagrime.termuxrunner.data.local.dao.ScriptExecutionDao
import io.github.swiftstagrime.termuxrunner.data.local.entity.ScriptExecutionEntity
import io.github.swiftstagrime.termuxrunner.domain.model.ExecutionSource
import io.github.swiftstagrime.termuxrunner.domain.model.ScriptExecution
import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptExecutionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ScriptExecutionRepositoryImpl
    @Inject
    constructor(
        private val dao: ScriptExecutionDao,
    ) : ScriptExecutionRepository {
        override suspend fun insert(execution: ScriptExecution) {
            dao.insert(execution.toEntity())
        }

        override fun getExecutionsForScript(scriptId: Int): Flow<List<ScriptExecution>> =
            dao.getExecutionsForScript(scriptId).map { it.map { entity -> entity.toDomain() } }

        override fun getRecentExecutions(limit: Int): Flow<List<ScriptExecution>> =
            dao.getRecentExecutions(limit).map { it.map { entity -> entity.toDomain() } }

        override fun getAllExecutions(): Flow<List<ScriptExecution>> = dao.getAllExecutions().map { it.map { entity -> entity.toDomain() } }

        override suspend fun deleteOldRecords(threshold: Long) {
            dao.deleteOldRecords(threshold)
        }

        override suspend fun deleteForScript(scriptId: Int) {
            dao.deleteForScript(scriptId)
        }

        override suspend fun clearAll() {
            dao.clearAll()
        }

        override fun getFailureCount(): Flow<Int> = dao.getFailureCount()

        override fun getTotalCount(): Flow<Int> = dao.getTotalCount()
    }

private fun ScriptExecution.toEntity() =
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
                ExecutionSource.MANUAL -> ScriptExecutionEntity.ExecutionSource.MANUAL
                ExecutionSource.AUTOMATION -> ScriptExecutionEntity.ExecutionSource.AUTOMATION
                ExecutionSource.TILE -> ScriptExecutionEntity.ExecutionSource.TILE
                ExecutionSource.WIDGET -> ScriptExecutionEntity.ExecutionSource.WIDGET
                ExecutionSource.SHORTCUT -> ScriptExecutionEntity.ExecutionSource.SHORTCUT
            },
        errorMessage = errorMessage,
    )
