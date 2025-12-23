package io.github.swiftstagrime.termuxrunner.data.repository

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.swiftstagrime.termuxrunner.R
import io.github.swiftstagrime.termuxrunner.data.local.ScriptDao
import io.github.swiftstagrime.termuxrunner.data.local.toEntity
import io.github.swiftstagrime.termuxrunner.domain.model.Script
import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptRepository
import io.github.swiftstagrime.termuxrunner.ui.extensions.UiText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject

class ScriptRepositoryImpl @Inject constructor(
    private val dao: ScriptDao,
    @ApplicationContext private val context: Context
) : ScriptRepository {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    override suspend fun exportScripts(uri: Uri): Result<Unit> = runCatching {
        val entities = dao.getAllScriptsOneShot()
        val domainScripts = entities.map { it.toDomain() }

        val jsonString = json.encodeToString(domainScripts)

        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(jsonString.toByteArray())
        } ?: throw IllegalStateException(
            UiText.StringResource(R.string.error_export_output).asString(context)
        )
    }

    override suspend fun importScripts(uri: Uri): Result<Unit> = runCatching {
        val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).readText()
        } ?: throw IllegalStateException(
            UiText.StringResource(R.string.error_import_input).asString(context)
        )

        val scripts = json.decodeFromString<List<Script>>(jsonString)
        val entities = scripts.map { it.copy(id = 0).toEntity() }

        dao.insertScripts(entities)
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