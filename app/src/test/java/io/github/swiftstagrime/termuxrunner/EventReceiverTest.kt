package io.github.swiftstagrime.termuxrunner

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import io.github.swiftstagrime.termuxrunner.data.receiver.EventAutomationWorker
import io.github.swiftstagrime.termuxrunner.data.receiver.EventReceiver
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(application = TestApplication::class, sdk = [34])
@RunWith(RobolectricTestRunner::class)
class EventReceiverTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var workManager: WorkManager

    @Before
    fun setup() {
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        workManager = WorkManager.getInstance(context)
    }

    private fun send(action: String) {
        EventReceiver().onReceive(context, Intent(action).setPackage(context.packageName))
    }

    @Test
    fun `charging broadcast enqueues overdue catch-up and charging event worker`() {
        send(Intent.ACTION_POWER_CONNECTED)

        assertEquals(
            1,
            workManager.getWorkInfosForUniqueWork("overdue_automation_catch_up").get().size,
        )
        assertEquals(
            1,
            workManager.getWorkInfosForUniqueWork("event_CHARGING_STARTED").get().size,
        )
    }

    @Test
    fun `rapid charging broadcasts coalesce into one pending worker`() {
        repeat(3) { send(Intent.ACTION_POWER_CONNECTED) }

        assertEquals(
            1,
            workManager.getWorkInfosForUniqueWork("event_CHARGING_STARTED").get().size,
        )
        assertEquals(
            1,
            workManager.getWorkInfosForUniqueWork("overdue_automation_catch_up").get().size,
        )
    }

    @Test
    fun `screen on broadcast enqueues a worker per occurrence`() {
        send(Intent.ACTION_SCREEN_ON)
        send(Intent.ACTION_SCREEN_ON)

        val screenOnRuns =
            workManager.getWorkInfosByTag(EventAutomationWorker::class.java.name).get()
        assertEquals(2, screenOnRuns.size)
    }

    @Test
    fun `unrelated broadcast enqueues nothing`() {
        send(Intent.ACTION_BATTERY_LOW)

        assertEquals(
            0,
            workManager.getWorkInfosByTag(EventAutomationWorker::class.java.name).get().size,
        )
    }
}
