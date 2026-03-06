package io.github.swiftstagrime.termuxrunner.data.repository

import io.github.swiftstagrime.termuxrunner.data.local.dao.AutomationLogDao
import io.github.swiftstagrime.termuxrunner.data.local.entity.toDomain
import io.github.swiftstagrime.termuxrunner.data.local.entity.toEntity
import io.github.swiftstagrime.termuxrunner.domain.model.AutomationLog
import io.github.swiftstagrime.termuxrunner.domain.repository.AutomationLogRepository
import io.github.swiftstagrime.termuxrunner.domain.usecase.TriggerGlanceAutomationUpdateUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AutomationLogRepositoryImpl
    @Inject
    constructor(
        private val logDao: AutomationLogDao,
        private val triggerGlanceAutomationUpdateUseCase: TriggerGlanceAutomationUpdateUseCase,
    ) : AutomationLogRepository {
        override fun getLogsForAutomation(automationId: Int): Flow<List<AutomationLog>> =
            logDao.getLogsForAutomation(automationId).map { entities ->
                entities.map { it.toDomain() }
            }

        override suspend fun insertLog(log: AutomationLog) {
            logDao.insertLog(log.toEntity())
            triggerGlanceAutomationUpdateUseCase.invoke()
        }

        override suspend fun deleteLogsForAutomation(automationId: Int) {
            logDao.deleteLogsForAutomation(automationId)
            triggerGlanceAutomationUpdateUseCase.invoke()
        }

        override suspend fun deleteOldLogs(threshold: Long) {
            logDao.deleteOldLogs(threshold)
            triggerGlanceAutomationUpdateUseCase.invoke()
        }

    override fun getRecentLogs(i: Int): Flow<List<AutomationLog>> =
        logDao.getLogs(i).map { entities ->
            entities.map { it.toDomain() }
        }
}
