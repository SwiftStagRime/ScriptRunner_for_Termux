package io.github.swiftstagrime.termuxrunner.domain.usecase

import io.github.swiftstagrime.termuxrunner.domain.model.Script
import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptRepository
import java.io.File
import javax.inject.Inject

class DeleteScriptUseCase @Inject constructor(
    private val scriptRepository: ScriptRepository
) {
    suspend operator fun invoke(script: Script) {
        script.iconPath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        }

        scriptRepository.deleteScript(script)
    }
}