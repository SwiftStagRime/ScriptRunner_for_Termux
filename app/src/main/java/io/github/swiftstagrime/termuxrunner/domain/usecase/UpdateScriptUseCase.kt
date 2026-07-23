package io.github.swiftstagrime.termuxrunner.domain.usecase

import io.github.swiftstagrime.termuxrunner.domain.model.Script
import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptVersionRepository
import java.io.File
import javax.inject.Inject

class UpdateScriptUseCase
    @Inject
    constructor(
        private val repository: ScriptRepository,
        private val versionRepository: ScriptVersionRepository,
    ) {
        suspend operator fun invoke(newScript: Script) {
            if (newScript.id != 0) {
                val fetched = repository.getScriptById(newScript.id)

                if (fetched != null) {
                    val oldPath = fetched.iconPath
                    val newPath = newScript.iconPath

                    if (oldPath != null && oldPath != newPath) {
                        val oldFile = File(oldPath)
                        if (oldFile.exists()) {
                            if (!oldFile.delete()) {
                            }
                        }
                    }

                    val codeChanged = fetched.codePages != newScript.codePages
                    val namesChanged = fetched.pageNames != newScript.pageNames

                    if (codeChanged || namesChanged) {
                        versionRepository.createVersion(
                            scriptId = fetched.id,
                            codePages = fetched.codePages,
                            pageNames = fetched.pageNames,
                        )
                    }

                    repository.updateScript(newScript)
                    return
                }
            }

            repository.insertScript(newScript)
        }
    }
