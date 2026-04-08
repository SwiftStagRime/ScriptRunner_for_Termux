package io.github.swiftstagrime.termuxrunner.domain.repository

import io.github.swiftstagrime.termuxrunner.domain.model.CustomTheme
import kotlinx.coroutines.flow.Flow

interface CustomThemeRepository {
    fun getAllCustomThemes(): Flow<List<CustomTheme>>

    suspend fun getThemeById(id: Int): CustomTheme?

    suspend fun saveTheme(theme: CustomTheme): Long

    suspend fun deleteTheme(theme: CustomTheme)

    fun getThemeByIdFlow(id: Int): Flow<CustomTheme?>
}
