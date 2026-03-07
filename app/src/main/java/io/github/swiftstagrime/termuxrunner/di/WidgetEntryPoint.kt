package io.github.swiftstagrime.termuxrunner.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.swiftstagrime.termuxrunner.domain.repository.AutomationLogRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.AutomationRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.UserPreferencesRepository
import io.github.swiftstagrime.termuxrunner.domain.usecase.RunScriptUseCase

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun scriptRepository(): ScriptRepository

    fun automationRepository(): AutomationRepository

    fun automationLogRepository(): AutomationLogRepository

    fun userPreferencesRepository(): UserPreferencesRepository

    fun runScriptUseCase(): RunScriptUseCase
}
