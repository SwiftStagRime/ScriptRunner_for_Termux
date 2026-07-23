package io.github.swiftstagrime.termuxrunner.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.swiftstagrime.termuxrunner.data.local.entity.ScriptExecutionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScriptExecutionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(execution: ScriptExecutionEntity): Long

    @Query("SELECT * FROM script_executions WHERE scriptId = :scriptId ORDER BY timestamp DESC LIMIT 50")
    fun getExecutionsForScript(scriptId: Int): Flow<List<ScriptExecutionEntity>>

    @Query("SELECT * FROM script_executions ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentExecutions(limit: Int): Flow<List<ScriptExecutionEntity>>

    @Query("SELECT * FROM script_executions ORDER BY timestamp DESC")
    fun getAllExecutions(): Flow<List<ScriptExecutionEntity>>

    @Query(
        "SELECT se.* FROM script_executions se " +
            "INNER JOIN scripts s ON se.scriptId = s.id " +
            "ORDER BY se.timestamp DESC",
    )
    fun getExecutionsWithScripts(): Flow<List<ScriptExecutionEntity>>

    @Query("DELETE FROM script_executions WHERE timestamp < :threshold")
    suspend fun deleteOldRecords(threshold: Long)

    @Query("DELETE FROM script_executions WHERE scriptId = :scriptId")
    suspend fun deleteForScript(scriptId: Int)

    @Query("DELETE FROM script_executions")
    suspend fun clearAll()

    @Delete
    suspend fun delete(execution: ScriptExecutionEntity)

    @Query("SELECT COUNT(*) FROM script_executions WHERE exitCode != 0")
    fun getFailureCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM script_executions")
    fun getTotalCount(): Flow<Int>
}
