package io.github.swiftstagrime.termuxrunner.ui.features.editor

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.swiftstagrime.termuxrunner.domain.model.Script
import io.github.swiftstagrime.termuxrunner.domain.repository.IconRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptRepository
import io.github.swiftstagrime.termuxrunner.domain.usecase.UpdateScriptUseCase
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
                val script = scriptRepository.getScriptById(id)
                _currentScript.value = script
            }
        }
    }

    fun saveScript(script: Script) {
        viewModelScope.launch {
            updateScriptUseCase(script)
            _uiEvent.send(EditorUiEvent.SaveSuccess)
        }
    }

    suspend fun processSelectedImage(uri: Uri): String? {
        return iconRepository.saveIcon(uri.toString())
    }
}

sealed class EditorUiEvent {
    object SaveSuccess : EditorUiEvent()
}