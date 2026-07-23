package io.github.swiftstagrime.termuxrunner.domain.usecase

import android.util.Log
import io.github.swiftstagrime.termuxrunner.domain.model.Script
import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptVersionRepository
import java.io.File
import javax.inject.Inject

class DeleteScriptUseCase
    @Inject
    constructor(
        private val scriptRepository: ScriptRepository,
        private val versionRepository: ScriptVersionRepository,
    ) {
        suspend operator fun invoke(script: Script) {
            script.iconPath?.let { path ->
                val file = File(path)
                if (file.exists()) {
                    if (!file.delete()) {
                        Log.w("DeleteScript", "Failed to delete icon: $path")
                    }
                }
            }

            scriptRepository.deleteScript(script)
        }
    }
