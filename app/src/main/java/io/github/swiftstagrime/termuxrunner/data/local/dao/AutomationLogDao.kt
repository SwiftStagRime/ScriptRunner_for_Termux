package io.github.swiftstagrime.termuxrunner.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.swiftstagrime.termuxrunner.data.local.entity.AutomationLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AutomationLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AutomationLogEntity)

    @Query("SELECT * FROM automation_logs WHERE automationId = :automationId ORDER BY timestamp DESC LIMIT 50")
    fun getLogsForAutomation(automationId: Int): Flow<List<AutomationLogEntity>>

    @Query("DELETE FROM automation_logs WHERE automationId = :automationId")
    suspend fun deleteLogsForAutomation(automationId: Int)

    @Query("DELETE FROM automation_logs WHERE timestamp < :threshold")
    suspend fun deleteOldLogs(threshold: Long)

    @Delete
    suspend fun deleteLog(log: AutomationLogEntity)
}