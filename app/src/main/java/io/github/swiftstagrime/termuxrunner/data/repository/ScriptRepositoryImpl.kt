package io.github.swiftstagrime.termuxrunner.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.swiftstagrime.termuxrunner.data.local.dao.AutomationDao
import io.github.swiftstagrime.termuxrunner.data.local.dao.CategoryDao
import io.github.swiftstagrime.termuxrunner.data.local.dao.ScriptDao
import io.github.swiftstagrime.termuxrunner.data.local.dto.CategoryExportDto
import io.github.swiftstagrime.termuxrunner.data.local.dto.FullBackupDto
import io.github.swiftstagrime.termuxrunner.data.local.dto.ScriptExportDto
import io.github.swiftstagrime.termuxrunner.data.local.dto.toExportDto
import io.github.swiftstagrime.termuxrunner.data.local.entity.AutomationEntity
import io.github.swiftstagrime.termuxrunner.data.local.entity.CategoryEntity
import io.github.swiftstagrime.termuxrunner.data.local.entity.ScriptEntity
import io.github.swiftstagrime.termuxrunner.data.local.entity.toAutomationDomain
import io.github.swiftstagrime.termuxrunner.data.local.entity.toScriptEntity
import io.github.swiftstagrime.termuxrunner.domain.model.Script
import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.decodeFromJsonElement
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.util.UUID
import javax.inject.Inject

sealed class ScriptException : Exception()

class ExportStreamException : ScriptException()

class ImportStreamException : ScriptException()

/**
 * Repository implementation for managing scripts, handling database operations
 * and script portability (import/export).
 */
class ScriptRepositoryImpl @Inject constructor(
    private val dao: ScriptDao,
    private val categoryDao: CategoryDao,
    private val automationDao: AutomationDao,
    @ApplicationContext private val context: Context,
) : ScriptRepository {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
    }

    override fun getAllScripts(): Flow<List<Script>> =
        dao.getAllScripts().map { entities ->
            entities.map { it.toScriptDomain() }
        }

    override suspend fun getScriptById(id: Int): Script? =
        dao.getScriptById(id)?.toScriptDomain()

    override suspend fun insertScript(script: Script): Int =
        dao.insertScript(script.toScriptEntity()).toInt()

    override suspend fun deleteScript(script: Script) {
        dao.deleteScript(script.toScriptEntity())
    }

    override suspend fun updateScriptsOrder(orders: List<Pair<Int, Int>>) {
        dao.updateScriptsOrder(orders)
    }

    override suspend fun exportScripts(uri: Uri): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            val categories = categoryDao.getAllCategoriesOneShot().map {
                CategoryExportDto(it.id, it.name, it.orderIndex)
            }

            val scripts = dao.getAllScriptsOneShot().map { entity ->
                val script = entity.toScriptDomain()
                var base64Icon: String? = null
                script.iconPath?.let { path ->
                    val file = File(path)
                    if (file.exists()) {
                        val bytes = file.readBytes()
                        base64Icon = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    }
                }
                script.toExportDto(base64Icon).copy(id = entity.id)
            }

            val automations = automationDao.getAllAutomationsOneShot().map {
                it.toAutomationDomain().toExportDto()
            }

            val backup = FullBackupDto(
                version = 3,
                categories = categories,
                scripts = scripts,
                automations = automations,
            )

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(json.encodeToString(backup).toByteArray())
            } ?: throw ExportStreamException()
        }
    }

    override suspend fun importScripts(uri: Uri): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw ImportStreamException()

            val backup = inputStream.use { stream ->
                val jsonString = stream.bufferedReader().readText()
                val jsonElement = json.parseToJsonElement(jsonString)

                if (jsonElement is JsonArray) {
                    FullBackupDto(
                        categories = emptyList(),
                        scripts = json.decodeFromJsonElement<List<ScriptExportDto>>(jsonElement),
                        automations = emptyList(),
                    )
                } else {
                    json.decodeFromJsonElement<FullBackupDto>(jsonElement)
                }
            }

            val existingCategoryMap = categoryDao.getAllCategoriesOneShot().associate { it.name to it.id }
            val categoryIdMap = mutableMapOf<Int, Int?>()

            backup.categories.forEach { dto ->
                val existingId = existingCategoryMap[dto.name]
                if (existingId != null) {
                    categoryIdMap[dto.id] = existingId
                } else {
                    val newId = categoryDao.insertCategory(
                        CategoryEntity(
                            name = dto.name,
                            orderIndex = dto.orderIndex,
                        ),
                    )
                    categoryIdMap[dto.id] = newId.toInt()
                }
            }

            val scriptIdMap = mutableMapOf<Int, Int>()
            backup.scripts.forEach { dto ->
                val newIconPath = saveBase64Icon(dto.iconBase64)
                val entity = ScriptEntity(
                    name = dto.name,
                    code = dto.code,
                    interactionMode = dto.interactionMode,
                    interpreter = dto.interpreter,
                    fileExtension = dto.fileExtension,
                    commandPrefix = dto.commandPrefix,
                    runInBackground = dto.runInBackground,
                    openNewSession = dto.openNewSession,
                    executionParams = dto.executionParams,
                    envVars = dto.envVars,
                    keepSessionOpen = dto.keepSessionOpen,
                    useHeartbeat = dto.useHeartbeat,
                    heartbeatTimeout = dto.heartbeatTimeout,
                    heartbeatInterval = dto.heartbeatInterval,
                    iconPath = newIconPath,
                    orderIndex = dto.orderIndex,
                    notifyOnResult = dto.notifyOnResult,
                    categoryId = if (dto.categoryId != null) categoryIdMap[dto.categoryId] else null,
                )
                val newId = dao.insertScript(entity)
                scriptIdMap[dto.id] = newId.toInt()
            }

            val automationEntities = backup.automations.mapNotNull { dto ->
                val newScriptId = scriptIdMap[dto.scriptId] ?: return@mapNotNull null

                AutomationEntity(
                    scriptId = newScriptId,
                    label = dto.label,
                    type = dto.type,
                    scheduledTimestamp = dto.scheduledTimestamp,
                    intervalMillis = dto.intervalMillis,
                    daysOfWeek = dto.daysOfWeek,
                    isEnabled = false,
                    runIfMissed = dto.runIfMissed,
                    lastExitCode = dto.lastExitCode,
                    runtimeArgs = dto.runtimeArgs,
                    runtimeEnv = dto.runtimeEnv ?: emptyMap(),
                    runtimePrefix = dto.runtimePrefix,
                    requireWifi = dto.requireWifi,
                    requireCharging = dto.requireCharging,
                    batteryThreshold = dto.batteryThreshold,
                    lastRunTimestamp = null,
                    nextRunTimestamp = null,
                )
            }

            automationEntities.forEach { automationDao.insertAutomation(it) }
        }
    }

    override suspend fun importSingleScript(uri: Uri): Result<Script> = runCatching {
        withContext(Dispatchers.IO) {
            val content = context.contentResolver.openInputStream(uri)?.use {
                BufferedReader(InputStreamReader(it)).readText()
            } ?: throw ImportStreamException()

            val fileName = getFileName(uri) ?: "Imported Script"
            val extension = fileName.substringAfterLast('.', "sh")

            // Logic for interpreter and shebang
            val (finalCode, detectedInterpreter) = processScriptContent(content, extension)

            val newScript = Script(
                name = fileName.substringBeforeLast('.'),
                code = finalCode,
                interpreter = detectedInterpreter,
                fileExtension = extension,
                runInBackground = false,
                openNewSession = false,
                keepSessionOpen = false,
            )

            // We return the script object so the UI can decide
            // whether to open it in the editor first or save it directly.
            newScript
        }
    }

    private fun processScriptContent(content: String, extension: String): Pair<String, String> {
        val interpreter = INTERPRETER_MAP[extension.lowercase()] ?: "bash"

        if (content.trimStart().startsWith("#!")) {
            return content to interpreter
        }

        val shebang = if (interpreter in NO_SHEBANG_INTERPRETERS) {
            ""
        } else {
            val baseDataDir = context.filesDir.absolutePath.substringBefore(context.packageName)
            "#!${baseDataDir}com.termux/files/usr/bin/$interpreter\n"
        }

        return (shebang + content) to interpreter
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst()) name = cursor.getString(nameIndex)
        }
        return name
    }

    private fun saveBase64Icon(base64: String?): String? {
        if (base64 == null) return null
        return try {
            val imageBytes = Base64.decode(base64, Base64.NO_WRAP)
            val directory = File(context.filesDir, "script_icons").apply {
                if (!exists()) mkdirs()
            }
            val file = File(directory, "icon_${UUID.randomUUID()}.webp")
            FileOutputStream(file).use { it.write(imageBytes) }
            file.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private val INTERPRETER_MAP = mapOf(
            "py" to "python", "py3" to "python",
            "js" to "node", "cjs" to "node", "mjs" to "node",
            "rb" to "ruby", "pl" to "perl", "php" to "php",
            "lua" to "lua", "sh" to "bash", "zsh" to "zsh",
            "fish" to "fish", "awk" to "awk", "sed" to "sed",
            "exp" to "expect", "go" to "go", "ts" to "ts-node",
            "c" to "clang", "cpp" to "clang++", "cc" to "clang++", "cxx" to "clang++",
            "rs" to "rustc", "java" to "java", "kt" to "kotlinc", "kts" to "kotlinc",
            "cs" to "csc", "swift" to "swift"
        )

        private val NO_SHEBANG_INTERPRETERS = setOf(
            "clang", "clang++", "rustc", "kotlinc", "java"
        )
    }
}
