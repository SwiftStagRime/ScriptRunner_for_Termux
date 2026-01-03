package io.github.swiftstagrime.termuxrunner.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.swiftstagrime.termuxrunner.data.local.dao.CategoryDao
import io.github.swiftstagrime.termuxrunner.data.local.dao.ScriptDao
import io.github.swiftstagrime.termuxrunner.data.local.dto.CategoryExportDto
import io.github.swiftstagrime.termuxrunner.data.local.dto.FullBackupDto
import io.github.swiftstagrime.termuxrunner.data.local.dto.ScriptExportDto
import io.github.swiftstagrime.termuxrunner.data.local.dto.toExportDto
import io.github.swiftstagrime.termuxrunner.data.local.entity.CategoryEntity
import io.github.swiftstagrime.termuxrunner.data.local.entity.ScriptEntity
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

sealed class ScriptException() : Exception()

class ExportStreamException : ScriptException(
)

class ImportStreamException : ScriptException(
)

/**
 * Repository implementation for managing scripts, handling database operations
 * and script portability (import/export).
 */
class ScriptRepositoryImpl @Inject constructor(
    private val dao: ScriptDao,
    private val categoryDao: CategoryDao,
    @ApplicationContext private val context: Context,
) : ScriptRepository {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
    }

    override suspend fun exportScripts(uri: Uri): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            val categories = dao.getAllScriptsOneShot().map {
                CategoryExportDto(it.id, it.name, it.orderIndex)
            }

            val scripts = dao.getAllScriptsOneShot().map { entity ->
                val script = entity.toScriptDomain()
                var base64Icon: String? = null
                if (script.iconPath != null) {
                    val file = File(script.iconPath)
                    if (file.exists()) {
                        val bytes = file.readBytes()
                        base64Icon = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    }
                }
                script.toExportDto(base64Icon)
            }

            val backup = FullBackupDto(categories = categories, scripts = scripts)
            val jsonString = json.encodeToString(backup)

            context.contentResolver.openOutputStream(uri)
                ?.use { it.write(jsonString.toByteArray()) }
                ?: throw ExportStreamException()
        }
    }

    override suspend fun importScripts(uri: Uri): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).readText()
            } ?: throw ImportStreamException()

            val jsonElement = json.parseToJsonElement(jsonString)

            val (categoriesDto, scriptsDto) = if (jsonElement is JsonArray) {
                // Old Format: Just a list of scripts
                null to json.decodeFromJsonElement<List<ScriptExportDto>>(jsonElement)
            } else {
                // New Format: FullBackupDto object
                val backup = json.decodeFromJsonElement<FullBackupDto>(jsonElement)
                backup.categories to backup.scripts
            }

            val categoryIdMap = mutableMapOf<Int, Int?>()

            categoriesDto?.forEach { dto ->
                val newId = categoryDao.insertCategory(
                    CategoryEntity(
                        name = dto.name,
                        orderIndex = dto.orderIndex
                    )
                )
                categoryIdMap[dto.id] = newId.toInt()
            }

            val newEntities = scriptsDto.map { dto ->
                val newIconPath = saveBase64Icon(dto.iconBase64)

                ScriptEntity(
                    name = dto.name,
                    code = dto.code,
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
                    categoryId = if (dto.categoryId != null) categoryIdMap[dto.categoryId] else null
                )
            }

            dao.insertScripts(newEntities)
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
                openNewSession = true,
                keepSessionOpen = true
            )

            // We return the script object so the UI can decide
            // whether to open it in the editor first or save it directly.
            newScript
        }
    }

    private fun processScriptContent(content: String, extension: String): Pair<String, String> {
        val ext = extension.lowercase()

        val interpreter = when (ext) {
            "py", "py3" -> "python"
            "js", "cjs", "mjs" -> "node"
            "rb" -> "ruby"
            "pl" -> "perl"
            "php" -> "php"
            "lua" -> "lua"
            "sh" -> "bash"
            "zsh" -> "zsh"
            "fish" -> "fish"
            "awk" -> "awk"
            "sed" -> "sed"
            "exp" -> "expect"
            "go" -> "go"
            "ts" -> "ts-node"
            "c" -> "clang"
            "cpp", "cc", "cxx" -> "clang++"
            "rs" -> "rustc"
            "java" -> "java"
            "kt", "kts" -> "kotlinc"
            "cs" -> "csc"
            "swift" -> "swift"
            else -> "bash"
        }

        val hasShebang = content.trimStart().startsWith("#!")

        return if (!hasShebang) {
            val baseDataDir = context.filesDir.absolutePath.substringBefore(context.packageName)
            val termuxBinPath = "${baseDataDir}com.termux/files/usr/bin/"

            val shebang = when (interpreter) {
                "clang", "clang++", "rustc", "kotlinc", "java" -> ""
                else -> "#!$termuxBinPath$interpreter\n"
            }

            (shebang + content) to interpreter
        } else {
            content to interpreter
        }
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
            val directory = File(context.filesDir, "script_icons").apply { if (!exists()) mkdirs() }
            val file = File(directory, "icon_${UUID.randomUUID()}.webp")
            FileOutputStream(file).use { it.write(imageBytes) }
            file.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    override fun getAllScripts(): Flow<List<Script>> {
        return dao.getAllScripts().map { entities ->
            entities.map { it.toScriptDomain() }
        }
    }

    override suspend fun getScriptById(id: Int): Script? {
        return dao.getScriptById(id)?.toScriptDomain()
    }

    override suspend fun insertScript(script: Script): Int {
        return dao.insertScript(script.toScriptEntity()).toInt()
    }

    override suspend fun deleteScript(script: Script) {
        dao.deleteScript(script.toScriptEntity())
    }

    override suspend fun updateScriptsOrder(orders: List<Pair<Int, Int>>) {
        dao.updateScriptsOrder(orders)
    }
}