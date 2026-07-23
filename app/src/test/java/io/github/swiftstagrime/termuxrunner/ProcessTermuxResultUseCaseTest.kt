package io.github.swiftstagrime.termuxrunner

import io.github.swiftstagrime.termuxrunner.domain.model.AutomationLog
import io.github.swiftstagrime.termuxrunner.domain.model.ExecutionSource
import io.github.swiftstagrime.termuxrunner.domain.repository.AutomationLogRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.AutomationRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptExecutionRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptResultNotificator
import io.github.swiftstagrime.termuxrunner.domain.repository.WidgetUpdater
import io.github.swiftstagrime.termuxrunner.domain.usecase.ExecuteChainStepUseCase
import io.github.swiftstagrime.termuxrunner.domain.usecase.ProcessTermuxResultUseCase
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ProcessTermuxResultUseCaseTest {
    private val automationRepo = mockk<AutomationRepository>(relaxed = true)
    private val logRepo = mockk<AutomationLogRepository>(relaxed = true)
    private val scriptExecRepo = mockk<ScriptExecutionRepository>(relaxed = true)
    private val notifier = mockk<ScriptResultNotificator>(relaxed = true)
    private val widgetUpdater = mockk<WidgetUpdater>(relaxed = true)
    private val chainStepUseCase = mockk<ExecuteChainStepUseCase>(relaxed = true)
    private val scriptRepo = mockk<ScriptRepository>(relaxed = true)
    private val useCase =
        ProcessTermuxResultUseCase(
            automationRepo,
            logRepo,
            scriptExecRepo,
            notifier,
            widgetUpdater,
            chainStepUseCase,
            scriptRepo,
        )

    @Test
    fun `execute updates db and shows notification`() =
        runTest {
            useCase.execute(
                automationId = 1,
                scriptId = 10,
                scriptName = "Test",
                exitCode = 0,
                internalError = null,
            )

            coVerify { automationRepo.updateLastResult(1, 0, any()) }
            coVerify { logRepo.insertLog(match { it.automationId == 1 && it.exitCode == 0 }) }
            verify {
                notifier.showResultNotification(
                    scriptId = 10,
                    name = "Test",
                    exitCode = 0,
                    internalError = null,
                )
            }
            coVerify { widgetUpdater.updateLogsWidget() }
        }

    @Test
    fun `execute with id -1 skip database but shows notification`() =
        runTest {
            useCase.execute(-1, 10, "Test", 0, null)

            coVerify(exactly = 0) { automationRepo.updateLastResult(any(), any(), any()) }
            coVerify(exactly = 0) { logRepo.insertLog(any()) }
            coVerify(exactly = 0) { widgetUpdater.updateLogsWidget() }
            verify {
                notifier.showResultNotification(
                    scriptId = 10,
                    name = "Test",
                    exitCode = 0,
                    internalError = null,
                )
            }
        }

    @Test
    fun `execute with automation logs the error message`() =
        runTest {
            useCase.execute(
                automationId = 2,
                scriptId = 20,
                scriptName = "FailScript",
                exitCode = 1,
                internalError = "Something went wrong",
            )

            coVerify { automationRepo.updateLastResult(2, 1, any()) }
            coVerify {
                logRepo.insertLog(
                    match { log: AutomationLog ->
                        log.automationId == 2 && log.exitCode == 1 && log.message == "Something went wrong"
                    },
                )
            }
            verify {
                notifier.showResultNotification(
                    scriptId = 20,
                    name = "FailScript",
                    exitCode = 1,
                    internalError = "Something went wrong",
                )
            }
        }

    @Test
    fun `execute records script execution with AUTOMATION source`() =
        runTest {
            useCase.execute(
                automationId = 1,
                scriptId = 10,
                scriptName = "AutoScript",
                exitCode = 0,
                internalError = null,
            )

            coVerify {
                scriptExecRepo.insert(
                    match { exec ->
                        exec.scriptId == 10 &&
                            exec.scriptName == "AutoScript" &&
                            exec.exitCode == 0 &&
                            exec.source == ExecutionSource.AUTOMATION &&
                            exec.errorMessage == null
                    },
                )
            }
        }

    @Test
    fun `execute records script execution with MANUAL source when automationId is -1`() =
        runTest {
            useCase.execute(
                automationId = -1,
                scriptId = 10,
                scriptName = "ManualScript",
                exitCode = 0,
                internalError = null,
            )

            coVerify {
                scriptExecRepo.insert(
                    match { exec ->
                        exec.scriptId == 10 &&
                            exec.source == ExecutionSource.MANUAL
                    },
                )
            }
        }

    @Test
    fun `execute records error message in script execution`() =
        runTest {
            useCase.execute(
                automationId = -1,
                scriptId = 10,
                scriptName = "FailingScript",
                exitCode = 1,
                internalError = "Permission denied",
            )

            coVerify {
                scriptExecRepo.insert(
                    match { exec ->
                        exec.errorMessage == "Permission denied" &&
                            exec.exitCode == 1
                    },
                )
            }
        }

    @Test
    fun `execute with recorded start time includes duration`() =
        runTest {
            useCase.recordStartTime(10)
            Thread.sleep(50)

            useCase.execute(
                automationId = -1,
                scriptId = 10,
                scriptName = "TimedScript",
                exitCode = 0,
                internalError = null,
            )

            coVerify {
                scriptExecRepo.insert(
                    match { exec ->
                        val d = exec.durationMs ?: return@match false
                        d >= 50L
                    },
                )
            }
        }

    @Test
    fun `execute without recorded start time has null duration`() =
        runTest {
            useCase.execute(
                automationId = -1,
                scriptId = 99,
                scriptName = "NoStartTime",
                exitCode = 0,
                internalError = null,
            )

            coVerify {
                scriptExecRepo.insert(
                    match { exec ->
                        exec.durationMs == null
                    },
                )
            }
        }

    @Test
    fun `concurrent executions track all start times independently`() =
        runTest {
            useCase.recordStartTime(10)
            Thread.sleep(20)
            useCase.recordStartTime(10)

            useCase.execute(-1, 10, "FirstRun", 0, null)
            useCase.execute(-1, 10, "SecondRun", 0, null)

            coVerify(exactly = 2) { scriptExecRepo.insert(any()) }
        }
}
