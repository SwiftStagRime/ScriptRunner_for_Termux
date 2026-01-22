package io.github.swiftstagrime.termuxrunner

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.swiftstagrime.termuxrunner.data.local.AppDatabase
import io.github.swiftstagrime.termuxrunner.data.local.dao.AutomationDao
import io.github.swiftstagrime.termuxrunner.data.local.dao.AutomationLogDao
import io.github.swiftstagrime.termuxrunner.data.local.dao.CategoryDao
import io.github.swiftstagrime.termuxrunner.data.local.dao.ScriptDao
import io.github.swiftstagrime.termuxrunner.data.local.entity.AutomationEntity
import io.github.swiftstagrime.termuxrunner.data.local.entity.AutomationLogEntity
import io.github.swiftstagrime.termuxrunner.data.local.entity.CategoryEntity
import io.github.swiftstagrime.termuxrunner.data.local.entity.ScriptEntity
import io.github.swiftstagrime.termuxrunner.domain.model.AutomationType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class DaoTests {

    private lateinit var db: AppDatabase
    private lateinit var scriptDao: ScriptDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var automationDao: AutomationDao
    private lateinit var logDao: AutomationLogDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        scriptDao = db.scriptDao()
        categoryDao = db.categoryDao()
        automationDao = db.automationDao()
        logDao = db.automationLogDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndGetScript() = runTest {
        val script = createScript(name = "Test Script")
        val id = scriptDao.insertScript(script).toInt()

        val fetched = scriptDao.getScriptById(id)
        assertEquals("Test Script", fetched?.name)
    }

    @Test
    fun updateScriptsOrderTransaction() = runTest {
        val id1 = scriptDao.insertScript(createScript(name = "S1")).toInt()
        val id2 = scriptDao.insertScript(createScript(name = "S2")).toInt()

        scriptDao.updateScriptsOrder(listOf(id1 to 10, id2 to 20))

        val scripts = scriptDao.getAllScriptsOneShot()
        assertEquals(10, scripts.find { it.id == id1 }?.orderIndex)
        assertEquals(20, scripts.find { it.id == id2 }?.orderIndex)
    }

    @Test
    fun insertAndObserveCategories() = runTest {
        val categoryId = categoryDao.insertCategory(CategoryEntity(name = "Utils", orderIndex = 1)).toInt()
        val categories = categoryDao.getAllCategories().first()
        assertEquals(1, categories.size)
        assertEquals("Utils", categories[0].name)
        assertEquals(categoryId, categories[0].id)
    }

    @Test
    fun automationFilteringByEnabled() = runTest {
        val scriptId = scriptDao.insertScript(createScript()).toInt()

        automationDao.insertAutomation(createAutomation(scriptId, "Auto 1", true))
        automationDao.insertAutomation(createAutomation(scriptId, "Auto 2", false))

        val enabled = automationDao.getEnabledAutomations()
        assertEquals(1, enabled.size)
        assertEquals("Auto 1", enabled[0].label)
    }

    @Test
    fun updateLastResultUpdatesSpecificFields() = runTest {
        val scriptId = scriptDao.insertScript(createScript()).toInt()
        val autoId = automationDao.insertAutomation(createAutomation(scriptId)).toInt()

        val timestamp = 123456789L
        automationDao.updateLastResult(autoId, 0, timestamp)

        val updated = automationDao.getAutomationById(autoId)
        assertEquals(0, updated?.lastExitCode)
        assertEquals(timestamp, updated?.lastRunTimestamp)
    }

    @Test
    fun foreignKeyDeleteCascade() = runTest {
        val script = createScript()
        val scriptId = scriptDao.insertScript(script).toInt()
        automationDao.insertAutomation(createAutomation(scriptId))

        scriptDao.deleteScript(script.copy(id = scriptId))

        val automations = automationDao.getAllAutomationsOneShot()
        assertTrue(automations.isEmpty())
    }

    @Test
    fun logCleanupByThreshold() = runTest {
        val scriptId = scriptDao.insertScript(createScript()).toInt()
        val autoId = automationDao.insertAutomation(createAutomation(scriptId)).toInt()

        logDao.insertLog(AutomationLogEntity(automationId = autoId, timestamp = 100, exitCode = 0))
        logDao.insertLog(AutomationLogEntity(automationId = autoId, timestamp = 500, exitCode = 0))

        logDao.deleteOldLogs(300)

        logDao.getLogsForAutomation(autoId).first().let { logs ->
            assertEquals(1, logs.size)
            assertEquals(500L, logs[0].timestamp)
        }
    }

    @Test
    fun logLimitCheck() = runTest {
        val scriptId = scriptDao.insertScript(createScript()).toInt()
        val autoId = automationDao.insertAutomation(createAutomation(scriptId)).toInt()

        repeat(60) { i ->
            logDao.insertLog(
                AutomationLogEntity(
                    automationId = autoId,
                    timestamp = i.toLong(),
                    exitCode = 0
                )
            )
        }

        val logs = logDao.getLogsForAutomation(autoId).first()
        assertEquals(50, logs.size)
        assertTrue(logs[0].timestamp > logs[1].timestamp)
    }

    private fun createScript(name: String = "Test") = ScriptEntity(
        name = name,
        code = "echo hello",
        interpreter = "bash",
        runInBackground = true,
        openNewSession = false,
        executionParams = "",
        iconPath = null,
        envVars = emptyMap(),
        keepSessionOpen = false
    )

    private fun createAutomation(scriptId: Int, label: String = "Auto", enabled: Boolean = true) = AutomationEntity(
        scriptId = scriptId,
        label = label,
        type = AutomationType.WEEKLY,
        scheduledTimestamp = System.currentTimeMillis(),
        daysOfWeek = listOf(1, 2, 3),
        isEnabled = enabled
    )
}