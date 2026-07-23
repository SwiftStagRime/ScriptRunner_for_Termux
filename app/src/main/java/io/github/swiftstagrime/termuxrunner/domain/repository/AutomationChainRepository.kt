package io.github.swiftstagrime.termuxrunner.domain.repository

import io.github.swiftstagrime.termuxrunner.domain.model.AutomationChain
import kotlinx.coroutines.flow.Flow

interface AutomationChainRepository {
    fun getAllChains(): Flow<List<AutomationChain>>

    suspend fun getChainById(id: Int): AutomationChain?

    suspend fun getChainsByTriggerId(automationId: Int): List<AutomationChain>

    suspend fun saveChain(chain: AutomationChain)

    suspend fun updateChain(chain: AutomationChain)

    suspend fun deleteChain(chain: AutomationChain)
}
