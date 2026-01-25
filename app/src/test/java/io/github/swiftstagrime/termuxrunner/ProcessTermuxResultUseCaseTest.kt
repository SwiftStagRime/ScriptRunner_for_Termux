package io.github.swiftstagrime.termuxrunner

import io.github.swiftstagrime.termuxrunner.data.automation.AutomationNotificationHelper
import io.github.swiftstagrime.termuxrunner.data.local.dao.AutomationDao
import io.github.swiftstagrime.termuxrunner.domain.repository.AutomationLogRepository
import io.github.swiftstagrime.termuxrunner.domain.usecase.ProcessTermuxResultUseCase
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ProcessTermuxResultUseCaseTest {
    private val dao = mockk<AutomationDao>(relaxed = true)
    private val repo = mockk<AutomationLogRepository>(relaxed = true)
    private val notifier = mockk<AutomationNotificationHelper>(relaxed = true)

    private val useCase = ProcessTermuxResultUseCase(dao, repo, notifier)

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

            coVerify { dao.updateLastResult(1, 0, any()) }
            coVerify { repo.insertLog(match { it.automationId == 1 && it.exitCode == 0 }) }
            verify { notifier.showResultNotification(10, "Test", 0, null) }
        }

    @Test
    fun `execute with id -1 skip database but shows notification`() =
        runTest {
            useCase.execute(-1, 10, "Test", 0, null)

            coVerify(exactly = 0) { dao.updateLastResult(any(), any(), any()) }
            verify { notifier.showResultNotification(10, "Test", 0, null) }
        }
}
