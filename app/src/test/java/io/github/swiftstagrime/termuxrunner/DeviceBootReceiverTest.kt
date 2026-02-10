package io.github.swiftstagrime.termuxrunner

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import io.github.swiftstagrime.termuxrunner.data.automation.AutomationScheduler
import io.github.swiftstagrime.termuxrunner.data.local.dao.AutomationDao
import io.github.swiftstagrime.termuxrunner.data.local.entity.AutomationEntity
import io.github.swiftstagrime.termuxrunner.data.receiver.DeviceBootReceiver
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(application = TestApplication::class)
@RunWith(RobolectricTestRunner::class)
class DeviceBootReceiverTest {
    private val dao = mockk<AutomationDao>(relaxed = true)
    private val scheduler = mockk<AutomationScheduler>(relaxed = true)
    private val receiver =
        DeviceBootReceiver().apply {
            this.automationDao = dao
            this.scheduler = scheduler
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `on BOOT_COMPLETED schedules all enabled automations`() =
        runTest {
            val enabledList = listOf(mockk<AutomationEntity>(), mockk<AutomationEntity>())
            coEvery { dao.getEnabledAutomations() } returns enabledList

            val intent = Intent(Intent.ACTION_BOOT_COMPLETED)
            receiver.onReceive(ApplicationProvider.getApplicationContext(), intent)

            advanceUntilIdle()

            verify(exactly = 2) { scheduler.schedule(any()) }
        }
}
