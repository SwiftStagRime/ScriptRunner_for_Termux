package io.github.swiftstagrime.termuxrunner.domain.repository

import io.github.swiftstagrime.termuxrunner.domain.model.AutomationLog
import kotlinx.coroutines.flow.Flow

interface AutomationLogRepository {
    fun getLogsForAutomation(automationId: Int): Flow<List<AutomationLog>>
    suspend fun insertLog(log: AutomationLog)
    suspend fun deleteLogsForAutomation(automationId: Int)
    suspend fun deleteOldLogs(threshold: Long)
}