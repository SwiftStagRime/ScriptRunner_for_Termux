package io.github.swiftstagrime.termuxrunner.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.github.swiftstagrime.termuxrunner.data.local.entity.AutomationChainEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AutomationChainDao {
    @Query("SELECT * FROM automation_chains")
    fun getAllChains(): Flow<List<AutomationChainEntity>>

    @Query("SELECT * FROM automation_chains WHERE id = :id")
    suspend fun getChainById(id: Int): AutomationChainEntity?

    @Query("SELECT * FROM automation_chains WHERE triggerAutomationId = :automationId")
    suspend fun getChainsByTriggerId(automationId: Int): List<AutomationChainEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChain(chain: AutomationChainEntity): Long

    @Update
    suspend fun updateChain(chain: AutomationChainEntity)

    @Delete
    suspend fun deleteChain(chain: AutomationChainEntity)

    @Query("DELETE FROM automation_chains WHERE id = :id")
    suspend fun deleteChainById(id: Int)
}
