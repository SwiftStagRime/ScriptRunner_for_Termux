package io.github.swiftstagrime.termuxrunner.domain.usecase

import io.github.swiftstagrime.termuxrunner.domain.model.Script
import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptRepository
import java.io.File
import javax.inject.Inject

class UpdateScriptUseCase
    @Inject
    constructor(
        private val repository: ScriptRepository,
    ) {
        suspend operator fun invoke(newScript: Script) {
            if (newScript.id != 0) {
                val oldScript = repository.getScriptById(newScript.id)

                if (oldScript != null) {
                    val oldPath = oldScript.iconPath
                    val newPath = newScript.iconPath

                    if (oldPath != null && oldPath != newPath) {
                        val oldFile = File(oldPath)
                        if (oldFile.exists()) {
                            oldFile.delete()
                        }
                    }
                }
            }

            repository.insertScript(newScript)
        }
    }
