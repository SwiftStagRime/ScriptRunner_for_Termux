package io.github.swiftstagrime.termuxrunner

import io.github.swiftstagrime.termuxrunner.data.automation.AutomationScheduler
import io.github.swiftstagrime.termuxrunner.data.local.dao.AutomationDao
import io.github.swiftstagrime.termuxrunner.data.local.dao.ScriptDao
import io.github.swiftstagrime.termuxrunner.data.local.entity.AutomationEntity
import io.github.swiftstagrime.termuxrunner.data.receiver.DeviceBootReceiver
import io.github.swiftstagrime.termuxrunner.domain.model.AutomationType
import io.github.swiftstagrime.termuxrunner.domain.usecase.RunScriptUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(application = TestApplication::class)
@RunWith(RobolectricTestRunner::class)
class DeviceBootReceiverTest {
    private val mockDao = mockk<AutomationDao>(relaxed = true)
    private val mockScheduler = mockk<AutomationScheduler>(relaxed = true)
    private val mockScriptDao = mockk<ScriptDao>(relaxed = true)
    private val mockRunUseCase = mockk<RunScriptUseCase>(relaxed = true)

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `on BOOT_COMPLETED schedules all enabled automations`() =
        runTest {
            val receiver =
                DeviceBootReceiver().apply {
                    automationDao = mockDao
                    scheduler = mockScheduler
                    scriptDao = mockScriptDao
                    runScriptUseCase = mockRunUseCase
                }

            val enabledList =
                listOf(
                    mockk<AutomationEntity>(relaxed = true) {
                        every { type } returns AutomationType.PERIODIC
                    },
                )
            coEvery { mockDao.getEnabledAutomations() } returns enabledList

            receiver.handleBoot()

            eventually {
                verify(exactly = 1) { mockScheduler.schedule(any()) }
            }
        }

    private suspend fun eventually(block: suspend () -> Unit) {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < 2000) {
            try {
                block()
                return
            } catch (e: Throwable) {
                delay(100)
            }
        }
        block()
    }
}
