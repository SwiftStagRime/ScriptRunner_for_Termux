package io.github.swiftstagrime.termuxrunner.domain.repository

import io.github.swiftstagrime.termuxrunner.ui.theme.AppTheme
import io.github.swiftstagrime.termuxrunner.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val hasCompletedOnboarding: Flow<Boolean>
    val selectedAccent: Flow<AppTheme>
    val selectedMode: Flow<ThemeMode>
    suspend fun setMode(mode: ThemeMode)
    suspend fun setAccent(accent: AppTheme)
    suspend fun setOnboardingCompleted(completed: Boolean)
    fun getScriptIdForTile(tileIndex: Int): Flow<Int?>
    suspend fun setScriptIdForTile(tileIndex: Int, scriptId: Int?)
}