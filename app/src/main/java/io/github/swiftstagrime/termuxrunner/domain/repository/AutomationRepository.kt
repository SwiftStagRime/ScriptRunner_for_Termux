package io.github.swiftstagrime.termuxrunner.domain.repository

import io.github.swiftstagrime.termuxrunner.domain.model.Automation
import kotlinx.coroutines.flow.Flow

interface AutomationRepository {
    fun getAllAutomations(): Flow<List<Automation>>

    suspend fun getAutomationById(id: Int): Automation?

    suspend fun saveAutomation(automation: Automation)

    suspend fun deleteAutomation(automation: Automation)

    suspend fun toggleAutomation(
        id: Int,
        enabled: Boolean,
    )

    suspend fun getAutomationsForScript(scriptId: Int): List<Automation>
}
