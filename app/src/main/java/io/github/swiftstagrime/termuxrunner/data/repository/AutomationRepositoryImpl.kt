package io.github.swiftstagrime.termuxrunner.data.repository

import io.github.swiftstagrime.termuxrunner.data.automation.AutomationScheduler
import io.github.swiftstagrime.termuxrunner.data.local.dao.AutomationDao
import io.github.swiftstagrime.termuxrunner.data.local.entity.toAutomationDomain
import io.github.swiftstagrime.termuxrunner.data.local.entity.toEntity
import io.github.swiftstagrime.termuxrunner.domain.model.Automation
import io.github.swiftstagrime.termuxrunner.domain.repository.AutomationRepository
import io.github.swiftstagrime.termuxrunner.domain.util.AutomationTimeCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AutomationRepositoryImpl @Inject constructor(
    private val dao: AutomationDao,
    private val scheduler: AutomationScheduler
) : AutomationRepository {

    override fun getAllAutomations(): Flow<List<Automation>> {
        return dao.getAllAutomations().map { entities ->
            entities.map { it.toAutomationDomain() }
        }
    }

    override suspend fun getAutomationById(id: Int): Automation? {
        return dao.getAutomationById(id)?.toAutomationDomain()
    }

    override suspend fun saveAutomation(automation: Automation) {
        var entity = automation.toEntity()
        val now = System.currentTimeMillis()
        val triggerTime = entity.nextRunTimestamp ?: entity.scheduledTimestamp
        if (triggerTime < now && !entity.runIfMissed) {
            val nextRun = AutomationTimeCalculator.calculateNextRun(entity, now)
            entity = entity.copy(nextRunTimestamp = nextRun)
        }

        val id = dao.insertAutomation(entity)
        val savedEntity = dao.getAutomationById(id.toInt())
        if (savedEntity != null) {
            scheduler.schedule(savedEntity)
        }
    }

    override suspend fun deleteAutomation(automation: Automation) {
        val entity = automation.toEntity()
        scheduler.cancel(entity)
        dao.deleteAutomation(entity)
    }

    override suspend fun toggleAutomation(id: Int, enabled: Boolean) {
        val entity = dao.getAutomationById(id) ?: return
        val updated = entity.copy(isEnabled = enabled)
        dao.updateAutomation(updated)

        if (enabled) {
            scheduler.schedule(updated)
        } else {
            scheduler.cancel(updated)
        }
    }

    override suspend fun getAutomationsForScript(scriptId: Int): List<Automation> {
        return dao.getAutomationsForScript(scriptId).map { it.toAutomationDomain() }
    }
}