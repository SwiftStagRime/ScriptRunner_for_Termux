package io.github.swiftstagrime.termuxrunner

import android.content.Context
import io.github.swiftstagrime.termuxrunner.data.automation.AutomationScheduler
import io.github.swiftstagrime.termuxrunner.data.local.dao.AutomationDao
import io.github.swiftstagrime.termuxrunner.data.local.dao.AutomationLogDao
import io.github.swiftstagrime.termuxrunner.data.local.dao.ScriptDao
import io.github.swiftstagrime.termuxrunner.data.local.entity.AutomationEntity
import io.github.swiftstagrime.termuxrunner.data.receiver.DeviceBootReceiver
import io.github.swiftstagrime.termuxrunner.data.repository.TermuxForceStoppedException
import io.github.swiftstagrime.termuxrunner.data.worker.AutomationWorker
import io.github.swiftstagrime.termuxrunner.domain.model.AutomationType
import io.github.swiftstagrime.termuxrunner.domain.repository.UserPreferencesRepository
import io.github.swiftstagrime.termuxrunner.domain.usecase.RunScriptUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.time.Duration.Companion.milliseconds

@Config(application = TestApplication::class, sdk = [34])
@RunWith(RobolectricTestRunner::class)
class DeviceBootReceiverTest {
    private val mockDao = mockk<AutomationDao>(relaxed = true)
    private val mockLogDao = mockk<AutomationLogDao>(relaxed = true)
    private val mockScheduler = mockk<AutomationScheduler>(relaxed = true)
    private val mockScriptDao = mockk<ScriptDao>(relaxed = true)
    private val mockRunUseCase = mockk<RunScriptUseCase>(relaxed = true)
    private val mockPreferencesRepo = mockk<UserPreferencesRepository>(relaxed = true)

    private val testContext: Context
        get() = RuntimeEnvironment.getApplication().applicationContext

    private fun buildReceiver(): DeviceBootReceiver =
        DeviceBootReceiver().apply {
            automationDao = mockDao
            automationLogDao = mockLogDao
            scheduler = mockScheduler
            scriptDao = mockScriptDao
            runScriptUseCase = mockRunUseCase
            userPreferencesRepository = mockPreferencesRepo
        }

    private fun bootAutomation(
        id: Int,
        scriptId: Int = id + 100,
    ): AutomationEntity =
        mockk(relaxed = true) {
            every { this@mockk.id } returns id
            every { this@mockk.scriptId } returns scriptId
            every { label } returns "Boot task $id"
            every { type } returns AutomationType.BOOT
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `on BOOT_COMPLETED schedules all enabled automations`() =
        runTest {
            val receiver = buildReceiver()

            every { mockPreferencesRepo.isWebhookEnabled } returns flowOf(false)

            val enabledList =
                listOf(
                    mockk<AutomationEntity>(relaxed = true) {
                        every { type } returns AutomationType.PERIODIC
                    },
                )
            coEvery { mockDao.getEnabledAutomations() } returns enabledList

            receiver.handleBoot(pendingResult = null, context = testContext)

            eventually {
                coVerify(exactly = 1) { mockScheduler.schedule(any()) }
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `BOOT dispatch failure is logged and records no run`() =
        runTest {
            val receiver = buildReceiver()
            every { mockPreferencesRepo.isWebhookEnabled } returns flowOf(false)
            coEvery { mockDao.getEnabledAutomations() } returns listOf(bootAutomation(id = 11))
            coEvery { mockScriptDao.getScriptById(any()) } returns mockk(relaxed = true)
            coEvery { mockRunUseCase(any(), any(), any(), any(), any()) } throws
                TermuxForceStoppedException()

            receiver.handleBoot(pendingResult = null, context = testContext)

            eventually {
                coVerify(exactly = 1) { mockLogDao.insertLog(any()) }
            }

            coVerify(exactly = 1) {
                mockLogDao.insertLog(
                    withArg {
                        assertEquals(11, it.automationId)
                        assertEquals(AutomationWorker.DISPATCH_FAILED_EXIT_CODE, it.exitCode)
                        assertEquals(
                            "Could not start script: " +
                                testContext.getString(R.string.error_termux_force_stopped),
                            it.message,
                        )
                    },
                )
            }

            // Handing the command to the system is not a completed run
            coVerify(exactly = 0) { mockDao.updateAutomation(any()) }
            coVerify(exactly = 0) { mockScheduler.schedule(any()) }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `one failing BOOT automation does not stop the others`() =
        runTest {
            val receiver = buildReceiver()
            every { mockPreferencesRepo.isWebhookEnabled } returns flowOf(false)
            coEvery {
                mockDao.getEnabledAutomations()
            } returns
                listOf(
                    bootAutomation(id = 1),
                    bootAutomation(id = 2),
                    mockk<AutomationEntity>(relaxed = true) {
                        every { type } returns AutomationType.PERIODIC
                    },
                )
            coEvery { mockScriptDao.getScriptById(any()) } returns mockk(relaxed = true)

            var dispatches = 0
            coEvery { mockRunUseCase(any(), any(), any(), any(), any()) } coAnswers {
                dispatches++
                if (dispatches == 1) throw TermuxForceStoppedException()
            }

            receiver.handleBoot(pendingResult = null, context = testContext)

            eventually {
                coVerify(exactly = 1) { mockScheduler.schedule(any()) }
            }

            coVerify(exactly = 2) { mockRunUseCase(any(), any(), any(), any(), any()) }
            coVerify(exactly = 1) { mockLogDao.insertLog(any()) }
        }

    private suspend fun eventually(block: suspend () -> Unit) {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < 2000) {
            try {
                block()
                return
            } catch (_: Throwable) {
                delay(100.milliseconds)
            }
        }
        block()
    }
}
