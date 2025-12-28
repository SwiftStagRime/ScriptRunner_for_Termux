package io.github.swiftstagrime.termuxrunner.data.repository

import android.content.Context
import android.net.Uri
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.swiftstagrime.termuxrunner.data.local.ScriptDao
import io.github.swiftstagrime.termuxrunner.data.local.ScriptEntity
import io.github.swiftstagrime.termuxrunner.data.local.ScriptExportDto
import io.github.swiftstagrime.termuxrunner.data.local.toEntity
import io.github.swiftstagrime.termuxrunner.data.local.toExportDto
import io.github.swiftstagrime.termuxrunner.domain.model.Script
import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.util.UUID
import javax.inject.Inject
/**
 * Repository implementation for managing scripts, handling database operations
 * and script portability (import/export).
 */
class ScriptRepositoryImpl @Inject constructor(
    private val dao: ScriptDao,
    @ApplicationContext private val context: Context
) : ScriptRepository {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override suspend fun exportScripts(uri: Uri): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            val entities = dao.getAllScriptsOneShot()

            val exportDtos = entities.map { entity ->
                val script = entity.toDomain()
                var base64Icon: String? = null

                // Convert local icon file to Base64 string for JSON portability
                if (script.iconPath != null) {
                    val file = File(script.iconPath)
                    if (file.exists()) {
                        val bytes = file.readBytes()
                        base64Icon = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    }
                }

                script.toExportDto(base64Icon)
            }

            val jsonString = json.encodeToString(exportDtos)

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jsonString.toByteArray())
            } ?: throw IllegalStateException("Could not open output stream")
        }
    }

    override suspend fun importScripts(uri: Uri): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).readText()
            } ?: throw IllegalStateException("Could not open input stream")

            val exportDtos = json.decodeFromString<List<ScriptExportDto>>(jsonString)

            val newEntities = exportDtos.map { dto ->
                var newIconPath: String? = null
                // Reconstruct icon file from Base64 if present in the import data
                if (dto.iconBase64 != null) {
                    try {
                        val imageBytes = Base64.decode(dto.iconBase64, Base64.NO_WRAP)

                        val directory = File(context.filesDir, "script_icons")
                        if (!directory.exists()) directory.mkdirs()

                        val fileName = "icon_${UUID.randomUUID()}.webp"
                        val destFile = File(directory, fileName)

                        FileOutputStream(destFile).use { out ->
                            out.write(imageBytes)
                        }

                        newIconPath = destFile.absolutePath
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                ScriptEntity(
                    id = 0, // Ensure Room treats this as a new entry. Thus we are not overriding current scripts, note that this will lead possible to duplicates
                    name = dto.name,
                    code = dto.code,
                    interpreter = dto.interpreter,
                    fileExtension = dto.fileExtension,
                    commandPrefix = dto.commandPrefix,
                    runInBackground = dto.runInBackground,
                    openNewSession = dto.openNewSession,
                    executionParams = dto.executionParams,
                    keepSessionOpen = dto.keepSessionOpen,
                    envVars = dto.envVars,
                    iconPath = newIconPath
                )
            }

            dao.insertScripts(newEntities)
        }
    }

    override fun getAllScripts(): Flow<List<Script>> {
        return dao.getAllScripts().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getScriptById(id: Int): Script? {
        return dao.getScriptById(id)?.toDomain()
    }

    override suspend fun insertScript(script: Script) {
        dao.insertScript(script.toEntity())
    }

    override suspend fun deleteScript(script: Script) {
        dao.deleteScript(script.toEntity())
    }
}