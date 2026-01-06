package io.github.swiftstagrime.termuxrunner.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.swiftstagrime.termuxrunner.domain.repository.UserPreferencesRepository
import io.github.swiftstagrime.termuxrunner.ui.theme.AppTheme
import io.github.swiftstagrime.termuxrunner.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Manages general app settings and UI preferences using Jetpack DataStore.
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

class UserPreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : UserPreferencesRepository {

    private object Keys {
        val THEME_ACCENT = stringPreferencesKey("theme_accent")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    override val selectedAccent: Flow<AppTheme> = context.dataStore.data
        .map { prefs ->
            val name = prefs[Keys.THEME_ACCENT] ?: AppTheme.GREEN.name
            AppTheme.valueOf(name)
        }

    override val selectedMode: Flow<ThemeMode> = context.dataStore.data
        .map { prefs ->
            val name = prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name
            ThemeMode.valueOf(name)
        }

    override suspend fun setAccent(accent: AppTheme) {
        context.dataStore.edit { it[Keys.THEME_ACCENT] = accent.name }
    }

    override suspend fun setMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    override val hasCompletedOnboarding: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[Keys.ONBOARDING_COMPLETED] ?: false }


    override suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    private fun getTileKey(index: Int) = intPreferencesKey("qs_tile_${index}_script_id")

    override fun getScriptIdForTile(tileIndex: Int): Flow<Int?> = context.dataStore.data
        .map { preferences -> preferences[getTileKey(tileIndex)] }

    override suspend fun setScriptIdForTile(tileIndex: Int, scriptId: Int?) {
        context.dataStore.edit { preferences ->
            val key = getTileKey(tileIndex)
            if (scriptId == null) {
                preferences.remove(key)
            } else {
                preferences[key] = scriptId
            }
        }
    }
}