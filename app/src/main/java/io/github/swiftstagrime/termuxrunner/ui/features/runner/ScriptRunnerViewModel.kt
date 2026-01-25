package io.github.swiftstagrime.termuxrunner.ui.features.runner

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.swiftstagrime.termuxrunner.data.repository.TermuxPermissionException
import io.github.swiftstagrime.termuxrunner.domain.model.InteractionMode
import io.github.swiftstagrime.termuxrunner.domain.model.Script
import io.github.swiftstagrime.termuxrunner.domain.repository.CategoryRepository
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
class ScriptRunnerViewModel @Inject constructor(
    private val scriptRepository: ScriptRepository,
    private val runScriptUseCase: RunScriptUseCase,
    private val categoryRepository: CategoryRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val scriptId: Int = savedStateHandle.get<Int>("SCRIPT_ID") ?: -1

    val selectedAccent = userPreferencesRepository.selectedAccent
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppTheme.GREEN)

    val selectedMode = userPreferencesRepository.selectedMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    // State for the Picker Dialog
    private val _showScriptPicker = MutableStateFlow(false)
    val showScriptPicker = _showScriptPicker.asStateFlow()

    // Data for the Picker
    val allScripts = scriptRepository.getAllScripts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories = categoryRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _scriptToPrompt = MutableStateFlow<Script?>(null)
    val scriptToPrompt = _scriptToPrompt.asStateFlow()

    private val _events = Channel<ScriptRunnerEvent>()
    val events = _events.receiveAsFlow()

    private var pendingScript: Script? = null
    private var pendingArgs: String? = null
    private var pendingPrefix: String? = null
    private var pendingEnv: Map<String, String>? = null

    init {
        if (scriptId != -1) {
            fetchAndRunScript(scriptId)
        } else {
            _showScriptPicker.value = true
        }
    }

    private fun fetchAndRunScript(id: Int) {
        viewModelScope.launch {
            val script = scriptRepository.getScriptById(id)
            if (script == null) {
                sendEvent(ScriptRunnerEvent.Finish)
                return@launch
            }
            processScriptLogic(script)
        }
    }

    fun onScriptSelected(script: Script) {
        _showScriptPicker.value = false
        processScriptLogic(script)
    }
    private fun processScriptLogic(script: Script) {
        if (script.interactionMode == InteractionMode.NONE) {
            executeScript(script)
        } else {
            _scriptToPrompt.value = script
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
                pendingScript = script
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
        val script = pendingScript ?: _scriptToPrompt.value ?: return
        executeScript(script, pendingArgs, pendingPrefix, pendingEnv)
    }

    fun dismissPrompt() {
        sendEvent(ScriptRunnerEvent.Finish)
    }

    fun dismissPicker() {
        sendEvent(ScriptRunnerEvent.Finish)
    }

    private fun sendEvent(event: ScriptRunnerEvent) {
        viewModelScope.launch { _events.send(event) }
    }
}

sealed interface ScriptRunnerEvent {
    data object Finish : ScriptRunnerEvent
    data object RequestPermission : ScriptRunnerEvent
    data class ShowError(val message: String) : ScriptRunnerEvent
}
