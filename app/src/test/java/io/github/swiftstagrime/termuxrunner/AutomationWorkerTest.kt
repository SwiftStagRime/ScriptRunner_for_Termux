package io.github.swiftstagrime.termuxrunner

import android.app.AlarmManager
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import androidx.work.workDataOf
import io.github.swiftstagrime.termuxrunner.data.automation.AutomationScheduler
import io.github.swiftstagrime.termuxrunner.data.local.AppDatabase
import io.github.swiftstagrime.termuxrunner.data.local.entity.AutomationEntity
import io.github.swiftstagrime.termuxrunner.data.local.entity.ScriptEntity
import io.github.swiftstagrime.termuxrunner.data.receiver.OverdueAutomationWorker
import io.github.swiftstagrime.termuxrunner.data.repository.TermuxBackgroundRestrictionException
import io.github.swiftstagrime.termuxrunner.data.worker.AutomationWorker
import io.github.swiftstagrime.termuxrunner.domain.model.AutomationType
import io.github.swiftstagrime.termuxrunner.domain.usecase.RunScriptUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@Config(application = TestApplication::class, sdk = [34])
@RunWith(RobolectricTestRunner::class)
class AutomationWorkerTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private lateinit var database: AppDatabase
    private lateinit var scheduler: AutomationScheduler
    private val runScriptUseCase = mockk<RunScriptUseCase>(relaxed = true)

    @Before
    fun setup() {
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        shadowOf(alarmManager).scheduledAlarms.clear()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        runBlocking {
            database.scriptDao().insertScript(
                ScriptEntity(
                    id = 1,
                    name = "Test",
                    codePages = listOf("echo 1"),
                    interpreter = "bash",
                    runInBackground = false,
                    openNewSession = false,
                    executionParams = "",
                    envVars = emptyMap(),
                    keepSessionOpen = false,
                    iconPath = null,
                ),
            )
        }
        scheduler = AutomationScheduler(context, database.automationDao())
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `condition unmet keeps catch-up pending and schedules capped fallback alarm`() {
        val missedAt = System.currentTimeMillis() - 10_000
        insertAutomation(
            id = 1,
            type = AutomationType.PERIODIC,
            intervalMillis = 60 * 60 * 1000,
            nextRunTimestamp = missedAt,
            requireWifi = true,
            runIfMissed = true,
        )

        runWorker(1)

        // Not executed while the condition is unmet
        coVerify(exactly = 0) { runScriptUseCase(any(), any(), any(), any(), any()) }

        // Catch-up stays pending: next run is still the missed slot in the past
        val updated = runBlocking { database.automationDao().getAutomationById(1) }
        assertEquals(missedAt, updated?.nextRunTimestamp)

        // Fallback alarm is capped around one hour out (not a few minutes of polling)
        val alarm = shadowOf(alarmManager).nextScheduledAlarm
        assertNotNull(alarm)
        val now = System.currentTimeMillis()
        val triggerAt = alarm?.triggerAtTime
        assertTrue(triggerAt in (now + 50 * 60 * 1000)..(now + 70 * 60 * 1000))
        assertEquals(1, shadowOf(alarm?.operation).savedIntent.getIntExtra("automation_id", -1))

        // No immediate work was enqueued
        val workInfos = WorkManager.getInstance(context).getWorkInfosByTag(AutomationWorker::class.java.name).get()
        assertTrue(workInfos.isEmpty())
    }

    @Test
    fun `fallback alarm is capped even for short interval schedules`() {
        val missedAt = System.currentTimeMillis() - 10_000
        insertAutomation(
            id = 1,
            type = AutomationType.PERIODIC,
            intervalMillis = 60 * 1000,
            nextRunTimestamp = missedAt,
            requireWifi = true,
            runIfMissed = true,
        )

        runWorker(1)

        // Without the cap this would re-check in ~1 minute: the alarm must be ~1 hour out
        val alarm = shadowOf(alarmManager).nextScheduledAlarm
        assertNotNull(alarm)
        val now = System.currentTimeMillis()
        val triggerAt = alarm?.triggerAtTime
        assertTrue(triggerAt in (now + 50 * 60 * 1000)..(now + 70 * 60 * 1000))
    }

    @Test
    fun `condition unmet skips slot and resumes at next regular slot when runIfMissed is false`() {
        val missedAt = System.currentTimeMillis() - 10_000
        insertAutomation(
            id = 1,
            type = AutomationType.PERIODIC,
            intervalMillis = 60 * 1000,
            nextRunTimestamp = missedAt,
            requireWifi = true,
            runIfMissed = false,
        )

        runWorker(1)

        coVerify(exactly = 0) { runScriptUseCase(any(), any(), any(), any(), any()) }

        val updated = runBlocking { database.automationDao().getAutomationById(1) }
        val next = updated?.nextRunTimestamp
        assertNotNull(next)
        // The missed slot is skipped: schedule resumes at the next future slot
        assertTrue(next in (System.currentTimeMillis() + 10_000)..(System.currentTimeMillis() + 120 * 1000))
        assertEquals(next, shadowOf(alarmManager).nextScheduledAlarm?.triggerAtTime)
    }

    @Test
    fun `conditions met runs script once and advances the schedule`() {
        val missedAt = System.currentTimeMillis() - 10_000
        insertAutomation(
            id = 1,
            type = AutomationType.PERIODIC,
            intervalMillis = 60 * 60 * 1000,
            nextRunTimestamp = missedAt,
            requireWifi = false,
            runIfMissed = true,
        )

        runWorker(1)

        coVerify(exactly = 1) { runScriptUseCase(any(), any(), any(), any(), any()) }

        val updated = runBlocking { database.automationDao().getAutomationById(1) }
        assertNotNull(updated?.lastRunTimestamp)
        val next = updated?.nextRunTimestamp
        assertNotNull(next)
        // All missed slots are skipped: first slot after now
        val now = System.currentTimeMillis()
        assertTrue(next in (now + 50 * 60 * 1000)..(now + 70 * 60 * 1000))
        assertEquals(next, shadowOf(alarmManager).nextScheduledAlarm?.triggerAtTime)
    }

    @Test
    fun `overdue worker re-triggers only pending catch-ups`() {
        val past = System.currentTimeMillis() - 10 * 60 * 1000
        val future = System.currentTimeMillis() + 10 * 60 * 1000
        // A: blocked catch-up still pending (next run in the past, runIfMissed on)
        insertAutomation(id = 1, nextRunTimestamp = past, requireWifi = true, runIfMissed = true)
        // B: already ran / next run in the future — must not be re-triggered
        insertAutomation(id = 2, nextRunTimestamp = future, runIfMissed = true)
        // C: overdue but runIfMissed disabled — must not be re-triggered
        insertAutomation(id = 3, nextRunTimestamp = past, requireWifi = true, runIfMissed = false)

        val mockScheduler = mockk<AutomationScheduler>(relaxed = true)
        val factory =
            object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters,
                ): ListenableWorker =
                    OverdueAutomationWorker(
                        appContext,
                        workerParameters,
                        database.automationDao(),
                        mockScheduler,
                    )
            }
        val worker =
            TestListenableWorkerBuilder<OverdueAutomationWorker>(context)
                .setWorkerFactory(factory)
                .build()
        worker.startWork().get()

        // Exactly-once catch-up: only the pending one is re-triggered
        verify(exactly = 1) { mockScheduler.triggerImmediate(1) }
        verify(exactly = 0) { mockScheduler.triggerImmediate(2) }
        verify(exactly = 0) { mockScheduler.triggerImmediate(3) }
    }

    @Test
    fun `dispatch failure logs error keeps catch-up pending and re-arms fallback alarm`() {
        val missedAt = System.currentTimeMillis() - 10_000
        insertAutomation(
            id = 1,
            type = AutomationType.PERIODIC,
            intervalMillis = 60 * 60 * 1000,
            nextRunTimestamp = missedAt,
            runIfMissed = true,
        )
        coEvery { runScriptUseCase(any(), any(), any(), any(), any()) } throws
            TermuxBackgroundRestrictionException()

        runWorker(1)

        coVerify(exactly = 1) { runScriptUseCase(any(), any(), any(), any(), any()) }

        val updated = runBlocking { database.automationDao().getAutomationById(1) }
        // The run never happened: no misleading "last run" record
        assertNull(updated?.lastRunTimestamp)
        // Catch-up stays pending: next run is still the missed slot in the past
        assertEquals(missedAt, updated?.nextRunTimestamp)

        // Fallback re-check alarm is armed: the schedule is not stranded
        val alarm = shadowOf(alarmManager).nextScheduledAlarm
        assertNotNull(alarm)
        val now = System.currentTimeMillis()
        val triggerAt = alarm?.triggerAtTime
        assertTrue(triggerAt in (now + 50 * 60 * 1000)..(now + 70 * 60 * 1000))

        // The failure is recorded in the automation log
        val logs = runBlocking { database.automationLogDao().getLogsForAutomation(1).first() }
        assertEquals(1, logs.size)
        assertEquals(AutomationWorker.DISPATCH_FAILED_EXIT_CODE, logs.first().exitCode)
        assertNotNull(logs.first().message)
    }

    @Test
    fun `dispatch failure skips missed slot when runIfMissed is false`() {
        val missedAt = System.currentTimeMillis() - 10_000
        insertAutomation(
            id = 1,
            type = AutomationType.PERIODIC,
            intervalMillis = 60 * 1000,
            nextRunTimestamp = missedAt,
            runIfMissed = false,
        )
        coEvery { runScriptUseCase(any(), any(), any(), any(), any()) } throws
            TermuxBackgroundRestrictionException()

        runWorker(1)

        val updated = runBlocking { database.automationDao().getAutomationById(1) }
        assertNull(updated?.lastRunTimestamp)
        // The missed slot is skipped: schedule resumes at the next future slot
        val next = updated?.nextRunTimestamp
        assertNotNull(next)
        assertTrue(next!! in (System.currentTimeMillis() + 10_000)..(System.currentTimeMillis() + 120 * 1000))
        assertEquals(next, shadowOf(alarmManager).nextScheduledAlarm?.triggerAtTime)
    }

    @Test
    fun `event-based automation stays enabled after a successful run`() {
        insertAutomation(
            id = 1,
            type = AutomationType.SCREEN_ON,
            nextRunTimestamp = null,
        )

        runWorker(1)

        coVerify(exactly = 1) { runScriptUseCase(any(), any(), any(), any(), any()) }

        val updated = runBlocking { database.automationDao().getAutomationById(1) }
        // Event automations have no next scheduled slot and must not be retired
        assertTrue(updated?.isEnabled == true)
        assertNotNull(updated?.lastRunTimestamp)
        assertNull(updated?.nextRunTimestamp)
    }

    private fun runWorker(automationId: Int) {
        val factory =
            object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters,
                ): ListenableWorker =
                    AutomationWorker(
                        appContext,
                        workerParameters,
                        database.automationDao(),
                        database.automationLogDao(),
                        database.scriptDao(),
                        runScriptUseCase,
                        scheduler,
                    )
            }
        val worker =
            TestListenableWorkerBuilder<AutomationWorker>(context)
                .setInputData(workDataOf("automation_id" to automationId))
                .setWorkerFactory(factory)
                .build()
        // Success is implied by the side effects asserted in each test
        worker.startWork().get()
    }

    private fun insertAutomation(
        id: Int,
        type: AutomationType = AutomationType.PERIODIC,
        intervalMillis: Long = 0,
        nextRunTimestamp: Long?,
        requireWifi: Boolean = false,
        runIfMissed: Boolean = true,
    ) = runBlocking {
        database.automationDao().insertAutomation(
            AutomationEntity(
                id = id,
                scriptId = 1,
                label = "Test $id",
                type = type,
                scheduledTimestamp = System.currentTimeMillis() - 24 * 60 * 60 * 1000,
                intervalMillis = intervalMillis,
                daysOfWeek = emptyList(),
                isEnabled = true,
                nextRunTimestamp = nextRunTimestamp,
                runIfMissed = runIfMissed,
                requireWifi = requireWifi,
            ),
        )
    }
}
