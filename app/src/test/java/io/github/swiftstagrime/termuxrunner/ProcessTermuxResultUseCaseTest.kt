package io.github.swiftstagrime.termuxrunner

import io.github.swiftstagrime.termuxrunner.domain.model.AutomationLog
import io.github.swiftstagrime.termuxrunner.domain.repository.AutomationLogRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.AutomationRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptResultNotificator
import io.github.swiftstagrime.termuxrunner.domain.repository.WidgetUpdater
import io.github.swiftstagrime.termuxrunner.domain.usecase.ProcessTermuxResultUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ProcessTermuxResultUseCaseTest {
    private val automationRepo = mockk<AutomationRepository>(relaxed = true)
    private val logRepo = mockk<AutomationLogRepository>(relaxed = true)
    private val notifier = mockk<ScriptResultNotificator>(relaxed = true)
    private val widgetUpdater = mockk<WidgetUpdater>(relaxed = true)
    private val useCase = ProcessTermuxResultUseCase(automationRepo, logRepo, notifier, widgetUpdater)

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
            verify { notifier.showResultNotification(scriptId = 10, name = "Test", exitCode = 0, internalError = null) }
            coVerify { widgetUpdater.updateLogsWidget() }
        }

    @Test
    fun `execute with id -1 skip database but shows notification`() =
        runTest {
            useCase.execute(-1, 10, "Test", 0, null)

            coVerify(exactly = 0) { automationRepo.updateLastResult(any(), any(), any()) }
            coVerify(exactly = 0) { logRepo.insertLog(any()) }
            coVerify(exactly = 0) { widgetUpdater.updateLogsWidget() }
            verify { notifier.showResultNotification(scriptId = 10, name = "Test", exitCode = 0, internalError = null) }
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
                logRepo.insertLog(match { log: AutomationLog ->
                    log.automationId == 2 && log.exitCode == 1 && log.message == "Something went wrong"
                })
            }
            verify { notifier.showResultNotification(scriptId = 20, name = "FailScript", exitCode = 1, internalError = "Something went wrong") }
        }
}
