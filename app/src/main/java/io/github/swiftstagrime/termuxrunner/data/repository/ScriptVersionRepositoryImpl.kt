package io.github.swiftstagrime.termuxrunner.data.repository

import io.github.swiftstagrime.termuxrunner.data.local.dao.ScriptVersionDao
import io.github.swiftstagrime.termuxrunner.data.local.entity.ScriptVersionEntity
import io.github.swiftstagrime.termuxrunner.domain.model.ScriptVersion
import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptVersionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ScriptVersionRepositoryImpl
    @Inject
    constructor(
        private val dao: ScriptVersionDao,
    ) : ScriptVersionRepository {
        override suspend fun createVersion(
            scriptId: Int,
            codePages: List<String>,
            pageNames: List<String>,
            label: String?,
        ): Boolean {
            val latest = dao.getLatestVersion(scriptId)
            if (latest != null && latest.codePages == codePages && latest.pageNames == pageNames) {
                return false
            }

            val versionCount = dao.getVersionsByScriptIdOneShot(scriptId).size + 1
            val autoLabel = label ?: "Version $versionCount"

            dao.insertVersion(
                ScriptVersionEntity(
                    scriptId = scriptId,
                    codePages = codePages,
                    pageNames = pageNames,
                    timestamp = System.currentTimeMillis(),
                    label = autoLabel,
                ),
            )
            return true
        }

        override fun getVersions(scriptId: Int): Flow<List<ScriptVersion>> =
            dao.getVersionsByScriptId(scriptId).map { entities ->
                entities.map { it.toDomain() }
            }

        override suspend fun getAllVersionsOneShot(): List<ScriptVersion> = dao.getAllVersionsOneShot().map { it.toDomain() }

        override suspend fun getVersionById(id: Int): ScriptVersion? = dao.getVersionById(id)?.toDomain()

        override suspend fun deleteVersion(id: Int) {
            dao.deleteVersionById(id)
        }

        override suspend fun getVersionByIdOneShot(id: Int): ScriptVersion? = dao.getVersionById(id)?.toDomain()
    }

private fun ScriptVersionEntity.toDomain(): ScriptVersion =
    ScriptVersion(
        id = id,
        scriptId = scriptId,
        codePages = codePages,
        pageNames = pageNames,
        timestamp = timestamp,
        label = label,
    )
