package io.github.swiftstagrime.termuxrunner.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.swiftstagrime.termuxrunner.data.local.entity.ScriptVersionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScriptVersionDao {
    @Query("SELECT * FROM script_versions WHERE scriptId = :scriptId ORDER BY timestamp DESC")
    fun getVersionsByScriptId(scriptId: Int): Flow<List<ScriptVersionEntity>>

    @Query("SELECT * FROM script_versions WHERE scriptId = :scriptId ORDER BY timestamp DESC")
    suspend fun getVersionsByScriptIdOneShot(scriptId: Int): List<ScriptVersionEntity>

    @Query("SELECT * FROM script_versions ORDER BY timestamp DESC")
    fun getAllVersions(): Flow<List<ScriptVersionEntity>>

    @Query("SELECT * FROM script_versions ORDER BY timestamp DESC")
    suspend fun getAllVersionsOneShot(): List<ScriptVersionEntity>

    @Query("SELECT * FROM script_versions WHERE id = :id LIMIT 1")
    suspend fun getVersionById(id: Int): ScriptVersionEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertVersion(version: ScriptVersionEntity): Long

    @Delete
    suspend fun deleteVersion(version: ScriptVersionEntity)

    @Query("DELETE FROM script_versions WHERE id = :id")
    suspend fun deleteVersionById(id: Int)

    @Query("DELETE FROM script_versions WHERE scriptId = :scriptId")
    suspend fun deleteVersionsByScriptId(scriptId: Int)

    @Query("SELECT * FROM script_versions WHERE scriptId = :scriptId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestVersion(scriptId: Int): ScriptVersionEntity?
}
