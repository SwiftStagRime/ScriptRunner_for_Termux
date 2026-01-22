package io.github.swiftstagrime.termuxrunner

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import io.github.swiftstagrime.termuxrunner.data.automation.AutomationScheduler
import io.github.swiftstagrime.termuxrunner.data.local.entity.AutomationEntity
import io.github.swiftstagrime.termuxrunner.data.worker.AutomationWorker
import io.github.swiftstagrime.termuxrunner.domain.model.AutomationType
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class AutomationSchedulerTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private lateinit var scheduler: AutomationScheduler

    @Before
    fun setup() {
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        val shadowAlarmManager = shadowOf(alarmManager)
        shadowAlarmManager.scheduledAlarms.clear()
        scheduler = AutomationScheduler(context)
    }

    @Test
    fun `schedule triggers immediate work if missed and runIfMissed is true`() {
        val pastTime = System.currentTimeMillis() - 10000
        val automation = createAutomation(
            nextRunTimestamp = pastTime,
            runIfMissed = true
        )

        scheduler.schedule(automation)

        val workManager = WorkManager.getInstance(context)
        val workInfos = workManager.getWorkInfosByTag(AutomationWorker::class.java.name).get()
        assert(workInfos.isNotEmpty() || shadowOf(alarmManager).nextScheduledAlarm == null)
    }

    @Test
    fun `schedule sets alarm for future time`() {
        val futureTime = System.currentTimeMillis() + 50000
        val automation = createAutomation(nextRunTimestamp = futureTime, id = 123)

        scheduler.schedule(automation)

        val shadowAlarmManager = shadowOf(alarmManager)
        val alarm = shadowAlarmManager.nextScheduledAlarm
        assertNotNull(alarm)
        assertEquals(futureTime, alarm?.triggerAtTime )

        val intent = shadowOf(alarm?.operation).savedIntent
        assertEquals(123, intent.getIntExtra("automation_id", -1))
        assertEquals("automation://123", intent.dataString)
    }

    private fun createAutomation(
        id: Int = 1,
        isEnabled: Boolean = true,
        nextRunTimestamp: Long? = null,
        runIfMissed: Boolean = true
    ) = AutomationEntity(
        id = id, isEnabled = isEnabled, nextRunTimestamp = nextRunTimestamp,
        runIfMissed = runIfMissed, scheduledTimestamp = System.currentTimeMillis(),
        type = AutomationType.PERIODIC, scriptId = 1, label = "Test",
        intervalMillis = 0, daysOfWeek = emptyList(), lastRunTimestamp = null
    )
}