package io.github.swiftstagrime.termuxrunner.domain.repository

import io.github.swiftstagrime.termuxrunner.domain.model.ScriptVersion
import kotlinx.coroutines.flow.Flow

interface ScriptVersionRepository {
    suspend fun getAllVersionsOneShot(): List<ScriptVersion>

    fun getVersions(scriptId: Int): Flow<List<ScriptVersion>>

    suspend fun getVersionById(id: Int): ScriptVersion?

    suspend fun createVersion(
        scriptId: Int,
        codePages: List<String>,
        pageNames: List<String> = emptyList(),
        label: String? = null,
    ): Boolean

    suspend fun deleteVersion(id: Int)

    suspend fun getVersionByIdOneShot(id: Int): ScriptVersion?
}
