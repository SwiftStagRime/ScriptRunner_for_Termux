package io.github.swiftstagrime.termuxrunner

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.swiftstagrime.termuxrunner.data.local.AppDatabase
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {
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
                    VALUES ('Test Script', 'echo hello', 'bash', '.sh', '', 0, 1, '', '{}', 0)
                    """.trimIndent(),
                )
                close()
            }

        db = helper.runMigrationsAndValidate(TEST_DB, 2, true)

        val cursor = db.query("SELECT * FROM scripts")
        cursor.moveToFirst()

        val heartbeatIndex = cursor.getColumnIndex("useHeartbeat")
        val timeoutIndex = cursor.getColumnIndex("heartbeatTimeout")

        assertNotEquals(-1, heartbeatIndex)
        assertEquals(0, cursor.getInt(heartbeatIndex))
        assertEquals(30000L, cursor.getLong(timeoutIndex))

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

        val scriptCursor = db.query("SELECT * FROM scripts")
        scriptCursor.moveToFirst()

        val categoryIdIndex = scriptCursor.getColumnIndex("categoryId")
        val orderIndex = scriptCursor.getColumnIndex("orderIndex")

        assertNotEquals(-1, categoryIdIndex)
        assertTrue(scriptCursor.isNull(categoryIdIndex))
        assertEquals(0, scriptCursor.getInt(orderIndex))
        scriptCursor.close()

        val catCursor =
            db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='categories'")
        assertEquals(1, catCursor.count)
        catCursor.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrateAll_transitiveTest() {
        helper.createDatabase(TEST_DB, 1).close()
        helper.runMigrationsAndValidate(TEST_DB, 3, true)
    }
}
