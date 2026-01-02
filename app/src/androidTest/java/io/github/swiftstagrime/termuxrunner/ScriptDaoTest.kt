package io.github.swiftstagrime.termuxrunner

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.swiftstagrime.termuxrunner.data.local.AppDatabase
import io.github.swiftstagrime.termuxrunner.data.local.dao.ScriptDao
import io.github.swiftstagrime.termuxrunner.data.local.entity.ScriptEntity
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class ScriptDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: ScriptDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = db.scriptDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun writeUserAndReadInList() = runBlocking {
        val script = ScriptEntity(
            name = "Test Script",
            code = "ls -la",
            interpreter = "bash",
            fileExtension = "sh",
            commandPrefix = "",
            runInBackground = true,
            openNewSession = false,
            executionParams = "",
            iconPath = null,
            envVars = mapOf("TEST" to "1"),
            keepSessionOpen = true,
            useHeartbeat = false,
            heartbeatTimeout = 30000,
            heartbeatInterval = 10000
        )

        dao.insertScript(script)

        val scripts = dao.getAllScripts().first()

        assertEquals(1, scripts.size)
        assertEquals("Test Script", scripts[0].name)
        assertEquals("ls -la", scripts[0].code)
        assertEquals("1", scripts[0].envVars["TEST"])
    }

    @Test
    fun deleteScript() = runBlocking {
        val script = ScriptEntity(
            id = 1,
            name = "To Delete",
            code = "",
            interpreter = "bash",
            fileExtension = "sh",
            commandPrefix = "",
            runInBackground = false,
            openNewSession = false,
            executionParams = "",
            iconPath = null,
            envVars = emptyMap(),
            keepSessionOpen = false,
            useHeartbeat = false,
            heartbeatTimeout = 30000,
            heartbeatInterval = 10000
        )

        dao.insertScript(script)
        dao.deleteScript(script)

        val scripts = dao.getAllScripts().first()
        assertTrue(scripts.isEmpty())
    }
}
