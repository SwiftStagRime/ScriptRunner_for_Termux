package io.github.swiftstagrime.termuxrunner.domain.usecase

import io.github.swiftstagrime.termuxrunner.domain.model.AutomationLog
import io.github.swiftstagrime.termuxrunner.domain.repository.AutomationLogRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.AutomationRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptResultNotificator
import io.github.swiftstagrime.termuxrunner.domain.repository.WidgetUpdater
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProcessTermuxResultUseCase
    @Inject
    constructor(
        private val automationRepository: AutomationRepository,
        private val logRepository: AutomationLogRepository,
        private val notificationHelper: ScriptResultNotificator,
        private val widgetManager: WidgetUpdater,
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
                automationRepository.updateLastResult(automationId, exitCode, timestamp)

                logRepository.insertLog(
                    AutomationLog(
                        automationId = automationId,
                        timestamp = timestamp,
                        exitCode = exitCode,
                        message = internalError,
                    ),
                )

                widgetManager.updateLogsWidget()
            }

            notificationHelper.showResultNotification(
                scriptId = scriptId,
                name = scriptName,
                exitCode = exitCode,
                internalError = internalError,
            )
        }
    }
