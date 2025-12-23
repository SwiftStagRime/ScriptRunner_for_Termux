package io.github.swiftstagrime.termuxrunner.ui.features.runner

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.swiftstagrime.termuxrunner.data.repository.TermuxPermissionException
import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptRepository
import io.github.swiftstagrime.termuxrunner.domain.usecase.RunScriptUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScriptRunnerViewModel @Inject constructor(
    private val scriptRepository: ScriptRepository,
    private val runScriptUseCase: RunScriptUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val scriptId: Int = savedStateHandle.get<Int>("SCRIPT_ID") ?: -1

    private val _events = Channel<ScriptRunnerEvent>()
    val events = _events.receiveAsFlow()

    init {
        if (scriptId != -1) {
            executeScript()
        } else {
            sendEvent(ScriptRunnerEvent.Finish)
        }
    }

    fun executeScript() {
        viewModelScope.launch {
            try {
                val script = scriptRepository.getScriptById(scriptId)
                if (script != null) {
                    runScriptUseCase(script)
                    sendEvent(ScriptRunnerEvent.Finish)
                } else {
                    sendEvent(ScriptRunnerEvent.Finish)
                }
            } catch (_: TermuxPermissionException) {
                sendEvent(ScriptRunnerEvent.RequestPermission)
            } catch (e: Exception) {
                sendEvent(ScriptRunnerEvent.ShowError("Error: ${e.message}"))
                sendEvent(ScriptRunnerEvent.Finish)
            }
        }
    }

    private fun sendEvent(event: ScriptRunnerEvent) {
        viewModelScope.launch { _events.send(event) }
    }
}

sealed class ScriptRunnerEvent {
    object Finish : ScriptRunnerEvent()
    object RequestPermission : ScriptRunnerEvent()
    data class ShowError(val message: String) : ScriptRunnerEvent()
}