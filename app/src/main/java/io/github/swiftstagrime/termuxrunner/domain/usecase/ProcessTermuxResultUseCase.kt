package io.github.swiftstagrime.termuxrunner.domain.usecase

import io.github.swiftstagrime.termuxrunner.data.automation.AutomationNotificationHelper
import io.github.swiftstagrime.termuxrunner.data.local.dao.AutomationDao
import io.github.swiftstagrime.termuxrunner.domain.model.AutomationLog
import io.github.swiftstagrime.termuxrunner.domain.repository.AutomationLogRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProcessTermuxResultUseCase @Inject constructor(
    private val automationDao: AutomationDao,
    private val logRepository: AutomationLogRepository,
    private val notificationHelper: AutomationNotificationHelper,
) {
    suspend fun execute(
        automationId: Int,
        scriptId: Int,
        scriptName: String,
        exitCode: Int,
        internalError: String?,
    ) {
        val timestamp = System.currentTimeMillis()

        if (automationId != -1) {
            automationDao.updateLastResult(automationId, exitCode, timestamp)

            logRepository.insertLog(
                AutomationLog(
                    automationId = automationId,
                    timestamp = timestamp,
                    exitCode = exitCode,
                    message = internalError
                )
            )
        }

        notificationHelper.showResultNotification(
            scriptId = scriptId,
            name = scriptName,
            exitCode = exitCode,
            internalError = internalError
        )
    }
}