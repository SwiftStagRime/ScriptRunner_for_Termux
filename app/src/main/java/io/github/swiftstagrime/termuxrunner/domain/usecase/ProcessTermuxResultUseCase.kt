package io.github.swiftstagrime.termuxrunner.domain.usecase

import io.github.swiftstagrime.termuxrunner.domain.model.AutomationLog
import io.github.swiftstagrime.termuxrunner.domain.model.ExecutionSource
import io.github.swiftstagrime.termuxrunner.domain.model.ScriptExecution
import io.github.swiftstagrime.termuxrunner.domain.repository.AutomationLogRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.AutomationRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptExecutionRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptResultNotificator
import io.github.swiftstagrime.termuxrunner.domain.repository.WidgetUpdater
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProcessTermuxResultUseCase
    @Inject
    constructor(
        private val automationRepository: AutomationRepository,
        private val logRepository: AutomationLogRepository,
        private val scriptExecutionRepository: ScriptExecutionRepository,
        private val notificationHelper: ScriptResultNotificator,
        private val widgetManager: WidgetUpdater,
        private val executeChainStepUseCase: ExecuteChainStepUseCase,
        private val scriptRepository: ScriptRepository,
    ) {
        private data class ExecutionToken(
            val startTime: Long,
        )

        private val executionStartTimes = ConcurrentHashMap<Int, MutableList<ExecutionToken>>()
        private val tokenCounter = AtomicLong(0)

        fun recordStartTime(scriptId: Int): Long {
            val token = tokenCounter.incrementAndGet()
            val tokens = executionStartTimes.computeIfAbsent(scriptId) { mutableListOf() }
            synchronized(tokens) { tokens.add(ExecutionToken(System.currentTimeMillis())) }
            return token
        }

        suspend fun execute(
            automationId: Int,
            scriptId: Int,
            scriptName: String,
            exitCode: Int,
            internalError: String?,
        ) {
            val timestamp = System.currentTimeMillis()
            val tokens = executionStartTimes[scriptId]
            val startTime =
                if (tokens != null && tokens.isNotEmpty()) {
                    synchronized(tokens) {
                        if (tokens.isNotEmpty()) tokens.removeAt(0).startTime else null
                    }
                } else {
                    null
                }
            val durationMs = startTime?.let { timestamp - it }

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

            scriptExecutionRepository.insert(
                ScriptExecution(
                    scriptId = scriptId,
                    scriptName = scriptName,
                    timestamp = timestamp,
                    exitCode = exitCode,
                    durationMs = durationMs,
                    source = if (automationId != -1) ExecutionSource.AUTOMATION else ExecutionSource.MANUAL,
                    errorMessage = internalError,
                ),
            )

            val scriptActions =
                scriptRepository.getScriptById(scriptId)?.notificationActions ?: emptyList()

            notificationHelper.showResultNotification(
                scriptId = scriptId,
                name = scriptName,
                exitCode = exitCode,
                internalError = internalError,
                actions = scriptActions,
            )

            // Trigger chain steps if automation is part of a chain
            if (automationId != -1) {
                executeChainStepUseCase(automationId, exitCode)
            }
        }
    }
