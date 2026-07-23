package io.github.swiftstagrime.termuxrunner.domain.repository

import io.github.swiftstagrime.termuxrunner.domain.model.ScriptExecution
import kotlinx.coroutines.flow.Flow

interface ScriptExecutionRepository {
    suspend fun insert(execution: ScriptExecution)

    fun getExecutionsForScript(scriptId: Int): Flow<List<ScriptExecution>>

    fun getRecentExecutions(limit: Int = 100): Flow<List<ScriptExecution>>

    fun getAllExecutions(): Flow<List<ScriptExecution>>

    suspend fun deleteOldRecords(threshold: Long)

    suspend fun deleteForScript(scriptId: Int)

    suspend fun clearAll()

    fun getFailureCount(): Flow<Int>

    fun getTotalCount(): Flow<Int>
}
