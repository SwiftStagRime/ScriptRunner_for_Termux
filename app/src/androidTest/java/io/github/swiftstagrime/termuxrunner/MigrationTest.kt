package io.github.swiftstagrime.termuxrunner

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.swiftstagrime.termuxrunner.data.local.AppDatabase
import io.github.swiftstagrime.termuxrunner.data.local.MIGRATION_6_7
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            AppDatabase::class.java,
        )

    @Test
    @Throws(IOException::class)
    fun migrate1To2_addsHeartbeatColumns() {
        var db =
            helper.createDatabase(TEST_DB, 1).apply {
                execSQL(
                    """
                    INSERT INTO scripts (name, code, interpreter, fileExtension, commandPrefix, 
                    runInBackground, openNewSession, executionParams, envVars, keepSessionOpen) 
                    VALUES ('V1 Script', 'echo hello', 'bash', '.sh', '', 0, 1, '', '{}', 0)
                    """.trimIndent(),
                )
                close()
            }

        db = helper.runMigrationsAndValidate(TEST_DB, 2, true)

        val cursor = db.query("SELECT * FROM scripts")
        cursor.moveToFirst()

        assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("useHeartbeat")))
        assertEquals(30000L, cursor.getLong(cursor.getColumnIndexOrThrow("heartbeatTimeout")))
        cursor.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate2To3_addsCategoryTableAndColumns() {
        var db =
            helper.createDatabase(TEST_DB, 2).apply {
                execSQL(
                    """
                    INSERT INTO scripts (name, code, interpreter, fileExtension, commandPrefix, 
                    runInBackground, openNewSession, executionParams, envVars, keepSessionOpen,
                    useHeartbeat, heartbeatTimeout, heartbeatInterval) 
                    VALUES ('V2 Script', 'exit', 'sh', '.sh', '', 0, 0, '', '{}', 0, 1, 5000, 1000)
                    """.trimIndent(),
                )
                close()
            }

        db = helper.runMigrationsAndValidate(TEST_DB, 3, true)

        val cursor = db.query("SELECT * FROM scripts")
        cursor.moveToFirst()

        assertNotEquals(-1, cursor.getColumnIndex("categoryId"))
        assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("orderIndex")))
        assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("notifyOnResult")))
        assertEquals("NONE", cursor.getString(cursor.getColumnIndexOrThrow("interactionMode")))
        assertEquals("", cursor.getString(cursor.getColumnIndexOrThrow("argumentPresets")))

        cursor.close()

        val catCursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='categories'")
        assertEquals(1, catCursor.count)
        catCursor.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate3To4_addsAutomationTables() {
        var db =
            helper.createDatabase(TEST_DB, 3).apply {
                execSQL(
                    """
                    INSERT INTO scripts (id, name, code, interpreter, fileExtension, commandPrefix, 
                    runInBackground, openNewSession, executionParams, envVars, keepSessionOpen) 
                    VALUES (1, 'V3 Script', 'ls', 'sh', '.sh', '', 0, 0, '', '{}', 0)
                    """.trimIndent(),
                )
                close()
            }

        db = helper.runMigrationsAndValidate(TEST_DB, 4, true)

        val tableCursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='automations'")
        assertTrue("Automations table should exist", tableCursor.count > 0)
        tableCursor.close()

        db.execSQL(
            """
            INSERT INTO automations (scriptId, label, type, scheduledTimestamp, intervalMillis, 
            daysOfWeek, isEnabled, runIfMissed, runtimeEnv, requireWifi, requireCharging, batteryThreshold)
            VALUES (1, 'Daily Backup', 'SCHEDULED', 1672531200000, 86400000, 'MTWTFSS', 1, 1, '{}', 0, 0, 0)
            """.trimIndent(),
        )

        val autoCursor = db.query("SELECT * FROM automations WHERE scriptId = 1")
        assertTrue(autoCursor.moveToFirst())
        assertEquals("Daily Backup", autoCursor.getString(autoCursor.getColumnIndexOrThrow("label")))
        autoCursor.close()

        val logTableCursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='automation_logs'")
        assertEquals(1, logTableCursor.count)
        logTableCursor.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrateAll_transitiveTest() {
        helper.createDatabase(TEST_DB, 1).close()

        val db = helper.runMigrationsAndValidate(TEST_DB, 4, true)

        val tables = listOf("scripts", "categories", "automations", "automation_logs")
        tables.forEach { tableName ->
            val cursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='$tableName'")
            assertEquals("Table $tableName should exist in V4", 1, cursor.count)
            cursor.close()
        }
    }

    @Test
    fun migrationFrom1To4_preservesAllOriginalData() {
        val dbV1 =
            helper.createDatabase(TEST_DB, 1).apply {
                execSQL(
                    "INSERT INTO scripts (name, code, interpreter, fileExtension, commandPrefix, runInBackground, openNewSession, executionParams, envVars, keepSessionOpen) " +
                        "VALUES ('Safety Test', 'echo 123', 'bash', '.sh', 'sudo', 1, 0, '--opt', '{\"KEY\":\"VAL\"}', 1)",
                )
                close()
            }

        val dbV4 = helper.runMigrationsAndValidate(TEST_DB, 4, true)

        val cursor = dbV4.query("SELECT * FROM scripts WHERE name = 'Safety Test'")
        assertTrue(cursor.moveToFirst())

        assertEquals("echo 123", cursor.getString(cursor.getColumnIndexOrThrow("code")))
        assertEquals("bash", cursor.getString(cursor.getColumnIndexOrThrow("interpreter")))
        assertEquals("sudo", cursor.getString(cursor.getColumnIndexOrThrow("commandPrefix")))
        assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("runInBackground")))
        assertEquals("{\"KEY\":\"VAL\"}", cursor.getString(cursor.getColumnIndexOrThrow("envVars")))
        assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("keepSessionOpen")))

        cursor.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate4To5_addsNewField() {
        val db =
            helper.createDatabase(TEST_DB, 4).apply {
                execSQL(
                    """
                    INSERT INTO scripts (id, name, code, interpreter, fileExtension, commandPrefix,
                    runInBackground, openNewSession, executionParams, envVars, keepSessionOpen) 
                    VALUES (1, 'V4 Script', 'ls', 'sh', '.sh', '', 0, 0, '', '{}', 0)
                    """.trimIndent(),
                )
                close()
            }

        val migratedDb = helper.runMigrationsAndValidate(TEST_DB, 5, true)

        val cursor = migratedDb.query("SELECT * FROM scripts WHERE id = 1")
        assertTrue(cursor.moveToFirst())

        assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("orderIndex")))

        cursor.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate6To7_renamesCodeColumnToCodePagesAndWrapsInJsonArray() {
        var db =
            helper.createDatabase(TEST_DB, 6).apply {
                execSQL(
                    """
                    INSERT INTO scripts (id, name, code, interpreter, fileExtension, commandPrefix,
                    runInBackground, openNewSession, executionParams, envVars, keepSessionOpen,
                    useHeartbeat, heartbeatTimeout, heartbeatInterval, categoryId, orderIndex,
                    notifyOnResult, interactionMode, argumentPresets, prefixPresets, envVarPresets, adbCode)
                    VALUES (1, 'Test Script', 'echo "hello world"', 'bash', '.sh', '', 0, 1, '', '{}', 0,
                    0, 30000, 10000, null, 0, 0, 'NONE', '', '', '', null)
                    """.trimIndent(),
                )
                execSQL(
                    """
                    INSERT INTO scripts (id, name, code, interpreter, fileExtension, commandPrefix,
                    runInBackground, openNewSession, executionParams, envVars, keepSessionOpen,
                    useHeartbeat, heartbeatTimeout, heartbeatInterval, categoryId, orderIndex,
                    notifyOnResult, interactionMode, argumentPresets, prefixPresets, envVarPresets, adbCode)
                    VALUES (2, 'Script with "quotes"', 'ls -la', 'sh', '.sh', '', 0, 0, '', '{}', 0,
                    0, 30000, 10000, null, 0, 0, 'NONE', '', '', '', null)
                    """.trimIndent(),
                )
                close()
            }

        db = helper.runMigrationsAndValidate(TEST_DB, 7, true, MIGRATION_6_7)

        val codePagesCheck = db.query("SELECT codePages FROM scripts LIMIT 1")
        assertTrue("codePages column should exist", codePagesCheck.getColumnIndexOrThrow("codePages") >= 0)
        codePagesCheck.close()

        val cursor = db.query("SELECT * FROM scripts WHERE id = 1")
        assertTrue(cursor.moveToFirst())

        val codePagesIndex = cursor.getColumnIndexOrThrow("codePages")
        val codePages = cursor.getString(codePagesIndex)
        assertTrue("codePages should be JSON array", codePages.startsWith("["))
        assertTrue("codePages should contain original code", codePages.contains("echo \\\"hello world\\\""))

        val cursor2 = db.query("SELECT * FROM scripts WHERE id = 2")
        assertTrue(cursor2.moveToFirst())
        val codePages2 = cursor2.getString(cursor2.getColumnIndexOrThrow("codePages"))
        assertTrue("codePages should be JSON array", codePages2.startsWith("["))
        assertTrue("codePages should contain original code", codePages2.contains("ls -la"))

        assertEquals("Test Script", cursor.getString(cursor.getColumnIndexOrThrow("name")))
        assertEquals("bash", cursor.getString(cursor.getColumnIndexOrThrow("interpreter")))

        val pageNamesIndex = cursor.getColumnIndexOrThrow("page_names")
        val pageNames = cursor.getString(pageNamesIndex)
        assertEquals("page_names should be empty array for migrated scripts", "[]", pageNames)

        cursor.close()
        cursor2.close()
    }
}
