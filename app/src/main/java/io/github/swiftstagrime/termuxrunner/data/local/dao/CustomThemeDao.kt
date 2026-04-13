package io.github.swiftstagrime.termuxrunner.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.swiftstagrime.termuxrunner.data.local.entity.CustomThemeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomThemeDao {
    @Query("SELECT * FROM custom_themes")
    fun getAllThemes(): Flow<List<CustomThemeEntity>>

    @Query("SELECT * FROM custom_themes WHERE id = :id")
    suspend fun getThemeById(id: Int): CustomThemeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTheme(theme: CustomThemeEntity): Long

    @Delete
    suspend fun deleteTheme(theme: CustomThemeEntity)

    @Query("SELECT * FROM custom_themes WHERE id = :id")
    fun getThemeByIdFlow(id: Int): Flow<CustomThemeEntity?>

    @Query("SELECT * FROM custom_themes")
    suspend fun getAllThemesOneShot(): List<CustomThemeEntity>
}
