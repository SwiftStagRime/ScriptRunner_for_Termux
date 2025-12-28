package io.github.swiftstagrime.termuxrunner.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.swiftstagrime.termuxrunner.domain.repository.UserPreferencesRepository
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
        val DYNAMIC_COLORS = booleanPreferencesKey("dynamic_colors")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    override val useDynamicColors: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[Keys.DYNAMIC_COLORS] ?: false }

    override val hasCompletedOnboarding: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[Keys.ONBOARDING_COMPLETED] ?: false }

    override suspend fun setDynamicColors(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DYNAMIC_COLORS] = enabled }
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }
}