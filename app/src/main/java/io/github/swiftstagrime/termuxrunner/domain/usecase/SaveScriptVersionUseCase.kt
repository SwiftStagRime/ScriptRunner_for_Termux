package io.github.swiftstagrime.termuxrunner.domain.usecase

import io.github.swiftstagrime.termuxrunner.domain.model.Script
import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptVersionRepository
import javax.inject.Inject

class SaveScriptVersionUseCase
    @Inject
    constructor(
        private val versionRepository: ScriptVersionRepository,
    ) {
        suspend operator fun invoke(script: Script): Boolean =
            versionRepository.createVersion(
                scriptId = script.id,
                codePages = script.codePages,
                pageNames = script.pageNames,
            )
    }
