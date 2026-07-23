package io.github.swiftstagrime.termuxrunner.data.repository

import io.github.swiftstagrime.termuxrunner.data.local.dao.AutomationChainDao
import io.github.swiftstagrime.termuxrunner.data.local.entity.toChainEntity
import io.github.swiftstagrime.termuxrunner.data.local.entity.toDomain
import io.github.swiftstagrime.termuxrunner.domain.model.AutomationChain
import io.github.swiftstagrime.termuxrunner.domain.repository.AutomationChainRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AutomationChainRepositoryImpl
    @Inject
    constructor(
        private val dao: AutomationChainDao,
    ) : AutomationChainRepository {
        override fun getAllChains(): Flow<List<AutomationChain>> = dao.getAllChains().map { it.map { entity -> entity.toDomain() } }

        override suspend fun getChainById(id: Int): AutomationChain? = dao.getChainById(id)?.toDomain()

        override suspend fun getChainsByTriggerId(automationId: Int): List<AutomationChain> =
            dao.getChainsByTriggerId(automationId).map { it.toDomain() }

        override suspend fun saveChain(chain: AutomationChain) {
            val entity = chain.toChainEntity()
            dao.insertChain(entity)
        }

        override suspend fun updateChain(chain: AutomationChain) {
            val entity = chain.toChainEntity()
            dao.updateChain(entity)
        }

        override suspend fun deleteChain(chain: AutomationChain) {
            dao.deleteChain(chain.toChainEntity())
        }
    }
