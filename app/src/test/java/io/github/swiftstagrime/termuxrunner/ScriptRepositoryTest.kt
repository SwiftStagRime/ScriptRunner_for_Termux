package io.github.swiftstagrime.termuxrunner

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import io.github.swiftstagrime.termuxrunner.data.local.dao.AutomationDao
import io.github.swiftstagrime.termuxrunner.data.local.dao.CategoryDao
import io.github.swiftstagrime.termuxrunner.data.local.dao.ScriptDao
import io.github.swiftstagrime.termuxrunner.data.local.dto.FullBackupDto
import io.github.swiftstagrime.termuxrunner.data.local.entity.AutomationEntity
import io.github.swiftstagrime.termuxrunner.data.local.entity.CategoryEntity
import io.github.swiftstagrime.termuxrunner.data.local.entity.ScriptEntity
import io.github.swiftstagrime.termuxrunner.data.repository.ScriptRepositoryImpl
import io.github.swiftstagrime.termuxrunner.domain.model.InteractionMode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertArrayEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class, sdk = [33])
class ScriptRepositoryImplTest {
    private lateinit var repository: ScriptRepositoryImpl
    private val scriptDao = mockk<ScriptDao>(relaxed = true)
    private val categoryDao = mockk<CategoryDao>(relaxed = true)
    private val automationDao = mockk<AutomationDao>(relaxed = true)
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val capturedScript = slot<ScriptEntity>()
    private val capturedAutomation = slot<AutomationEntity>()

    private fun validScriptJson(
        id: Int,
        name: String,
    ) = """
        "id": $id, "name": "$name", "code": "ls", "interpreter": "bash",
        "fileExtension": "sh", "commandPrefix": "", "runInBackground": false,
        "openNewSession": true, "executionParams": "", "envVars": {}, "keepSessionOpen": false
        """.trimIndent()

    private fun validAutomationJson(scriptId: Int) =
        """
        "scriptId": $scriptId, "type": "ONE_TIME", "scheduledTimestamp": 0,
        "intervalMillis": 0, "daysOfWeek": [], "isEnabled": false,
        "runtimeArgs": null, "runtimeEnv": null, "runtimePrefix": null,
        "label": "Auto", "runIfMissed": false, "lastExitCode": null,
        "requireWifi": false, "requireCharging": false, "batteryThreshold": 0
        """.trimIndent()

    @Before
    fun setup() {
        repository = ScriptRepositoryImpl(scriptDao, categoryDao, automationDao, context)
        java.io.File(context.filesDir, "script_icons").deleteRecursively()
        Robolectric.setupContentProvider(TestFileProvider::class.java, "io.github.swiftstagrime.provider")
    }

    private fun setupMockFile(
        fileName: String,
        content: String,
    ): Uri {
        TestFileProvider.fileName = fileName
        TestFileProvider.fileContent = content
        return Uri.parse("content://io.github.swiftstagrime.provider/$fileName")
    }

    @Test
    fun `importScripts links automations to new script IDs`() =
        runTest {
            val json =
                """
                {
                    "categories": [],
                    "scripts": [{ ${validScriptJson(555, "Test")} }],
                    "automations": [{ ${validAutomationJson(555)} }]
                }
                """.trimIndent()

            val uri = setupMockFile("backup.json", json)
            coEvery { scriptDao.insertScript(any()) } returns 100L
            coEvery { automationDao.insertAutomation(capture(capturedAutomation)) } returns 1L

            val result = repository.importScripts(uri)
            assertTrue("Import failed: ${result.exceptionOrNull()}", result.isSuccess)
            assertEquals(100, capturedAutomation.captured.scriptId)
        }

    @Test
    fun `importSingleScript detects python and adds shebang`() =
        runTest {
            val uri = setupMockFile("test.py", "print('hello')")
            val result = repository.importSingleScript(uri)

            assertTrue(result.isSuccess)
            val script = result.getOrThrow()
            assertEquals("python", script.interpreter)
            assertTrue(script.code.contains("/usr/bin/python"))
        }

    @Test
    fun `importScripts preserves all entity fields including presets`() =
        runTest {
            val json =
                """
                {
                    "categories": [],
                    "scripts": [{
                        ${validScriptJson(1, "FullTest")},
                        "interactionMode": "MULTI_CHOICE",
                        "argumentPresets": ["--a"],
                        "prefixPresets": ["sudo"],
                        "envVarPresets": ["K=V"]
                    }],
                    "automations": []
                }
                """.trimIndent()

            val uri = setupMockFile("presets.json", json)
            coEvery { scriptDao.insertScript(capture(capturedScript)) } returns 1L

            val result = repository.importScripts(uri)
            assertTrue("Import failed: ${result.exceptionOrNull()}", result.isSuccess)

            val entity = capturedScript.captured
            assertEquals(InteractionMode.MULTI_CHOICE, entity.interactionMode)
            assertEquals(listOf("--a"), entity.argumentPresets)
        }

    @Test
    fun `importScripts deduplicates categories by name`() =
        runTest {
            val json = """{
            "categories": [{"id": 9, "name": "Existing", "orderIndex": 0}],
            "scripts": [{ ${validScriptJson(1, "S")}, "categoryId": 9}],
            "automations": []
        }"""

            coEvery { categoryDao.getAllCategoriesOneShot() } returns listOf(CategoryEntity(id = 77, name = "Existing"))
            coEvery { scriptDao.insertScript(capture(capturedScript)) } returns 1L

            val uri = setupMockFile("cat.json", json)
            val finalResult = repository.importScripts(uri)

            assertTrue("Import failed: ${finalResult.exceptionOrNull()}", finalResult.isSuccess)
            coVerify(exactly = 0) { categoryDao.insertCategory(any()) }
            assertEquals(77, capturedScript.captured.categoryId)
        }

    @Test
    fun `importScripts handles legacy list format`() =
        runTest {
            val json = """[{ ${validScriptJson(1, "Legacy")} }]"""

            val uri = setupMockFile("legacy.json", json)
            coEvery { scriptDao.insertScript(capture(capturedScript)) } returns 1L

            val result = repository.importScripts(uri)

            assertTrue("Import failed: ${result.exceptionOrNull()}", result.isSuccess)
            assertEquals("Legacy", capturedScript.captured.name)
        }

    @Test
    fun `importSingleScript does not add redundant shebang if already present`() =
        runTest {
            val content = "#!/bin/custom\nprint(1)"
            val uri = setupMockFile("test.py", content)

            val result = repository.importSingleScript(uri)

            assertEquals(content, result.getOrThrow().code)
        }

    @Test
    fun `importSingleScript for compiled languages omits shebang`() =
        runTest {
            val uri = setupMockFile("Test.java", "class Test {}")

            val result = repository.importSingleScript(uri)

            val script = result.getOrThrow()
            assertEquals("java", script.interpreter)
            assertFalse(script.code.startsWith("#!"))
        }

    @Test
    fun `importScripts saves base64 icons to local storage`() =
        runTest {
            val dummyBase64 = Base64.encodeToString(byteArrayOf(10, 20, 30), Base64.NO_WRAP)
            val json = """{
            "categories": [],
            "scripts": [{ ${validScriptJson(1, "IconTest")}, "iconBase64": "$dummyBase64" }],
            "automations": []
        }"""

            val uri = setupMockFile("icon.json", json)
            coEvery { scriptDao.insertScript(capture(capturedScript)) } returns 1L

            repository.importScripts(uri)

            val savedPath = capturedScript.captured.iconPath
            assertNotNull(savedPath)
            val savedFile = java.io.File(savedPath!!)
            assertTrue(savedFile.exists())
            assertArrayEquals(byteArrayOf(10, 20, 30), savedFile.readBytes())
        }

    @Test
    fun `importScripts handles malformed json with failure result`() =
        runTest {
            val uri = setupMockFile("bad.json", "{ \"invalid\": true ")

            val result = repository.importScripts(uri)

            assertTrue(result.isFailure)
        }

    @Test
    fun `updateScriptsOrder delegates correctly to dao`() =
        runTest {
            val orders = listOf(1 to 0, 2 to 1)
            repository.updateScriptsOrder(orders)
            coVerify { scriptDao.updateScriptsOrder(orders) }
        }

    @Test
    fun `exportScripts includes all complex fields in json output`() =
        runTest {
            val script =
                ScriptEntity(
                    id = 1,
                    name = "FullFeature",
                    code = "exit 0",
                    interpreter = "python",
                    fileExtension = "py",
                    commandPrefix = "python3",
                    runInBackground = true,
                    openNewSession = false,
                    executionParams = "--debug",
                    envVars = mapOf("DEBUG" to "1"),
                    keepSessionOpen = true,
                    useHeartbeat = true,
                    heartbeatTimeout = 60000,
                    heartbeatInterval = 20000,
                    orderIndex = 5,
                    notifyOnResult = true,
                    interactionMode = InteractionMode.MULTI_CHOICE,
                    argumentPresets = listOf("--opt1", "--opt2"),
                    prefixPresets = listOf("sudo", "time"),
                    envVarPresets = listOf("KEY=VAL"),
                    iconPath = null,
                    categoryId = null,
                )

            coEvery { scriptDao.getAllScriptsOneShot() } returns listOf(script)
            coEvery { categoryDao.getAllCategoriesOneShot() } returns emptyList()
            coEvery { automationDao.getAllAutomationsOneShot() } returns emptyList()

            val outputFile = java.io.File(context.cacheDir, "full_export.json")
            val uri = Uri.fromFile(outputFile)

            repository.exportScripts(uri)

            val jsonString = outputFile.readText()
            val decoded = Json.decodeFromString<FullBackupDto>(jsonString)
            val exportedScript = decoded.scripts.first()

            assertEquals(InteractionMode.MULTI_CHOICE, exportedScript.interactionMode)
            assertEquals(listOf("--opt1", "--opt2"), exportedScript.argumentPresets)
            assertEquals(listOf("sudo", "time"), exportedScript.prefixPresets)
            assertEquals(listOf("KEY=VAL"), exportedScript.envVarPresets)
            assertEquals(true, exportedScript.useHeartbeat)
            assertEquals(60000L, exportedScript.heartbeatTimeout)
            assertEquals(mapOf("DEBUG" to "1"), exportedScript.envVars)
        }

    @Test
    fun `importScripts correctly maps all automation constraint fields`() =
        runTest {
            val json =
                """
                {
                    "version": 3,
                    "categories": [],
                    "scripts": [{ ${validScriptJson(10, "ConstraintTest")} }],
                    "automations": [{
                        "scriptId": 10,
                        "type": "PERIODIC",
                        "scheduledTimestamp": 1000,
                        "intervalMillis": 3600000,
                        "daysOfWeek": [1, 2, 3],
                        "isEnabled": true,
                        "label": "Heavy Task",
                        "runIfMissed": true,
                        "requireWifi": true,
                        "requireCharging": true,
                        "batteryThreshold": 20,
                        "runtimeArgs": "--force",
                        "runtimePrefix": "nice -n 19",
                        "runtimeEnv": {"TMP": "/sdcard"},
                        "lastExitCode": 0
                    }]
                }
                """.trimIndent()

            val uri = setupMockFile("constraints.json", json)
            coEvery { scriptDao.insertScript(any()) } returns 500L
            coEvery { automationDao.insertAutomation(capture(capturedAutomation)) } returns 1L

            repository.importScripts(uri)

            val auto = capturedAutomation.captured
            assertEquals(500, auto.scriptId)
            assertTrue(auto.requireWifi)
            assertTrue(auto.requireCharging)
            assertEquals(20, auto.batteryThreshold)
            assertEquals("--force", auto.runtimeArgs)
            assertEquals("nice -n 19", auto.runtimePrefix)
            assertEquals(mapOf("TMP" to "/sdcard"), auto.runtimeEnv)
            assertEquals(listOf(1, 2, 3), auto.daysOfWeek)
            assertFalse(auto.isEnabled)
        }

    @Test
    fun `importScripts handles legacy V2 format by providing missing required fields`() =
        runTest {
            val json =
                """
                {
                    "version": 2,
                    "categories": [],
                    "scripts": [
                        {
                            "id": 1,
                            "name": "V2Script",
                            "code": "echo hello",
                            "interpreter": "bash",
                            "fileExtension": "sh",
                            "commandPrefix": "",
                            "runInBackground": false,
                            "openNewSession": true,
                            "executionParams": "",
                            "envVars": {},
                            "keepSessionOpen": false
                        }
                    ]
                }
                """.trimIndent()

            val uri = setupMockFile("v2_fixed.json", json)
            coEvery { scriptDao.insertScript(capture(capturedScript)) } returns 1L

            val result = repository.importScripts(uri)

            assertTrue("Import failed: ${result.exceptionOrNull()}", result.isSuccess)
            assertEquals("V2Script", capturedScript.captured.name)
        }

    @Test
    fun `importScripts correctly maps all script fields to entity during import`() =
        runTest {
            val json =
                """
                {
                    "categories": [],
                    "scripts": [{
                        "id": 1,
                        "name": "CheckFields",
                        "code": "uptime",
                        "interpreter": "bash",
                        "fileExtension": "sh",
                        "commandPrefix": "tsu",
                        "runInBackground": true,
                        "openNewSession": false,
                        "executionParams": "-x",
                        "envVars": {"KEY": "VAL"},
                        "keepSessionOpen": true,
                        "useHeartbeat": true,
                        "heartbeatTimeout": 45000,
                        "heartbeatInterval": 15000,
                        "notifyOnResult": true,
                        "interactionMode": "TEXT_INPUT",
                        "argumentPresets": ["p1"],
                        "prefixPresets": ["pr1"],
                        "envVarPresets": ["e1"],
                        "orderIndex": 99
                    }],
                    "automations": []
                }
                """.trimIndent()

            val uri = setupMockFile("field_check.json", json)
            coEvery { scriptDao.insertScript(capture(capturedScript)) } returns 1L

            repository.importScripts(uri)

            val entity = capturedScript.captured
            assertEquals("CheckFields", entity.name)
            assertEquals("uptime", entity.code)
            assertEquals("tsu", entity.commandPrefix)
            assertTrue(entity.runInBackground)
            assertFalse(entity.openNewSession)
            assertEquals("-x", entity.executionParams)
            assertEquals(mapOf("KEY" to "VAL"), entity.envVars)
            assertTrue(entity.keepSessionOpen)
            assertTrue(entity.useHeartbeat)
            assertEquals(45000L, entity.heartbeatTimeout)
            assertEquals(15000L, entity.heartbeatInterval)
            assertTrue(entity.notifyOnResult)
            assertEquals(InteractionMode.TEXT_INPUT, entity.interactionMode)
            assertEquals(listOf("p1"), entity.argumentPresets)
            assertEquals(listOf("pr1"), entity.prefixPresets)
            assertEquals(listOf("e1"), entity.envVarPresets)
            assertEquals(99, entity.orderIndex)
        }

    @Test
    fun `importScripts correctly reconstructs category relationships`() =
        runTest {
            val json =
                """
                {
                    "categories": [
                        {"id": 100, "name": "Tools", "orderIndex": 1},
                        {"id": 200, "name": "Tests", "orderIndex": 2}
                    ],
                    "scripts": [
                        { ${validScriptJson(1, "S1")}, "categoryId": 100 },
                        { ${validScriptJson(2, "S2")}, "categoryId": 200 }
                    ],
                    "automations": []
                }
                """.trimIndent()

            val uri = setupMockFile("cat_rel.json", json)

            val categoryIdCounter =
                java.util.concurrent.atomic
                    .AtomicInteger(10)
            coEvery { categoryDao.insertCategory(any()) } answers { categoryIdCounter.getAndIncrement().toLong() }

            val scriptCategories = mutableListOf<Int?>()
            coEvery { scriptDao.insertScript(capture(capturedScript)) } answers {
                scriptCategories.add(capturedScript.captured.categoryId)
                1L
            }

            repository.importScripts(uri)

            assertEquals(2, scriptCategories.size)
            assertEquals(10, scriptCategories[0])
            assertEquals(11, scriptCategories[1])
        }

    @Test
    fun `importScripts handles Version 1 format (raw array) correctly`() =
        runTest {
            val json =
                """
                [
                    {
                        "name": "LegacyV1",
                        "code": "ls",
                        "envVars": {}
                    }
                ]
                """.trimIndent()

            val uri = setupMockFile("v1.json", json)
            coEvery { scriptDao.insertScript(capture(capturedScript)) } returns 1L

            val result = repository.importScripts(uri)

            assertTrue("V1 Import failed: ${result.exceptionOrNull()}", result.isSuccess)
            assertEquals("LegacyV1", capturedScript.captured.name)
            assertEquals("", capturedScript.captured.interpreter)
        }

    @Test
    fun `importScripts handles Version 2 format (object with scripts only) correctly`() =
        runTest {
            val json =
                """
                {
                    "scripts": [
                        {
                            "name": "LegacyV2",
                            "code": "whoami",
                            "envVars": {"USER": "root"}
                        }
                    ]
                }
                """.trimIndent()

            val uri = setupMockFile("v2.json", json)
            coEvery { scriptDao.insertScript(capture(capturedScript)) } returns 1L

            val result = repository.importScripts(uri)

            assertTrue("V2 Import failed: ${result.exceptionOrNull()}", result.isSuccess)
            assertEquals("LegacyV2", capturedScript.captured.name)
            assertEquals(mapOf("USER" to "root"), capturedScript.captured.envVars)
        }

    @Test
    fun `importScripts fills default values for missing optional DTO fields`() =
        runTest {
            val json =
                """
                {
                    "scripts": [
                        {
                            "name": "MinimalScript",
                            "code": "echo 1",
                            "envVars": {}
                        }
                    ]
                }
                """.trimIndent()

            val uri = setupMockFile("minimal.json", json)
            coEvery { scriptDao.insertScript(capture(capturedScript)) } returns 1L

            repository.importScripts(uri)

            val entity = capturedScript.captured
            assertEquals(InteractionMode.NONE, entity.interactionMode)
            assertFalse(entity.useHeartbeat)
            assertEquals(30000L, entity.heartbeatTimeout)
            assertEquals(emptyList<String>(), entity.argumentPresets)
        }

    @Test
    fun `exportScripts produces JSON compatible with Version 3 specification`() =
        runTest {
            val script =
                ScriptEntity(
                    name = "ExportTest",
                    code = "pwd",
                    interpreter = "sh",
                    envVars = emptyMap(),
                    runInBackground = false,
                    openNewSession = true,
                    executionParams = "",
                    keepSessionOpen = false,
                    iconPath = null,
                )

            coEvery { scriptDao.getAllScriptsOneShot() } returns listOf(script)
            coEvery { categoryDao.getAllCategoriesOneShot() } returns emptyList()
            coEvery { automationDao.getAllAutomationsOneShot() } returns emptyList()

            val outputFile = java.io.File(context.cacheDir, "v3_export.json")
            val uri = Uri.fromFile(outputFile)

            repository.exportScripts(uri)

            val jsonString = outputFile.readText()
            assertTrue(jsonString.contains("\"version\": 3"))
            assertTrue(jsonString.contains("\"categories\": []"))
            assertTrue(jsonString.contains("\"scripts\":"))
            assertTrue(jsonString.contains("\"automations\": []"))
        }

    @Test
    fun `importScripts maintains data integrity when envVars are complex`() =
        runTest {
            val json =
                """
                {
                    "scripts": [
                        {
                            "name": "EnvTest",
                            "code": "printenv",
                            "envVars": {
                                "PATH": "/usr/bin:/bin",
                                "EMPTY": "",
                                "SPECIAL": "!@#$%^&*()"
                            }
                        }
                    ]
                }
                """.trimIndent()

            val uri = setupMockFile("env.json", json)
            coEvery { scriptDao.insertScript(capture(capturedScript)) } returns 1L

            repository.importScripts(uri)

            val env = capturedScript.captured.envVars
            assertEquals("/usr/bin:/bin", env["PATH"])
            assertEquals("", env["EMPTY"])
            assertEquals("!@#$%^&*()", env["SPECIAL"])
        }

    @Test
    fun `importSingleScript correctly handles shebang with whitespaces and newlines`() =
        runTest {
            val rawContent = "   \n\n#!/bin/sh\necho 1"
            val uri = setupMockFile("clean.sh", rawContent)

            val result = repository.importSingleScript(uri)

            assertEquals(rawContent, result.getOrThrow().code)
        }
}

class TestFileProvider : android.content.ContentProvider() {
    companion object {
        var fileName: String = "default.json"
        var fileContent: String = "{}"
    }

    override fun query(
        u: Uri,
        p: Array<out String>?,
        s: String?,
        sa: Array<out String>?,
        o: String?,
    ): android.database.Cursor {
        val cursor =
            android.database.MatrixCursor(arrayOf(android.provider.OpenableColumns.DISPLAY_NAME))
        cursor.addRow(arrayOf(fileName))
        return cursor
    }

    override fun openFile(
        uri: Uri,
        mode: String,
    ): android.os.ParcelFileDescriptor? {
        val file = java.io.File.createTempFile("test_import", ".tmp")
        file.writeText(fileContent)
        return android.os.ParcelFileDescriptor.open(
            file,
            android.os.ParcelFileDescriptor.MODE_READ_ONLY,
        )
    }

    override fun insert(
        u: Uri,
        v: android.content.ContentValues?,
    ): Uri? = null

    override fun update(
        u: Uri,
        v: android.content.ContentValues?,
        s: String?,
        sa: Array<out String>?,
    ): Int = 0

    override fun delete(
        u: Uri,
        s: String?,
        sa: Array<out String>?,
    ): Int = 0

    override fun getType(u: Uri): String? = null

    override fun onCreate(): Boolean = true
}
