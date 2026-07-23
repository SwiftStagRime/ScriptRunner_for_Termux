package io.github.swiftstagrime.termuxrunner.domain.usecase

import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptVersionRepository
import javax.inject.Inject

class DeleteScriptVersionUseCase
    @Inject
    constructor(
        private val versionRepository: ScriptVersionRepository,
    ) {
        suspend operator fun invoke(versionId: Int) {
            versionRepository.deleteVersion(versionId)
        }
    }
