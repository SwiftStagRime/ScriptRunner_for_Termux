package io.github.swiftstagrime.termuxrunner.ui.features.editor

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.swiftstagrime.termuxrunner.R
import io.github.swiftstagrime.termuxrunner.domain.model.Script
import io.github.swiftstagrime.termuxrunner.domain.repository.IconRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptRepository
import io.github.swiftstagrime.termuxrunner.domain.usecase.UpdateScriptUseCase
import io.github.swiftstagrime.termuxrunner.ui.extensions.UiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val scriptRepository: ScriptRepository,
    private val updateScriptUseCase: UpdateScriptUseCase,
    private val iconRepository: IconRepository
) : ViewModel() {

    private val _currentScript = MutableStateFlow<Script?>(null)
    val currentScript = _currentScript.asStateFlow()

    private val _uiEvent = Channel<EditorUiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    fun loadScript(id: Int) {
        if (id == 0) {
            _currentScript.value = Script(
                id = 0,
                name = "",
                code = "#!/bin/bash\n\n",
                interpreter = "bash"
            )
        } else {
            viewModelScope.launch {
                try {
                    val script = scriptRepository.getScriptById(id)
                    if (script != null) {
                        _currentScript.value = script
                    } else {
                        _uiEvent.send(
                            EditorUiEvent.ShowSnackbar(
                                UiText.StringResource(R.string.error_script_not_found)
                            )
                        )
                    }
                } catch (_: Exception) {
                    _uiEvent.send(
                        EditorUiEvent.ShowSnackbar(
                            UiText.StringResource(R.string.error_loading_failed)
                        )
                    )
                }
            }
        }
    }

    fun saveScript(script: Script) {
        viewModelScope.launch {
            try {
                updateScriptUseCase(script)

                _uiEvent.send(EditorUiEvent.SaveSuccess)
            } catch (_: Exception) {
                _uiEvent.send(
                    EditorUiEvent.ShowSnackbar(
                        UiText.StringResource(R.string.error_save_failed)
                    )
                )
            }
        }
    }

    suspend fun processSelectedImage(uri: Uri): String? {
        return try {
            iconRepository.saveIcon(uri.toString())
        } catch (_: Exception) {
            null
        }
    }
}

sealed interface EditorUiEvent {
    data object SaveSuccess : EditorUiEvent
    data class ShowSnackbar(val message: UiText) : EditorUiEvent
}