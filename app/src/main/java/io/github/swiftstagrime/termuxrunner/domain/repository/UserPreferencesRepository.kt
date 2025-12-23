package io.github.swiftstagrime.termuxrunner.domain.repository

import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val useDynamicColors: Flow<Boolean>
    val hasCompletedOnboarding: Flow<Boolean>

    suspend fun setDynamicColors(enabled: Boolean)
    suspend fun setOnboardingCompleted(completed: Boolean)
}