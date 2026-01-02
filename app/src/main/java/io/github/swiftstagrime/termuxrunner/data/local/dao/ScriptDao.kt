package io.github.swiftstagrime.termuxrunner.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import io.github.swiftstagrime.termuxrunner.data.local.entity.ScriptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScriptDao {

    @Query("SELECT * FROM scripts ORDER BY id DESC")
    fun getAllScripts(): Flow<List<ScriptEntity>>

    @Query("SELECT * FROM scripts WHERE id = :id")
    suspend fun getScriptById(id: Int): ScriptEntity?

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertScript(script: ScriptEntity)

    @Delete
    suspend fun deleteScript(script: ScriptEntity)

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertScripts(scripts: List<ScriptEntity>)

    @Query("SELECT * FROM scripts")
    suspend fun getAllScriptsOneShot(): List<ScriptEntity>

    @Query("UPDATE scripts SET orderIndex = :orderIndex WHERE id = :scriptId")
    suspend fun updateScriptOrder(scriptId: Int, orderIndex: Int)

    @Transaction
    suspend fun updateScriptsOrder(orders: List<Pair<Int, Int>>) {
        orders.forEach { (id, index) ->
            updateScriptOrder(id, index)
        }
    }
}