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
import io.github.swiftstagrime.termuxrunner.data.automation.AutomationScheduler
import io.github.swiftstagrime.termuxrunner.data.local.AppDatabase
import io.github.swiftstagrime.termuxrunner.data.local.entity.AutomationEntity
import io.github.swiftstagrime.termuxrunner.data.local.entity.ScriptEntity
import io.github.swiftstagrime.termuxrunner.data.worker.AutomationSweepWorker
import io.github.swiftstagrime.termuxrunner.domain.model.AutomationType
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
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
class AutomationSweepWorkerTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private lateinit var database: AppDatabase
    private lateinit var scheduler: AutomationScheduler

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
    fun `sweep re-triggers overdue catch-ups without touching their entities`() {
        val past = System.currentTimeMillis() - 10 * 60 * 1000
        insertAutomation(
            id = 1,
            nextRunTimestamp = past,
            intervalMillis = 30 * 60 * 1000,
            runIfMissed = true,
        )

        runSweep()

        val workManager = WorkManager.getInstance(context)
        assertTrue(workManager.getWorkInfosForUniqueWork("automation_worker_1").get().isNotEmpty())

        // The catch-up stays pending in the entity: the re-triggered worker
        // re-checks conditions and fires once when they are met
        val updated = runBlocking { database.automationDao().getAutomationById(1) }
        assertEquals(past, updated?.nextRunTimestamp)
    }

    @Test
    fun `sweep skips missed slot and re-arms alarm when runIfMissed is false`() {
        val past = System.currentTimeMillis() - 10 * 60 * 1000
        insertAutomation(
            id = 1,
            nextRunTimestamp = past,
            intervalMillis = 30 * 60 * 1000,
            runIfMissed = false,
        )

        runSweep()

        val workManager = WorkManager.getInstance(context)
        assertTrue(workManager.getWorkInfosForUniqueWork("automation_worker_1").get().isEmpty())

        val updated = runBlocking { database.automationDao().getAutomationById(1) }
        val next = updated?.nextRunTimestamp
        assertNotNull(next)
        assertTrue(next!! > System.currentTimeMillis())
        // Alarm re-armed at the advanced slot: the schedule is not stranded
        assertEquals(next, shadowOf(alarmManager).nextScheduledAlarm?.triggerAtTime)
    }

    @Test
    fun `sweep ignores non-overdue and non-scheduled automations`() {
        val future = System.currentTimeMillis() + 10 * 60 * 1000
        insertAutomation(
            id = 1,
            nextRunTimestamp = future,
            intervalMillis = 30 * 60 * 1000,
            runIfMissed = true,
        )
        insertAutomation(
            id = 2,
            type = AutomationType.SCREEN_ON,
            nextRunTimestamp = null,
            runIfMissed = true,
        )

        runSweep()

        val workManager = WorkManager.getInstance(context)
        assertTrue(workManager.getWorkInfosForUniqueWork("automation_worker_1").get().isEmpty())
        assertTrue(workManager.getWorkInfosForUniqueWork("automation_worker_2").get().isEmpty())
        assertNull(shadowOf(alarmManager).nextScheduledAlarm)

        val futureRun = runBlocking { database.automationDao().getAutomationById(1) }
        assertEquals(future, futureRun?.nextRunTimestamp)
    }

    private fun runSweep() {
        val factory =
            object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters,
                ): ListenableWorker =
                    AutomationSweepWorker(
                        appContext,
                        workerParameters,
                        database.automationDao(),
                        scheduler,
                    )
            }
        val worker =
            TestListenableWorkerBuilder<AutomationSweepWorker>(context)
                .setWorkerFactory(factory)
                .build()
        worker.startWork().get()
    }

    private fun insertAutomation(
        id: Int,
        type: AutomationType = AutomationType.PERIODIC,
        intervalMillis: Long = 0,
        nextRunTimestamp: Long?,
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
            ),
        )
    }
}
