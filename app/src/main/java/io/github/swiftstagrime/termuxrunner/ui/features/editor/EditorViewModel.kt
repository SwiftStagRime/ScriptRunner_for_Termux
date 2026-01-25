package io.github.swiftstagrime.termuxrunner.ui.features.editor

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.swiftstagrime.termuxrunner.R
import io.github.swiftstagrime.termuxrunner.domain.model.Category
import io.github.swiftstagrime.termuxrunner.domain.model.Script
import io.github.swiftstagrime.termuxrunner.domain.repository.CategoryRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.IconRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptRepository
import io.github.swiftstagrime.termuxrunner.domain.usecase.UpdateScriptUseCase
import io.github.swiftstagrime.termuxrunner.ui.extensions.UiText
import io.github.swiftstagrime.termuxrunner.ui.features.scriptconfigdialog.ScriptConfigState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface EditorUiEvent {
    data object SaveSuccess : EditorUiEvent

    data class ShowSnackbar(
        val message: UiText,
    ) : EditorUiEvent
}

@HiltViewModel
class EditorViewModel
    @Inject
    constructor(
        private val scriptRepository: ScriptRepository,
        private val categoryRepository: CategoryRepository,
        private val updateScriptUseCase: UpdateScriptUseCase,
        private val iconRepository: IconRepository,
    ) : ViewModel() {
        val categories =
            categoryRepository
                .getAllCategories()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = emptyList(),
                )

        private val _currentScript = MutableStateFlow<Script?>(null)
        val currentScript = _currentScript.asStateFlow()

        private val _uiEvent = Channel<EditorUiEvent>()
        val uiEvent = _uiEvent.receiveAsFlow()

        var editingCode by mutableStateOf(TextFieldValue(""))
            private set

        var configState by mutableStateOf<ScriptConfigState?>(null)
            private set

        fun loadScript(id: Int) {
            if (id == 0) {
                _currentScript.value =
                    Script(
                        id = 0,
                        name = "",
                        code = "#!/bin/bash\n\n",
                        interpreter = "bash",
                    )
                return
            }

            viewModelScope.launch {
                try {
                    val script = scriptRepository.getScriptById(id)
                    if (script != null) {
                        _currentScript.value = script
                        editingCode =
                            TextFieldValue(
                                text = script.code,
                                selection = TextRange(script.code.length),
                            )
                    } else {
                        _uiEvent.send(
                            EditorUiEvent.ShowSnackbar(
                                UiText.StringResource(R.string.error_script_not_found),
                            ),
                        )
                    }
                } catch (_: Exception) {
                    _uiEvent.send(
                        EditorUiEvent.ShowSnackbar(
                            UiText.StringResource(R.string.error_loading_failed),
                        ),
                    )
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
                            UiText.StringResource(R.string.error_save_failed),
                        ),
                    )
                }
            }
        }

        fun openConfig(script: Script) {
            configState = ScriptConfigState(script)
        }

        fun dismissConfig() {
            configState = null
        }

        fun addCategory(name: String) {
            viewModelScope.launch {
                categoryRepository.upsertCategory(Category(name = name))
            }
        }

        suspend fun processSelectedImage(uri: Uri): String? =
            try {
                iconRepository.saveIcon(uri.toString())
            } catch (_: Exception) {
                null
            }
    }
