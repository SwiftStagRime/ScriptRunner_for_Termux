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
import io.github.swiftstagrime.termuxrunner.data.local.dao.ScriptExecutionDao
import io.github.swiftstagrime.termuxrunner.data.local.entity.AutomationEntity
import io.github.swiftstagrime.termuxrunner.data.local.entity.AutomationLogEntity
import io.github.swiftstagrime.termuxrunner.data.local.entity.CategoryEntity
import io.github.swiftstagrime.termuxrunner.data.local.entity.ScriptEntity
import io.github.swiftstagrime.termuxrunner.data.local.entity.ScriptExecutionEntity
import io.github.swiftstagrime.termuxrunner.domain.model.AutomationType
import io.github.swiftstagrime.termuxrunner.domain.model.ForegroundSessionBehavior
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
    private lateinit var executionDao: ScriptExecutionDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db =
            Room
                .inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()

        scriptDao = db.scriptDao()
        categoryDao = db.categoryDao()
        automationDao = db.automationDao()
        logDao = db.automationLogDao()
        executionDao = db.scriptExecutionDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndGetScript() =
        runTest {
            val script = createScript(name = "Test Script")
            val id = scriptDao.insertScript(script).toInt()

            val fetched = scriptDao.getScriptById(id)
            assertEquals("Test Script", fetched?.name)
        }

    @Test
    fun insertAndGetScriptPreservesForegroundSessionBehavior() =
        runTest {
            val script =
                createScript(name = "FgBehavior").copy(
                    foregroundSessionBehavior = ForegroundSessionBehavior.SWITCH_OPEN,
                )
            val id = scriptDao.insertScript(script).toInt()

            val fetched = scriptDao.getScriptById(id)
            assertEquals(
                ForegroundSessionBehavior.SWITCH_OPEN,
                fetched?.foregroundSessionBehavior,
            )

            val plainId = scriptDao.insertScript(createScript(name = "Plain")).toInt()
            val plainFetched = scriptDao.getScriptById(plainId)
            assertEquals(
                ForegroundSessionBehavior.KEEP_OPEN,
                plainFetched?.foregroundSessionBehavior,
            )
        }

    @Test
    fun insertAndGetScriptPreservesReuseSession() =
        runTest {
            val script = createScript(name = "Reuse").copy(reuseSession = true)
            val id = scriptDao.insertScript(script).toInt()

            val fetched = scriptDao.getScriptById(id)
            assertTrue(fetched?.reuseSession == true)

            val plainId = scriptDao.insertScript(createScript(name = "PlainReuse")).toInt()
            val plainFetched = scriptDao.getScriptById(plainId)
            assertTrue(plainFetched?.reuseSession == false)
        }

    @Test
    fun updateScriptsOrderTransaction() =
        runTest {
            val id1 = scriptDao.insertScript(createScript(name = "S1")).toInt()
            val id2 = scriptDao.insertScript(createScript(name = "S2")).toInt()

            scriptDao.updateScriptsOrder(listOf(id1 to 10, id2 to 20))

            val scripts = scriptDao.getAllScriptsOneShot()
            assertEquals(10, scripts.find { it.id == id1 }?.orderIndex)
            assertEquals(20, scripts.find { it.id == id2 }?.orderIndex)
        }

    @Test
    fun insertAndObserveCategories() =
        runTest {
            val categoryId =
                categoryDao.insertCategory(CategoryEntity(name = "Utils", orderIndex = 1)).toInt()
            val categories = categoryDao.getAllCategories().first()
            assertEquals(1, categories.size)
            assertEquals("Utils", categories[0].name)
            assertEquals(categoryId, categories[0].id)
        }

    @Test
    fun automationFilteringByEnabled() =
        runTest {
            val scriptId = scriptDao.insertScript(createScript()).toInt()

            automationDao.insertAutomation(createAutomation(scriptId, "Auto 1", true))
            automationDao.insertAutomation(createAutomation(scriptId, "Auto 2", false))

            val enabled = automationDao.getEnabledAutomations()
            assertEquals(1, enabled.size)
            assertEquals("Auto 1", enabled[0].label)
        }

    @Test
    fun updateLastResultUpdatesSpecificFields() =
        runTest {
            val scriptId = scriptDao.insertScript(createScript()).toInt()
            val autoId = automationDao.insertAutomation(createAutomation(scriptId)).toInt()

            val timestamp = 123456789L
            automationDao.updateLastResult(autoId, 0, timestamp)

            val updated = automationDao.getAutomationById(autoId)
            assertEquals(0, updated?.lastExitCode)
            assertEquals(timestamp, updated?.lastRunTimestamp)
        }

    @Test
    fun foreignKeyDeleteCascade() =
        runTest {
            val script = createScript()
            val scriptId = scriptDao.insertScript(script).toInt()
            automationDao.insertAutomation(createAutomation(scriptId))

            scriptDao.deleteScript(script.copy(id = scriptId))

            val automations = automationDao.getAllAutomationsOneShot()
            assertTrue(automations.isEmpty())
        }

    @Test
    fun logCleanupByThreshold() =
        runTest {
            val scriptId = scriptDao.insertScript(createScript()).toInt()
            val autoId = automationDao.insertAutomation(createAutomation(scriptId)).toInt()

            logDao.insertLog(
                AutomationLogEntity(
                    automationId = autoId,
                    timestamp = 100,
                    exitCode = 0,
                ),
            )
            logDao.insertLog(
                AutomationLogEntity(
                    automationId = autoId,
                    timestamp = 500,
                    exitCode = 0,
                ),
            )

            logDao.deleteOldLogs(300)

            logDao.getLogsForAutomation(autoId).first().let { logs ->
                assertEquals(1, logs.size)
                assertEquals(500L, logs[0].timestamp)
            }
        }

    @Test
    fun logLimitCheck() =
        runTest {
            val scriptId = scriptDao.insertScript(createScript()).toInt()
            val autoId = automationDao.insertAutomation(createAutomation(scriptId)).toInt()

            repeat(60) { i ->
                logDao.insertLog(
                    AutomationLogEntity(
                        automationId = autoId,
                        timestamp = i.toLong(),
                        exitCode = 0,
                    ),
                )
            }

            val logs = logDao.getLogsForAutomation(autoId).first()
            assertEquals(50, logs.size)
            assertTrue(logs[0].timestamp > logs[1].timestamp)
        }

    @Test
    fun insertAndGetScriptExecution() =
        runTest {
            val execution =
                ScriptExecutionEntity(
                    scriptId = 1,
                    scriptName = "TestScript",
                    timestamp = System.currentTimeMillis(),
                    exitCode = 0,
                    durationMs = 1500L,
                    source = ScriptExecutionEntity.ExecutionSource.MANUAL,
                )
            val id = executionDao.insert(execution)

            assertTrue("Insert should return positive ID", id > 0)

            val recent = executionDao.getRecentExecutions(10).first()
            assertEquals(1, recent.size)
            assertEquals("TestScript", recent[0].scriptName)
            assertEquals(0, recent[0].exitCode)
        }

    @Test
    fun getExecutionsForScriptFiltersByScriptIdAndOrdersDesc() =
        runTest {
            val now = System.currentTimeMillis()
            executionDao.insert(
                ScriptExecutionEntity(
                    scriptId = 1,
                    scriptName = "Old",
                    timestamp = now - 10000,
                    exitCode = 0,
                ),
            )
            executionDao.insert(
                ScriptExecutionEntity(
                    scriptId = 1,
                    scriptName = "New",
                    timestamp = now,
                    exitCode = 1,
                ),
            )
            executionDao.insert(
                ScriptExecutionEntity(
                    scriptId = 2,
                    scriptName = "Other",
                    timestamp = now - 5000,
                    exitCode = 0,
                ),
            )

            val results = executionDao.getExecutionsForScript(1).first()

            assertEquals(2, results.size)
            assertEquals("New", results[0].scriptName)
            assertEquals("Old", results[1].scriptName)
        }

    @Test
    fun getRecentExecutionsRespectsLimit() =
        runTest {
            repeat(15) { i ->
                executionDao.insert(
                    ScriptExecutionEntity(
                        scriptId = 1,
                        scriptName = "Script $i",
                        timestamp = System.currentTimeMillis() + i,
                        exitCode = 0,
                    ),
                )
            }

            val results = executionDao.getRecentExecutions(5).first()
            assertEquals(5, results.size)
        }

    @Test
    fun deleteOldRecordsRemovesOnlyBeforeThreshold() =
        runTest {
            val now = System.currentTimeMillis()
            executionDao.insert(
                ScriptExecutionEntity(
                    scriptId = 1,
                    scriptName = "Very Old",
                    timestamp = now - 100000,
                    exitCode = 0,
                ),
            )
            executionDao.insert(
                ScriptExecutionEntity(
                    scriptId = 1,
                    scriptName = "Old",
                    timestamp = now - 50000,
                    exitCode = 0,
                ),
            )
            executionDao.insert(
                ScriptExecutionEntity(
                    scriptId = 1,
                    scriptName = "Recent",
                    timestamp = now,
                    exitCode = 0,
                ),
            )

            executionDao.deleteOldRecords(now - 75000)

            val remaining = executionDao.getAllExecutions().first()
            assertEquals(2, remaining.size)
        }

    @Test
    fun deleteForScriptRemovesOnlyMatchingScriptId() =
        runTest {
            executionDao.insert(
                ScriptExecutionEntity(
                    scriptId = 1,
                    scriptName = "A",
                    timestamp = System.currentTimeMillis(),
                    exitCode = 0,
                ),
            )
            executionDao.insert(
                ScriptExecutionEntity(
                    scriptId = 2,
                    scriptName = "B",
                    timestamp = System.currentTimeMillis(),
                    exitCode = 0,
                ),
            )
            executionDao.insert(
                ScriptExecutionEntity(
                    scriptId = 1,
                    scriptName = "C",
                    timestamp = System.currentTimeMillis(),
                    exitCode = 0,
                ),
            )

            executionDao.deleteForScript(1)

            val remaining = executionDao.getAllExecutions().first()
            assertEquals(1, remaining.size)
            assertEquals(2, remaining[0].scriptId)
        }

    @Test
    fun clearAllRemovesAllRecords() =
        runTest {
            repeat(5) {
                executionDao.insert(
                    ScriptExecutionEntity(
                        scriptId = 1,
                        scriptName = "Script",
                        timestamp = System.currentTimeMillis(),
                        exitCode = 0,
                    ),
                )
            }

            executionDao.clearAll()

            val remaining = executionDao.getAllExecutions().first()
            assertTrue(remaining.isEmpty())
        }

    @Test
    fun getFailureCountReturnsCorrectCount() =
        runTest {
            executionDao.insert(
                ScriptExecutionEntity(
                    scriptId = 1,
                    scriptName = "Success",
                    timestamp = System.currentTimeMillis(),
                    exitCode = 0,
                ),
            )
            executionDao.insert(
                ScriptExecutionEntity(
                    scriptId = 1,
                    scriptName = "Fail1",
                    timestamp = System.currentTimeMillis(),
                    exitCode = 1,
                ),
            )
            executionDao.insert(
                ScriptExecutionEntity(
                    scriptId = 2,
                    scriptName = "Fail2",
                    timestamp = System.currentTimeMillis(),
                    exitCode = 127,
                ),
            )

            val count = executionDao.getFailureCount().first()
            assertEquals(2, count)
        }

    @Test
    fun getTotalCountReturnsCorrectTotal() =
        runTest {
            repeat(10) {
                executionDao.insert(
                    ScriptExecutionEntity(
                        scriptId = 1,
                        scriptName = "Script",
                        timestamp = System.currentTimeMillis(),
                        exitCode = 0,
                    ),
                )
            }

            val count = executionDao.getTotalCount().first()
            assertEquals(10, count)
        }

    @Test
    fun deleteSingleExecutionRemovesCorrectRecord() =
        runTest {
            val exec =
                ScriptExecutionEntity(
                    scriptId = 1,
                    scriptName = "DeleteMe",
                    timestamp = System.currentTimeMillis(),
                    exitCode = 0,
                )
            val id = executionDao.insert(exec)

            executionDao.delete(
                ScriptExecutionEntity(
                    id = id,
                    scriptId = 1,
                    scriptName = "DeleteMe",
                    timestamp = System.currentTimeMillis(),
                    exitCode = 0,
                ),
            )

            val remaining = executionDao.getAllExecutions().first()
            assertTrue(remaining.none { it.id == id })
        }

    @Test
    fun getExecutionsWithScriptsJoinsWithScriptsTable() =
        runTest {
            scriptDao.insertScript(createScript(name = "Joined"))
            executionDao.insert(
                ScriptExecutionEntity(
                    scriptId = 1,
                    scriptName = "Exec",
                    timestamp = System.currentTimeMillis(),
                    exitCode = 0,
                ),
            )

            val results = executionDao.getExecutionsWithScripts().first()
            assertTrue(results.isNotEmpty())
        }

    private fun createScript(name: String = "Test") =
        ScriptEntity(
            name = name,
            codePages = listOf("echo hello"),
            interpreter = "bash",
            runInBackground = true,
            openNewSession = false,
            executionParams = "",
            iconPath = null,
            envVars = emptyMap(),
            keepSessionOpen = false,
        )

    private fun createAutomation(
        scriptId: Int,
        label: String = "Auto",
        enabled: Boolean = true,
    ) = AutomationEntity(
        scriptId = scriptId,
        label = label,
        type = AutomationType.WEEKLY,
        scheduledTimestamp = System.currentTimeMillis(),
        daysOfWeek = listOf(1, 2, 3),
        isEnabled = enabled,
    )
}
