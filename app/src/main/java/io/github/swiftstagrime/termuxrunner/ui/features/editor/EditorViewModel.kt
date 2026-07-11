package io.github.swiftstagrime.termuxrunner.ui.features.editor

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.swiftstagrime.termuxrunner.R
import io.github.swiftstagrime.termuxrunner.di.IoDispatcher
import io.github.swiftstagrime.termuxrunner.domain.model.Category
import io.github.swiftstagrime.termuxrunner.domain.model.Script
import io.github.swiftstagrime.termuxrunner.domain.repository.CategoryRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.IconRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.UserPreferencesRepository
import io.github.swiftstagrime.termuxrunner.domain.usecase.UpdateScriptUseCase
import io.github.swiftstagrime.termuxrunner.ui.extensions.UiText
import io.github.swiftstagrime.termuxrunner.ui.features.scriptconfigdialog.ScriptConfigState
import io.github.swiftstagrime.termuxrunner.ui.utils.WidgetManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
        private val userPreferencesRepository: UserPreferencesRepository,
        private val updateScriptUseCase: UpdateScriptUseCase,
        private val iconRepository: IconRepository,
        private val widgetManager: WidgetManager,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : ViewModel() {
        val categories =
            categoryRepository
                .getAllCategories()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = emptyList(),
                )

        val lineWrappingEnabled =
            userPreferencesRepository.editorLineWrappingEnabled
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = false,
                )

        private val _currentScript = MutableStateFlow<Script?>(null)
        val currentScript = _currentScript.asStateFlow()

        private val _uiEvent = Channel<EditorUiEvent>()
        val uiEvent = _uiEvent.receiveAsFlow()

        var editingCode by mutableStateOf(TextFieldValue(""))
            private set

        var currentPageIndex by mutableIntStateOf(0)
            private set

        var configState by mutableStateOf<ScriptConfigState?>(null)
            private set

        var pageToDeleteIndex by mutableStateOf<Int?>(null)
            internal set

        fun loadScript(id: Int) {
            if (id == 0) {
                _currentScript.value =
                    Script(
                        id = 0,
                        name = "",
                        codePages = listOf("#!/bin/bash\n\n"),
                        pageNames = listOf(""),
                        interpreter = "bash",
                    )
                return
            }

            viewModelScope.launch(ioDispatcher) {
                try {
                    val script = scriptRepository.getScriptById(id)
                    if (script != null) {
                        val finalScript =
                            if (script.pageNames.size != script.codePages.size) {
                                val defaultNames =
                                    List(script.codePages.size) { "" }
                                script.copy(pageNames = defaultNames)
                            } else {
                                script
                            }
                        _currentScript.value = finalScript
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
            viewModelScope.launch(ioDispatcher) {
                try {
                    val scriptToSave =
                        _currentScript.value?.let { current ->
                            script.copy(codePages = current.codePages)
                        } ?: script
                    updateScriptUseCase(scriptToSave)

                    try {
                        widgetManager.updateScriptsWidget()
                    } catch (_: Exception) {
                    }

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
            viewModelScope.launch(ioDispatcher) {
                categoryRepository.upsertCategory(Category(name = name))
            }
        }

        fun switchPage(index: Int) {
            val script = _currentScript.value ?: return
            if (index < 0 || index >= script.codePages.size) return
            currentPageIndex = index
            editingCode =
                TextFieldValue(
                    text = script.codePages[index],
                )
        }

        fun addPage() {
            val script = _currentScript.value ?: return
            val newPages = script.codePages.toMutableList()
            newPages.add("# New page\n")
            val newNames = script.pageNames.toMutableList()
            newNames.add("")
            _currentScript.value = script.copy(codePages = newPages, pageNames = newNames)
            switchPage(newPages.size - 1)
        }

        fun renamePage(
            index: Int,
            name: String,
        ) {
            val script = _currentScript.value ?: return
            val newNames = script.pageNames.toMutableList()
            if (index in newNames.indices) {
                newNames[index] = name
                _currentScript.value = script.copy(pageNames = newNames)
            }
        }

        fun reorderPage(
            fromIndex: Int,
            toIndex: Int,
        ) {
            val script = _currentScript.value ?: return
            if (fromIndex == toIndex) return
            if (fromIndex !in script.codePages.indices || toIndex !in script.codePages.indices) return
            val newPages = script.codePages.toMutableList()
            val newNames = script.pageNames.toMutableList()
            val movedPage = newPages.removeAt(fromIndex)
            val movedName = if (fromIndex in newNames.indices) newNames.removeAt(fromIndex) else ""
            newPages.add(toIndex, movedPage)
            newNames.add(toIndex.coerceIn(0, newNames.size), movedName)
            _currentScript.value = script.copy(codePages = newPages, pageNames = newNames)
            when (currentPageIndex) {
                fromIndex -> {
                    currentPageIndex = toIndex
                }
                in (fromIndex + 1)..toIndex -> {
                    currentPageIndex -= 1
                }
                in toIndex..<fromIndex -> {
                    currentPageIndex += 1
                }
            }
        }

        fun showDeletePageDialog(index: Int) {
            pageToDeleteIndex = index
        }

        fun confirmDeletePage() {
            val index = pageToDeleteIndex ?: return
            pageToDeleteIndex = null
            val script = _currentScript.value ?: return
            if (script.codePages.size <= 1) return
            val newPages = script.codePages.toMutableList()
            newPages.removeAt(index)
            val newNames = script.pageNames.toMutableList()
            if (index in newNames.indices) {
                newNames.removeAt(index)
            }
            _currentScript.value = script.copy(codePages = newPages, pageNames = newNames)
            val targetIndex =
                when {
                    index < currentPageIndex -> currentPageIndex - 1
                    currentPageIndex >= newPages.size -> newPages.size - 1
                    else -> currentPageIndex
                }
            switchPage(targetIndex)
        }

        fun dismissDeletePageDialog() {
            pageToDeleteIndex = null
        }

        fun updateCurrentPageCode(code: String) {
            val script = _currentScript.value ?: return
            val newPages = script.codePages.toMutableList()
            if (currentPageIndex in newPages.indices) {
                newPages[currentPageIndex] = code
                _currentScript.value = script.copy(codePages = newPages)
            }
        }

        suspend fun processSelectedImage(uri: Uri): String? =
            withContext(ioDispatcher) {
                try {
                    iconRepository.saveIcon(uri.toString())
                } catch (_: Exception) {
                    null
                }
            }
    }
