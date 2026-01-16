package io.github.swiftstagrime.termuxrunner.ui.features.runner

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.swiftstagrime.termuxrunner.data.repository.TermuxPermissionException
import io.github.swiftstagrime.termuxrunner.domain.model.InteractionMode
import io.github.swiftstagrime.termuxrunner.domain.model.Script
import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.UserPreferencesRepository
import io.github.swiftstagrime.termuxrunner.domain.usecase.RunScriptUseCase
import io.github.swiftstagrime.termuxrunner.ui.theme.AppTheme
import io.github.swiftstagrime.termuxrunner.ui.theme.ThemeMode
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScriptRunnerViewModel
    @Inject
    constructor(
        private val scriptRepository: ScriptRepository,
        private val runScriptUseCase: RunScriptUseCase,
        private val userPreferencesRepository: UserPreferencesRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val scriptId: Int = savedStateHandle.get<Int>("SCRIPT_ID") ?: -1

        private val _events = Channel<ScriptRunnerEvent>()
        val events = _events.receiveAsFlow()

        private val _scriptToPrompt = MutableStateFlow<Script?>(null)
        val scriptToPrompt = _scriptToPrompt.asStateFlow()

        val selectedAccent =
            userPreferencesRepository.selectedAccent
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppTheme.GREEN)

        val selectedMode =
            userPreferencesRepository.selectedMode
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

        private var pendingArgs: String? = null
        private var pendingPrefix: String? = null
        private var pendingEnv: Map<String, String>? = null

        init {
            if (scriptId != -1) {
                checkScriptAndExecute()
            } else {
                sendEvent(ScriptRunnerEvent.Finish)
            }
        }

        private fun checkScriptAndExecute() {
            viewModelScope.launch {
                val script = scriptRepository.getScriptById(scriptId)
                if (script == null) {
                    sendEvent(ScriptRunnerEvent.Finish)
                    return@launch
                }

                if (script.interactionMode == InteractionMode.NONE) {
                    executeScript(script)
                } else {
                    _scriptToPrompt.value = script
                }
            }
        }

        fun executeScript(
            script: Script,
            runtimeArgs: String? = null,
            runtimePrefix: String? = null,
            runtimeEnv: Map<String, String>? = null,
        ) {
            viewModelScope.launch {
                try {
                    runScriptUseCase(
                        script = script,
                        runtimeArgs = runtimeArgs,
                        runtimePrefix = runtimePrefix,
                        runtimeEnv = runtimeEnv,
                    )
                    sendEvent(ScriptRunnerEvent.Finish)
                } catch (_: TermuxPermissionException) {
                    pendingArgs = runtimeArgs
                    pendingPrefix = runtimePrefix
                    pendingEnv = runtimeEnv
                    sendEvent(ScriptRunnerEvent.RequestPermission)
                } catch (e: Exception) {
                    sendEvent(ScriptRunnerEvent.ShowError("Error: ${e.message}"))
                    sendEvent(ScriptRunnerEvent.Finish)
                }
            }
        }

        fun onPermissionGranted() {
            val script = _scriptToPrompt.value ?: return
            executeScript(script, pendingArgs, pendingPrefix, pendingEnv)
        }

        fun dismissPrompt() {
            sendEvent(ScriptRunnerEvent.Finish)
        }

        private fun sendEvent(event: ScriptRunnerEvent) {
            viewModelScope.launch { _events.send(event) }
        }
    }

sealed class ScriptRunnerEvent {
    object Finish : ScriptRunnerEvent()

    object RequestPermission : ScriptRunnerEvent()

    data class ShowError(
        val message: String,
    ) : ScriptRunnerEvent()
}
