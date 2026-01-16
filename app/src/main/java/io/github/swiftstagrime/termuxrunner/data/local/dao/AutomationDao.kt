package io.github.swiftstagrime.termuxrunner.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.github.swiftstagrime.termuxrunner.data.local.entity.AutomationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AutomationDao {
    @Query("SELECT * FROM automations")
    fun getAllAutomations(): Flow<List<AutomationEntity>>

    @Query("SELECT * FROM automations WHERE isEnabled = 1")
    suspend fun getEnabledAutomations(): List<AutomationEntity>

    @Query("SELECT * FROM automations WHERE id = :id")
    suspend fun getAutomationById(id: Int): AutomationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAutomation(automation: AutomationEntity): Long

    @Update
    suspend fun updateAutomation(automation: AutomationEntity)

    @Delete
    suspend fun deleteAutomation(automation: AutomationEntity)

    @Query("SELECT * FROM automations WHERE scriptId = :scriptId")
    suspend fun getAutomationsForScript(scriptId: Int): List<AutomationEntity>

    @Query("UPDATE automations SET lastExitCode = :exitCode, lastRunTimestamp = :timestamp WHERE id = :automationId")
    suspend fun updateLastResult(automationId: Int, exitCode: Int, timestamp: Long)

    @Query("SELECT * FROM automations")
    suspend fun getAllAutomationsOneShot(): List<AutomationEntity>
}