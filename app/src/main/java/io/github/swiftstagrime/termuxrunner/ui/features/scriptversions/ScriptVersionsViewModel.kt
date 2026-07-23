package io.github.swiftstagrime.termuxrunner.ui.features.scriptversions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.swiftstagrime.termuxrunner.di.IoDispatcher
import io.github.swiftstagrime.termuxrunner.domain.model.ScriptVersion
import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptVersionRepository
import io.github.swiftstagrime.termuxrunner.domain.usecase.DeleteScriptVersionUseCase
import io.github.swiftstagrime.termuxrunner.domain.usecase.RestoreScriptVersionUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

sealed class ScriptVersionsNavEvent {
    data class NavigateBack(
        val scriptId: Int,
    ) : ScriptVersionsNavEvent()
}

@HiltViewModel
class ScriptVersionsViewModel
    @Inject
    constructor(
        private val versionRepository: ScriptVersionRepository,
        private val restoreUseCase: RestoreScriptVersionUseCase,
        private val deleteUseCase: DeleteScriptVersionUseCase,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : ViewModel() {
        private val _navEvents = Channel<ScriptVersionsNavEvent>(Channel.UNLIMITED)
        val navEvents = _navEvents.receiveAsFlow()

        private val scriptId = MutableStateFlow(0)

        private val _versions = MutableStateFlow<List<ScriptVersion>>(emptyList())
        val versions: StateFlow<List<ScriptVersion>> = _versions.asStateFlow()

        private fun refreshVersions(scriptId: Int) {
            viewModelScope.launch(ioDispatcher) {
                val allV = versionRepository.getAllVersionsOneShot()
                _versions.value = allV.filter { it.scriptId == scriptId }
            }
        }

        fun setScriptId(newScriptId: Int) {
            scriptId.value = newScriptId
            refreshVersions(newScriptId)
        }

        fun restoreVersion(versionId: Int) {
            val currentScriptId = scriptId.value
            viewModelScope.launch(ioDispatcher) {
                restoreUseCase(versionId).fold(
                    onSuccess = {
                        _navEvents.send(ScriptVersionsNavEvent.NavigateBack(currentScriptId))
                    },
                    onFailure = { },
                )
            }
        }

        fun deleteVersion(version: ScriptVersion) {
            viewModelScope.launch(ioDispatcher) {
                deleteUseCase(version.id)
            }
        }

        fun navigateBack() {
            viewModelScope.launch {
                _navEvents.send(ScriptVersionsNavEvent.NavigateBack(scriptId.value))
            }
        }

        companion object {
            private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

            fun formatTimestamp(timestamp: Long): String = dateFormat.format(timestamp)
        }
    }
