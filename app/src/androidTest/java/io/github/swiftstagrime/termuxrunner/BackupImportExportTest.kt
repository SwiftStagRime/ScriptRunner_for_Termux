package io.github.swiftstagrime.termuxrunner

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.swiftstagrime.termuxrunner.data.local.AppDatabase
import io.github.swiftstagrime.termuxrunner.data.local.dao.AutomationDao
import io.github.swiftstagrime.termuxrunner.data.local.dao.CategoryDao
import io.github.swiftstagrime.termuxrunner.data.local.dao.CustomThemeDao
import io.github.swiftstagrime.termuxrunner.data.local.dao.ScriptDao
import io.github.swiftstagrime.termuxrunner.data.local.dto.FullBackupDto
import io.github.swiftstagrime.termuxrunner.data.local.entity.AutomationEntity
import io.github.swiftstagrime.termuxrunner.data.local.entity.CategoryEntity
import io.github.swiftstagrime.termuxrunner.data.local.entity.CustomThemeEntity
import io.github.swiftstagrime.termuxrunner.data.local.entity.ScriptEntity
import io.github.swiftstagrime.termuxrunner.data.repository.ScriptRepositoryImpl
import io.github.swiftstagrime.termuxrunner.domain.model.AutomationType
import io.github.swiftstagrime.termuxrunner.domain.model.InteractionMode
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class BackupImportExportTest {
    private lateinit var db: AppDatabase
    private lateinit var scriptDao: ScriptDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var automationDao: AutomationDao
    private lateinit var customThemeDao: CustomThemeDao
    private lateinit var repository: ScriptRepositoryImpl
    private lateinit var context: Context

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        scriptDao = db.scriptDao()
        categoryDao = db.categoryDao()
        automationDao = db.automationDao()
        customThemeDao = db.customThemeDao()

        repository = ScriptRepositoryImpl(
            dao = scriptDao,
            categoryDao = categoryDao,
            automationDao = automationDao,
            customThemeDao = customThemeDao,
            appDatabase = db,
            context = context,
        )
    }

    @After
    fun closeDb() {
        db.close()
    }

    private fun writeTempJson(content: String): File {
        val file = File(context.cacheDir, "test_backup_${System.currentTimeMillis()}.json")
        file.writeText(content)
        return file
    }

    // --- V1 Format Tests (raw JSON array of scripts) ---

    @Test
    fun importV1_legacyFormat_rawArray_importsScriptsCorrectly() = runTest {
        val v1Json = """
            [
                {
                    "name": "LegacyScript1",
                    "code": "echo hello",
                    "envVars": {},
                    "interpreter": "bash"
                },
                {
                    "name": "LegacyScript2",
                    "code": "ls -la",
                    "envVars": {"HOME": "/data"},
                    "fileExtension": "sh"
                }
            ]
        """.trimIndent()

        val file = writeTempJson(v1Json)
        val result = repository.importScripts(Uri.fromFile(file))

        assertTrue("Import should succeed: ${result.exceptionOrNull()}", result.isSuccess)

        val scripts = scriptDao.getAllScriptsOneShot()
        assertEquals(2, scripts.size)
        assertEquals("LegacyScript1", scripts[0].name)
        assertEquals(listOf("echo hello"), scripts[0].codePages)
        assertEquals("bash", scripts[0].interpreter)

        assertEquals("LegacyScript2", scripts[1].name)
        assertEquals(mapOf("HOME" to "/data"), scripts[1].envVars)
        assertEquals("sh", scripts[1].fileExtension)

        file.delete()
    }

    @Test
    fun importV1_format_migratesLegacyCodeFieldToCodePages() = runTest {
        val v1Json = """
            [
                {
                    "name": "CodeMigration",
                    "code": "echo migrated"
                }
            ]
        """.trimIndent()

        val file = writeTempJson(v1Json)
        repository.importScripts(Uri.fromFile(file))

        val script = scriptDao.getAllScriptsOneShot().first()
        assertEquals(listOf("echo migrated"), script.codePages)
        assertEquals(listOf("Main"), script.pageNames)

        file.delete()
    }

    // --- V2 Format Tests (object with only scripts field, no wrapper version) ---

    @Test
    fun importV2_format_withScriptsOnly_importsCorrectly() = runTest {
        val v2Json = """
            {
                "scripts": [
                    {
                        "id": 10,
                        "name": "V2Script",
                        "code": "whoami",
                        "interpreter": "bash",
                        "fileExtension": "sh",
                        "commandPrefix": "",
                        "runInBackground": false,
                        "openNewSession": true,
                        "executionParams": "",
                        "envVars": {"USER": "root"},
                        "keepSessionOpen": false
                    }
                ]
            }
        """.trimIndent()

        val file = writeTempJson(v2Json)
        val result = repository.importScripts(Uri.fromFile(file))

        assertTrue("Import should succeed: ${result.exceptionOrNull()}", result.isSuccess)

        val scripts = scriptDao.getAllScriptsOneShot()
        assertEquals(1, scripts.size)
        assertEquals("V2Script", scripts[0].name)
        assertEquals(mapOf("USER" to "root"), scripts[0].envVars)

        file.delete()
    }

    @Test
    fun importV2_format_withExplicitVersionField() = runTest {
        val v2Json = """
            {
                "version": 2,
                "scripts": [
                    {
                        "id": 5,
                        "name": "VersionedV2",
                        "code": "pwd",
                        "interpreter": "sh",
                        "envVars": {}
                    }
                ]
            }
        """.trimIndent()

        val file = writeTempJson(v2Json)
        repository.importScripts(Uri.fromFile(file))

        val script = scriptDao.getAllScriptsOneShot().first()
        assertEquals("VersionedV2", script.name)
        assertEquals(listOf("pwd"), script.codePages)

        file.delete()
    }

    // --- V3 Format Tests (with categories and automations, no themes) ---

    @Test
    fun importV3_format_withCategoriesScriptsAndAutomations() = runTest {
        val v3Json = """
            {
                "version": 3,
                "categories": [
                    {"id": 100, "name": "Networking", "orderIndex": 0},
                    {"id": 200, "name": "System", "orderIndex": 1}
                ],
                "scripts": [
                    {
                        "id": 10,
                        "name": "PingTest",
                        "codePages": ["ping -c 3 google.com"],
                        "interpreter": "bash",
                        "fileExtension": "sh",
                        "commandPrefix": "",
                        "runInBackground": false,
                        "openNewSession": true,
                        "executionParams": "",
                        "envVars": {},
                        "keepSessionOpen": false,
                        "categoryId": 100
                    },
                    {
                        "id": 20,
                        "name": "DiskUsage",
                        "codePages": ["df -h"],
                        "interpreter": "bash",
                        "fileExtension": "sh",
                        "commandPrefix": "",
                        "runInBackground": false,
                        "openNewSession": true,
                        "executionParams": "",
                        "envVars": {},
                        "keepSessionOpen": false,
                        "categoryId": 200
                    }
                ],
                "automations": [
                    {
                        "scriptId": 10,
                        "type": "PERIODIC",
                        "scheduledTimestamp": 1700000000000,
                        "intervalMillis": 3600000,
                        "daysOfWeek": [],
                        "isEnabled": true,
                        "runtimeArgs": null,
                        "runtimeEnv": null,
                        "runtimePrefix": null,
                        "label": "Hourly Ping",
                        "runIfMissed": false,
                        "lastExitCode": 0,
                        "requireWifi": true,
                        "requireCharging": false,
                        "batteryThreshold": 0
                    }
                ]
            }
        """.trimIndent()

        val file = writeTempJson(v3Json)
        repository.importScripts(Uri.fromFile(file))

        val categories = categoryDao.getAllCategoriesOneShot()
        assertEquals(2, categories.size)
        assertEquals(setOf("Networking", "System"), categories.map { it.name }.toSet())

        val scripts = scriptDao.getAllScriptsOneShot()
        assertEquals(2, scripts.size)
        assertTrue(scripts.all { it.categoryId != null })

        val pingScript = scripts.find { it.name == "PingTest" }!!
        val netCategory = categories.find { it.name == "Networking" }!!
        assertEquals(netCategory.id, pingScript.categoryId)

        val automations = automationDao.getAllAutomationsOneShot()
        assertEquals(1, automations.size)
        assertEquals("Hourly Ping", automations[0].label)
        assertEquals(pingScript.id, automations[0].scriptId)
        assertFalse(automations[0].isEnabled)

        file.delete()
    }

    @Test
    fun importV3_format_skipsThemesFieldSinceAbsent() = runTest {
        val v3Json = """
            {
                "version": 3,
                "categories": [],
                "scripts": [
                    {
                        "id": 1,
                        "name": "V3NoThemes",
                        "code": "echo ok",
                        "interpreter": "bash",
                        "envVars": {}
                    }
                ],
                "automations": []
            }
        """.trimIndent()

        val file = writeTempJson(v3Json)
        repository.importScripts(Uri.fromFile(file))

        assertEquals(0, customThemeDao.getAllThemesOneShot().size)
        assertEquals(1, scriptDao.getAllScriptsOneShot().size)

        file.delete()
    }

    // --- V4 Format Tests (full object with all fields except themes) ---

    @Test
    fun importV4_format_withAdvancedScriptFields() = runTest {
        val v4Json = """
            {
                "version": 4,
                "categories": [
                    {"id": 1, "name": "DevTools", "orderIndex": 0}
                ],
                "scripts": [
                    {
                        "id": 50,
                        "name": "ComplexScript",
                        "codePages": ["#!/bin/bash", "set -e", "echo deploy"],
                        "pageNames": ["Setup", "Config", "Deploy"],
                        "interpreter": "bash",
                        "fileExtension": "sh",
                        "commandPrefix": "tsu",
                        "runInBackground": true,
                        "openNewSession": false,
                        "executionParams": "--verbose",
                        "envVars": {"DEPLOY_ENV": "staging"},
                        "keepSessionOpen": true,
                        "useHeartbeat": true,
                        "heartbeatTimeout": 60000,
                        "heartbeatInterval": 5000,
                        "orderIndex": 10,
                        "notifyOnResult": true,
                        "interactionMode": "MULTI_CHOICE",
                        "argumentPresets": ["--prod", "--staging"],
                        "prefixPresets": ["sudo", "nice -n 19"],
                        "envVarPresets": ["ENV=PROD", "ENV=STAGING"],
                        "adbCode": "deploy-001",
                        "categoryId": 1
                    }
                ],
                "automations": []
            }
        """.trimIndent()

        val file = writeTempJson(v4Json)
        repository.importScripts(Uri.fromFile(file))

        val script = scriptDao.getAllScriptsOneShot().first()
        assertEquals("ComplexScript", script.name)
        assertEquals(listOf("#!/bin/bash", "set -e", "echo deploy"), script.codePages)
        assertEquals(listOf("Setup", "Config", "Deploy"), script.pageNames)
        assertEquals("tsu", script.commandPrefix)
        assertTrue(script.runInBackground)
        assertFalse(script.openNewSession)
        assertEquals("--verbose", script.executionParams)
        assertEquals(mapOf("DEPLOY_ENV" to "staging"), script.envVars)
        assertTrue(script.keepSessionOpen)
        assertTrue(script.useHeartbeat)
        assertEquals(60000L, script.heartbeatTimeout)
        assertEquals(5000L, script.heartbeatInterval)
        assertEquals(10, script.orderIndex)
        assertTrue(script.notifyOnResult)
        assertEquals(InteractionMode.MULTI_CHOICE, script.interactionMode)
        assertEquals(listOf("--prod", "--staging"), script.argumentPresets)
        assertEquals(listOf("sudo", "nice -n 19"), script.prefixPresets)
        assertEquals(listOf("ENV=PROD", "ENV=STAGING"), script.envVarPresets)
        assertEquals("deploy-001", script.adbCode)

        file.delete()
    }

    // --- V5 Format Tests (current version with themes) ---

    @Test
    fun importV5_format_withCustomThemes() = runTest {
        val v5Json = """
            {
                "version": 5,
                "categories": [
                    {"id": 1, "name": "Utilities", "orderIndex": 0}
                ],
                "scripts": [
                    {
                        "id": 1,
                        "name": "HelloWorld",
                        "codePages": ["echo hello"],
                        "interpreter": "bash",
                        "fileExtension": "sh",
                        "commandPrefix": "",
                        "runInBackground": false,
                        "openNewSession": true,
                        "executionParams": "",
                        "envVars": {},
                        "keepSessionOpen": false,
                        "categoryId": 1
                    }
                ],
                "automations": [
                    {
                        "scriptId": 1,
                        "type": "BOOT",
                        "scheduledTimestamp": 0,
                        "intervalMillis": 0,
                        "daysOfWeek": [],
                        "isEnabled": false,
                        "runtimeArgs": null,
                        "runtimeEnv": null,
                        "runtimePrefix": null,
                        "label": "Boot Script",
                        "runIfMissed": true,
                        "lastExitCode": null,
                        "requireWifi": false,
                        "requireCharging": false,
                        "batteryThreshold": 0,
                        "lastRunTimestamp": 1700000000000
                    }
                ],
                "themes": [
                    {
                        "name": "Ocean Dark",
                        "isDark": true,
                        "primary": -926580365,
                        "onPrimary": -1,
                        "primaryContainer": -1047711705,
                        "onPrimaryContainer": -1,
                        "secondary": -922169526,
                        "onSecondary": -1,
                        "secondaryContainer": -1038174169,
                        "onSecondaryContainer": -1,
                        "tertiary": -847425911,
                        "onTertiary": -1,
                        "tertiaryContainer": -1008494553,
                        "onTertiaryContainer": -1,
                        "error": -1174576641,
                        "onError": -1,
                        "errorContainer": -1219159067,
                        "onErrorContainer": -1,
                        "background": -1480348416,
                        "onBackground": -1,
                        "surface": -1480348416,
                        "onSurface": -1,
                        "surfaceVariant": -1125971015,
                        "onSurfaceVariant": -1,
                        "outline": -766267383,
                        "outlineVariant": -1248523345,
                        "surfaceContainer": -1380658930,
                        "surfaceContainerLowest": -1480348416
                    }
                ]
            }
        """.trimIndent()

        val file = writeTempJson(v5Json)
        repository.importScripts(Uri.fromFile(file))

        assertEquals(1, scriptDao.getAllScriptsOneShot().size)
        assertEquals(1, categoryDao.getAllCategoriesOneShot().size)
        assertEquals(1, automationDao.getAllAutomationsOneShot().size)

        val theme = customThemeDao.getAllThemesOneShot().first()
        assertEquals("Ocean Dark", theme.name)
        assertTrue(theme.isDark)
        assertEquals(-926580365L, theme.primary)
        assertEquals(-1480348416L, theme.background)

        val automation = automationDao.getAllAutomationsOneShot().first()
        assertEquals(AutomationType.BOOT, automation.type)
        assertFalse(automation.isEnabled)
        assertEquals(1700000000000L, automation.lastRunTimestamp)

        file.delete()
    }

    // --- Roundtrip Export/Import Tests ---

    @Test
    fun roundtrip_exportAndImport_preservesAllData() = runTest {
        val catId1 = categoryDao.insertCategory(CategoryEntity(name = "Network", orderIndex = 0)).toInt()
        val catId2 = categoryDao.insertCategory(CategoryEntity(name = "Storage", orderIndex = 1)).toInt()

        val scriptId1 = scriptDao.insertScript(
            ScriptEntity(
                name = "PingHosts",
                codePages = listOf("ping -c 1 \$1"),
                pageNames = listOf("Main"),
                interpreter = "bash",
                fileExtension = "sh",
                commandPrefix = "",
                runInBackground = false,
                openNewSession = true,
                executionParams = "",
                envVars = mapOf("PATH" to "/usr/bin:/bin"),
                keepSessionOpen = false,
                useHeartbeat = false,
                categoryId = catId1,
                orderIndex = 0,
                iconPath = null,
            ),
        ).toInt()

        val scriptId2 = scriptDao.insertScript(
            ScriptEntity(
                name = "BackupData",
                codePages = listOf("tar czf backup.tar.gz /data"),
                pageNames = listOf("Main"),
                interpreter = "bash",
                fileExtension = "sh",
                commandPrefix = "nice -n 19",
                runInBackground = true,
                openNewSession = false,
                executionParams = "",
                envVars = emptyMap(),
                keepSessionOpen = false,
                useHeartbeat = true,
                heartbeatTimeout = 120000,
                heartbeatInterval = 10000,
                categoryId = catId2,
                orderIndex = 1,
                iconPath = null,
            ),
        ).toInt()

        automationDao.insertAutomation(
            AutomationEntity(
                scriptId = scriptId1,
                label = "Daily Ping",
                type = AutomationType.PERIODIC,
                scheduledTimestamp = System.currentTimeMillis(),
                intervalMillis = 86400000,
                daysOfWeek = listOf(1, 3, 5),
                isEnabled = true,
                requireWifi = true,
            ),
        )

        customThemeDao.insertTheme(
            CustomThemeEntity(
                name = "Test Theme",
                isDark = false,
                primary = -16777216L,
                onPrimary = -1L,
                primaryContainer = 0xFFBB86FC,
                onPrimaryContainer = 0L,
                secondary = 0xFF03DAC6,
                onSecondary = -1L,
                secondaryContainer = 0xFFE4C6A5,
                onSecondaryContainer = 0L,
                tertiary = 0xFFFF7879,
                onTertiary = -1L,
                tertiaryContainer = 0xFF3D4B52,
                onTertiaryContainer = -1L,
                error = 0xFFB00020,
                onError = -1L,
                errorContainer = 0xFF370013,
                onErrorContainer = -1L,
                background = -1L,
                onBackground = 0L,
                surface = -1L,
                onSurface = 0L,
                surfaceVariant = 0xFFE7E0EC,
                onSurfaceVariant = 0xFF49454F,
                outline = 0xFF79747E,
                outlineVariant = 0xFFCAC4D0,
                surfaceContainer = 0xFFE6E1E5,
                surfaceContainerLowest = -1L,
            ),
        )

        val exportFile = File(context.cacheDir, "roundtrip_export.json")
        repository.exportScripts(Uri.fromFile(exportFile))

        val exportedJson = exportFile.readText()
        val backupDto = json.decodeFromString<FullBackupDto>(exportedJson)
        assertEquals(5, backupDto.version)
        assertEquals(2, backupDto.scripts.size)
        assertEquals(2, backupDto.categories.size)
        assertEquals(1, backupDto.automations.size)
        assertEquals(1, backupDto.themes.size)

        db.close()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        scriptDao = db.scriptDao()
        categoryDao = db.categoryDao()
        automationDao = db.automationDao()
        customThemeDao = db.customThemeDao()
        repository = ScriptRepositoryImpl(scriptDao, categoryDao, automationDao, customThemeDao, db, context)

        val result = repository.importScripts(Uri.fromFile(exportFile))
        assertTrue("Import should succeed: ${result.exceptionOrNull()}", result.isSuccess)

        val importedScripts = scriptDao.getAllScriptsOneShot()
        assertEquals(2, importedScripts.size)

        val pingScript = importedScripts.find { it.name == "PingHosts" }!!
        assertEquals(listOf("ping -c 1 \$1"), pingScript.codePages)
        assertEquals(mapOf("PATH" to "/usr/bin:/bin"), pingScript.envVars)

        val backupScript = importedScripts.find { it.name == "BackupData" }!!
        assertEquals("nice -n 19", backupScript.commandPrefix)
        assertTrue(backupScript.useHeartbeat)
        assertEquals(120000L, backupScript.heartbeatTimeout)

        val categories = categoryDao.getAllCategoriesOneShot()
        assertEquals(setOf("Network", "Storage"), categories.map { it.name }.toSet())

        val automations = automationDao.getAllAutomationsOneShot()
        assertEquals(1, automations.size)
        assertEquals("Daily Ping", automations[0].label)
        assertFalse(automations[0].isEnabled)
        assertEquals(pingScript.id, automations[0].scriptId)

        val themes = customThemeDao.getAllThemesOneShot()
        assertEquals(1, themes.size)
        assertEquals("Test Theme", themes[0].name)

        exportFile.delete()
    }

    // --- Category Deduplication Tests ---

    @Test
    fun import_deduplicatesCategoriesByName() = runTest {
        val existingCatId = categoryDao.insertCategory(CategoryEntity(name = "Existing", orderIndex = 5)).toInt()

        scriptDao.insertScript(
            ScriptEntity(
                name = "PreExisting",
                codePages = listOf("echo pre"),
                interpreter = "bash",
                runInBackground = false,
                openNewSession = true,
                executionParams = "",
                envVars = emptyMap(),
                keepSessionOpen = false,
                categoryId = existingCatId,
                iconPath = null,
            ),
        )

        val jsonWithExistingCategory = """
            {
                "version": 5,
                "categories": [
                    {"id": 999, "name": "Existing", "orderIndex": 0},
                    {"id": 888, "name": "NewCat", "orderIndex": 1}
                ],
                "scripts": [
                    {
                        "id": 1,
                        "name": "ImportedScript",
                        "codePages": ["echo imported"],
                        "interpreter": "bash",
                        "runInBackground": false,
                        "openNewSession": true,
                        "executionParams": "",
                        "envVars": {},
                        "keepSessionOpen": false,
                        "categoryId": 999
                    }
                ],
                "automations": []
            }
        """.trimIndent()

        val file = writeTempJson(jsonWithExistingCategory)
        repository.importScripts(Uri.fromFile(file))

        val categories = categoryDao.getAllCategoriesOneShot()
        assertEquals(2, categories.size)
        val existingCat = categories.find { it.name == "Existing" }!!
        assertEquals(existingCatId, existingCat.id)

        val importedScript = scriptDao.getScriptById(
            scriptDao.getAllScriptsOneShot().find { it.name == "ImportedScript" }!!.id
        )!!
        assertEquals(existingCatId, importedScript.categoryId)

        file.delete()
    }

    // --- Automation Script ID Remapping Tests ---

    @Test
    fun import_remapsAutomationScriptIdsToNewlyInsertedScripts() = runTest {
        val jsonWithAutomations = """
            {
                "version": 5,
                "categories": [],
                "scripts": [
                    {
                        "id": 100,
                        "name": "AutoScript",
                        "codePages": ["echo auto"],
                        "interpreter": "bash",
                        "runInBackground": false,
                        "openNewSession": true,
                        "executionParams": "",
                        "envVars": {},
                        "keepSessionOpen": false
                    },
                    {
                        "id": 200,
                        "name": "OrphanAutoScript",
                        "codePages": ["echo orphan"],
                        "interpreter": "bash",
                        "runInBackground": false,
                        "openNewSession": true,
                        "executionParams": "",
                        "envVars": {},
                        "keepSessionOpen": false
                    }
                ],
                "automations": [
                    {
                        "scriptId": 100,
                        "type": "ONE_TIME",
                        "scheduledTimestamp": 1700000000000,
                        "intervalMillis": 0,
                        "daysOfWeek": [],
                        "isEnabled": true,
                        "runtimeArgs": null,
                        "runtimeEnv": null,
                        "runtimePrefix": null,
                        "label": "Linked Auto",
                        "runIfMissed": false,
                        "lastExitCode": 0,
                        "requireWifi": false,
                        "requireCharging": false,
                        "batteryThreshold": 0
                    },
                    {
                        "scriptId": 999,
                        "type": "ONE_TIME",
                        "scheduledTimestamp": 1700000000000,
                        "intervalMillis": 0,
                        "daysOfWeek": [],
                        "isEnabled": true,
                        "runtimeArgs": null,
                        "runtimeEnv": null,
                        "runtimePrefix": null,
                        "label": "Orphan Auto",
                        "runIfMissed": false,
                        "lastExitCode": 0,
                        "requireWifi": false,
                        "requireCharging": false,
                        "batteryThreshold": 0
                    }
                ]
            }
        """.trimIndent()

        val file = writeTempJson(jsonWithAutomations)
        repository.importScripts(Uri.fromFile(file))

        val scripts = scriptDao.getAllScriptsOneShot()
        assertEquals(2, scripts.size)

        val autoScript = scripts.find { it.name == "AutoScript" }!!
        val automations = automationDao.getAllAutomationsOneShot()
        assertEquals(1, automations.size)
        assertEquals(autoScript.id, automations[0].scriptId)
        assertEquals("Linked Auto", automations[0].label)

        file.delete()
    }

    // --- Theme Deduplication Tests ---

    @Test
    fun import_skipsThemesWithDuplicateNames() = runTest {
        customThemeDao.insertTheme(
            CustomThemeEntity(
                name = "ExistingTheme",
                isDark = true,
                primary = -1L, onPrimary = 0L,
                primaryContainer = 0L, onPrimaryContainer = -1L,
                secondary = -2L, onSecondary = 1L,
                secondaryContainer = 2L, onSecondaryContainer = 3L,
                tertiary = 4L, onTertiary = 5L,
                tertiaryContainer = 6L, onTertiaryContainer = 7L,
                error = 8L, onError = 9L,
                errorContainer = 10L, onErrorContainer = 11L,
                background = 12L, onBackground = 13L,
                surface = 14L, onSurface = 15L,
                surfaceVariant = 16L, onSurfaceVariant = 17L,
                outline = 18L, outlineVariant = 19L,
                surfaceContainer = 20L, surfaceContainerLowest = 21L,
            ),
        )

        val jsonWithThemes = """
            {
                "version": 5,
                "categories": [],
                "scripts": [
                    {"id": 1, "name": "S", "codePages": ["echo s"], "interpreter": "bash",
                     "runInBackground": false, "openNewSession": true, "executionParams": "",
                     "envVars": {}, "keepSessionOpen": false}
                ],
                "automations": [],
                "themes": [
                    {
                        "name": "ExistingTheme",
                        "isDark": false,
                        "primary": 100, "onPrimary": 200,
                        "primaryContainer": 300, "onPrimaryContainer": 400,
                        "secondary": 500, "onSecondary": 600,
                        "secondaryContainer": 700, "onSecondaryContainer": 800,
                        "tertiary": 900, "onTertiary": 1000,
                        "tertiaryContainer": 1100, "onTertiaryContainer": 1200,
                        "error": 1300, "onError": 1400,
                        "errorContainer": 1500, "onErrorContainer": 1600,
                        "background": 1700, "onBackground": 1800,
                        "surface": 1900, "onSurface": 2000,
                        "surfaceVariant": 2100, "onSurfaceVariant": 2200,
                        "outline": 2300, "outlineVariant": 2400,
                        "surfaceContainer": 2500, "surfaceContainerLowest": 2600
                    },
                    {
                        "name": "NewTheme",
                        "isDark": true,
                        "primary": -1, "onPrimary": -2,
                        "primaryContainer": -3, "onPrimaryContainer": -4,
                        "secondary": -5, "onSecondary": -6,
                        "secondaryContainer": -7, "onSecondaryContainer": -8,
                        "tertiary": -9, "onTertiary": -10,
                        "tertiaryContainer": -11, "onTertiaryContainer": -12,
                        "error": -13, "onError": -14,
                        "errorContainer": -15, "onErrorContainer": -16,
                        "background": -17, "onBackground": -18,
                        "surface": -19, "onSurface": -20,
                        "surfaceVariant": -21, "onSurfaceVariant": -22,
                        "outline": -23, "outlineVariant": -24,
                        "surfaceContainer": -25, "surfaceContainerLowest": -26
                    }
                ]
            }
        """.trimIndent()

        val file = writeTempJson(jsonWithThemes)
        repository.importScripts(Uri.fromFile(file))

        val themes = customThemeDao.getAllThemesOneShot()
        assertEquals(2, themes.size)

        val existing = themes.find { it.name == "ExistingTheme" }!!
        assertEquals(-1L, existing.primary)
        val newTheme = themes.find { it.name == "NewTheme" }!!
        assertEquals(-1L, newTheme.primary)

        file.delete()
    }

    // --- Icon Import Tests ---

    @Test
    fun import_savesBase64IconAndRoundtripsCorrectly() = runTest {
        val originalBytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D)
        val base64Icon = Base64.encodeToString(originalBytes, Base64.NO_WRAP)

        val jsonWithIcon = """
            {
                "version": 5,
                "categories": [],
                "scripts": [
                    {
                        "id": 1,
                        "name": "IconScript",
                        "codePages": ["echo icon"],
                        "interpreter": "bash",
                        "runInBackground": false,
                        "openNewSession": true,
                        "executionParams": "",
                        "envVars": {},
                        "keepSessionOpen": false,
                        "iconBase64": "$base64Icon"
                    }
                ],
                "automations": [],
                "themes": []
            }
        """.trimIndent()

        val file = writeTempJson(jsonWithIcon)
        repository.importScripts(Uri.fromFile(file))

        val script = scriptDao.getAllScriptsOneShot().first()
        assertNotNull("Icon path should not be null", script.iconPath)
        val iconFile = File(script.iconPath!!)
        assertTrue(iconFile.exists())
        assertEquals(originalBytes.toList(), iconFile.readBytes().toList())

        file.delete()
    }

    // --- Error Handling Tests ---

    @Test
    fun import_malformedJson_returnsFailure() = runTest {
        val file = writeTempJson("{ invalid json content !!")
        val result = repository.importScripts(Uri.fromFile(file))

        assertTrue("Import should fail for malformed JSON", result.isFailure)
        file.delete()
    }

    @Test
    fun import_emptyFile_returnsFailure() = runTest {
        val file = writeTempJson("")
        val result = repository.importScripts(Uri.fromFile(file))

        assertTrue("Import should fail for empty file", result.isFailure)
        file.delete()
    }

    // --- Export Format Validation Tests ---

    @Test
    fun export_producesValidV5JsonWithAllTopLevelFields() = runTest {
        scriptDao.insertScript(
            ScriptEntity(
                name = "ExportTest",
                codePages = listOf("echo test"),
                interpreter = "bash",
                fileExtension = "sh",
                commandPrefix = "",
                runInBackground = false,
                openNewSession = true,
                executionParams = "",
                envVars = emptyMap(),
                keepSessionOpen = false,
                iconPath = null,
            ),
        )

        val exportFile = File(context.cacheDir, "format_check.json")
        repository.exportScripts(Uri.fromFile(exportFile))

        val content = exportFile.readText()
        assertTrue(content.contains("\"version\": 5"))
        assertTrue(content.contains("\"scripts\""))
        assertTrue(content.contains("\"categories\""))
        assertTrue(content.contains("\"automations\""))
        assertTrue(content.contains("\"themes\""))

        val dto = json.decodeFromString<FullBackupDto>(content)
        assertEquals(5, dto.version)
        assertEquals(1, dto.scripts.size)

        exportFile.delete()
    }

    @Test
    fun export_withEmptyDatabase_producesValidMinimalBackup() = runTest {
        val exportFile = File(context.cacheDir, "empty_export.json")
        repository.exportScripts(Uri.fromFile(exportFile))

        val content = exportFile.readText()
        val dto = json.decodeFromString<FullBackupDto>(content)
        assertEquals(5, dto.version)
        assertTrue(dto.scripts.isEmpty())
        assertTrue(dto.categories.isEmpty())
        assertTrue(dto.automations.isEmpty())
        assertTrue(dto.themes.isEmpty())

        exportFile.delete()
    }

    // --- Complex Automation Types Tests ---

    @Test
    fun import_handlesAllAutomationTypes() = runTest {
        val jsonWithAllTypes = """
            {
                "version": 5,
                "categories": [],
                "scripts": [
                    {"id": 1, "name": "S1", "codePages": ["echo s1"], "interpreter": "bash",
                     "runInBackground": false, "openNewSession": true, "executionParams": "",
                     "envVars": {}, "keepSessionOpen": false},
                    {"id": 2, "name": "S2", "codePages": ["echo s2"], "interpreter": "bash",
                     "runInBackground": false, "openNewSession": true, "executionParams": "",
                     "envVars": {}, "keepSessionOpen": false}
                ],
                "automations": [
                    {
                        "scriptId": 1,
                        "type": "ONE_TIME",
                        "scheduledTimestamp": 1700000000000,
                        "intervalMillis": 0,
                        "daysOfWeek": [],
                        "isEnabled": false,
                        "runtimeArgs": null,
                        "runtimeEnv": null,
                        "runtimePrefix": null,
                        "label": "OneTime",
                        "runIfMissed": false,
                        "lastExitCode": 0,
                        "requireWifi": false,
                        "requireCharging": false,
                        "batteryThreshold": 0
                    },
                    {
                        "scriptId": 1,
                        "type": "PERIODIC",
                        "scheduledTimestamp": 1700000000000,
                        "intervalMillis": 3600000,
                        "daysOfWeek": [],
                        "isEnabled": false,
                        "runtimeArgs": null,
                        "runtimeEnv": null,
                        "runtimePrefix": null,
                        "label": "Periodic",
                        "runIfMissed": true,
                        "lastExitCode": 0,
                        "requireWifi": true,
                        "requireCharging": false,
                        "batteryThreshold": 20
                    },
                    {
                        "scriptId": 2,
                        "type": "WEEKLY",
                        "scheduledTimestamp": 1700000000000,
                        "intervalMillis": 0,
                        "daysOfWeek": [1, 3, 5],
                        "isEnabled": false,
                        "runtimeArgs": "--force",
                        "runtimeEnv": {"DEBUG": "1"},
                        "runtimePrefix": "nice -n 19",
                        "label": "WeeklyMonWedFri",
                        "runIfMissed": true,
                        "lastExitCode": null,
                        "requireWifi": false,
                        "requireCharging": true,
                        "batteryThreshold": 50
                    },
                    {
                        "scriptId": 2,
                        "type": "BOOT",
                        "scheduledTimestamp": 0,
                        "intervalMillis": 0,
                        "daysOfWeek": [],
                        "isEnabled": false,
                        "runtimeArgs": null,
                        "runtimeEnv": null,
                        "runtimePrefix": null,
                        "label": "OnBoot",
                        "runIfMissed": true,
                        "lastExitCode": 0,
                        "requireWifi": false,
                        "requireCharging": false,
                        "batteryThreshold": 0
                    }
                ],
                "themes": []
            }
        """.trimIndent()

        val file = writeTempJson(jsonWithAllTypes)
        repository.importScripts(Uri.fromFile(file))

        val automations = automationDao.getAllAutomationsOneShot()
        assertEquals(4, automations.size)

        val oneTime = automations.find { it.label == "OneTime" }!!
        assertEquals(AutomationType.ONE_TIME, oneTime.type)

        val periodic = automations.find { it.label == "Periodic" }!!
        assertEquals(AutomationType.PERIODIC, periodic.type)
        assertTrue(periodic.requireWifi)
        assertEquals(20, periodic.batteryThreshold)

        val weekly = automations.find { it.label == "WeeklyMonWedFri" }!!
        assertEquals(AutomationType.WEEKLY, weekly.type)
        assertEquals(listOf(1, 3, 5), weekly.daysOfWeek)
        assertTrue(weekly.requireCharging)
        assertEquals("--force", weekly.runtimeArgs)

        val boot = automations.find { it.label == "OnBoot" }!!
        assertEquals(AutomationType.BOOT, boot.type)

        file.delete()
    }

    // --- Code Pages and Page Names Tests ---

    @Test
    fun import_preservesCodePagesAndPageNames() = runTest {
        val jsonWithPages = """
            {
                "version": 5,
                "categories": [],
                "scripts": [
                    {
                        "id": 1,
                        "name": "MultiPage",
                        "codePages": ["#!/bin/bash", "echo page2", "echo page3"],
                        "pageNames": ["Init", "Step 2", "Final"],
                        "interpreter": "bash",
                        "runInBackground": false,
                        "openNewSession": true,
                        "executionParams": "",
                        "envVars": {},
                        "keepSessionOpen": false
                    }
                ],
                "automations": [],
                "themes": []
            }
        """.trimIndent()

        val file = writeTempJson(jsonWithPages)
        repository.importScripts(Uri.fromFile(file))

        val script = scriptDao.getAllScriptsOneShot().first()
        assertEquals(3, script.codePages.size)
        assertEquals(listOf("Init", "Step 2", "Final"), script.pageNames)

        file.delete()
    }

    @Test
    fun import_generatesPageNamesWhenNotProvided() = runTest {
        val jsonWithoutPageNames = """
            {
                "version": 5,
                "categories": [],
                "scripts": [
                    {
                        "id": 1,
                        "name": "NoPages",
                        "codePages": ["echo a", "echo b", "echo c"],
                        "interpreter": "bash",
                        "runInBackground": false,
                        "openNewSession": true,
                        "executionParams": "",
                        "envVars": {},
                        "keepSessionOpen": false
                    }
                ],
                "automations": [],
                "themes": []
            }
        """.trimIndent()

        val file = writeTempJson(jsonWithoutPageNames)
        repository.importScripts(Uri.fromFile(file))

        val script = scriptDao.getAllScriptsOneShot().first()
        assertEquals(listOf("Main", "Page 2", "Page 3"), script.pageNames)

        file.delete()
    }
}
