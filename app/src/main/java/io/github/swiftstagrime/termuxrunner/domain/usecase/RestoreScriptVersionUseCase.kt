package io.github.swiftstagrime.termuxrunner.domain.usecase

import io.github.swiftstagrime.termuxrunner.domain.model.Script
import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptVersionRepository
import javax.inject.Inject

class RestoreScriptVersionUseCase
    @Inject
    constructor(
        private val versionRepository: ScriptVersionRepository,
        private val scriptRepository: ScriptRepository,
    ) {
        suspend operator fun invoke(versionId: Int): Result<Script> =
            runCatching {
                val version =
                    versionRepository.getVersionByIdOneShot(versionId)
                        ?: throw IllegalArgumentException("Version not found: $versionId")

                val currentScript =
                    scriptRepository.getScriptById(version.scriptId)
                        ?: throw IllegalArgumentException("Script not found: ${version.scriptId}")

                // Backup current state as a version before restoring
                versionRepository.createVersion(
                    scriptId = currentScript.id,
                    codePages = currentScript.codePages,
                    pageNames = currentScript.pageNames,
                    label = "Backup before restore",
                )

                val restoredScript =
                    currentScript.copy(
                        codePages = version.codePages,
                        pageNames = version.pageNames,
                    )

                scriptRepository.updateScript(restoredScript)
                restoredScript
            }
    }
