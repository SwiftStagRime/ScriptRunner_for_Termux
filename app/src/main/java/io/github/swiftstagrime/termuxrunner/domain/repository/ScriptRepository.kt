package io.github.swiftstagrime.termuxrunner.domain.repository

import android.net.Uri
import io.github.swiftstagrime.termuxrunner.domain.model.Script
import kotlinx.coroutines.flow.Flow

interface ScriptRepository {
    fun getAllScripts(): Flow<List<Script>>
    suspend fun getScriptById(id: Int): Script?
    suspend fun insertScript(script: Script)
    suspend fun deleteScript(script: Script)
    suspend fun exportScripts(uri: Uri): Result<Unit>
    suspend fun importScripts(uri: Uri): Result<Unit>
    suspend fun updateScriptsOrder(orders: List<Pair<Int, Int>>)
}