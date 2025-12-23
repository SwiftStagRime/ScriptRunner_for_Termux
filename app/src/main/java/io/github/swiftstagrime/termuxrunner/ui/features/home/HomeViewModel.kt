package io.github.swiftstagrime.termuxrunner.ui.features.home

import android.net.Uri
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.swiftstagrime.termuxrunner.R
import io.github.swiftstagrime.termuxrunner.data.repository.TermuxException
import io.github.swiftstagrime.termuxrunner.data.repository.TermuxPermissionException
import io.github.swiftstagrime.termuxrunner.domain.model.Script
import io.github.swiftstagrime.termuxrunner.domain.repository.IconRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.ShortcutRepository
import io.github.swiftstagrime.termuxrunner.domain.usecase.DeleteScriptUseCase
import io.github.swiftstagrime.termuxrunner.domain.usecase.RunScriptUseCase
import io.github.swiftstagrime.termuxrunner.domain.usecase.UpdateScriptUseCase
import io.github.swiftstagrime.termuxrunner.ui.extensions.UiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(val scripts: List<Script>) : HomeUiState
}

sealed interface HomeUiEvent {
    data class ShowSnackbar(val message: UiText) : HomeUiEvent
    data object RequestTermuxPermission : HomeUiEvent
    data class CreateShortcut(val shortcutInfo: ShortcutInfoCompat) : HomeUiEvent
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    scriptRepository: ScriptRepository,
    private val runScriptUseCase: RunScriptUseCase,
    private val deleteScriptUseCase: DeleteScriptUseCase,
    private val updateScriptUseCase: UpdateScriptUseCase,
    private val shortcutRepository: ShortcutRepository,
    private val iconRepository: IconRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val homeUiState: StateFlow<HomeUiState> = combine(
        scriptRepository.getAllScripts(),
        _searchQuery
    ) { scripts, query ->
        if (query.isBlank()) {
            HomeUiState.Success(scripts)
        } else {
            val filtered = scripts.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.code.contains(query, ignoreCase = true)
            }
            HomeUiState.Success(filtered)
        } as HomeUiState
    }
        .onStart { emit(HomeUiState.Loading) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HomeUiState.Loading
        )

    private val _uiEvent = Channel<HomeUiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()
    private var pendingScript: Script? = null

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    suspend fun processImage(uri: Uri): String? {
        return iconRepository.saveIcon(uri.toString())
    }

    fun runScript(script: Script) {
        viewModelScope.launch {
            try {
                runScriptUseCase(script)
                sendEvent(
                    HomeUiEvent.ShowSnackbar(
                        UiText.StringResource(R.string.msg_running_script, script.name)
                    )
                )
                pendingScript = null
            } catch (_: TermuxPermissionException) {
                pendingScript = script
                sendEvent(HomeUiEvent.RequestTermuxPermission)
            } catch (e: TermuxException) {
                sendEvent(HomeUiEvent.ShowSnackbar(e.uiText))
            } catch (e: Exception) {
                e.printStackTrace()
                sendEvent(
                    HomeUiEvent.ShowSnackbar(
                        UiText.DynamicString(e.message ?: "Unknown Error")
                    )
                )
            }
        }
    }

    fun deleteScript(script: Script) {
        viewModelScope.launch {
            deleteScriptUseCase(script)
            sendEvent(HomeUiEvent.ShowSnackbar(UiText.StringResource(R.string.msg_script_deleted)))
        }
    }

    fun updateScript(script: Script) {
        viewModelScope.launch {
            updateScriptUseCase(script)
            sendEvent(HomeUiEvent.ShowSnackbar(UiText.StringResource(R.string.msg_config_saved)))
        }
    }

    fun onPermissionResult(isGranted: Boolean) {
        if (isGranted) {
            pendingScript?.let { runScript(it) }
        } else {
            sendEvent(HomeUiEvent.ShowSnackbar(UiText.StringResource(R.string.msg_permission_denied)))
            pendingScript = null
        }
    }

    fun createShortcut(script: Script) {
        viewModelScope.launch {
            if (shortcutRepository.isPinningSupported()) {
                val info = shortcutRepository.createShortcutInfo(script)
                if (info != null) {
                    sendEvent(HomeUiEvent.CreateShortcut(info))
                } else {
                    sendEvent(HomeUiEvent.ShowSnackbar(UiText.StringResource(R.string.error_shortcut_failed)))
                }
            } else {
                sendEvent(HomeUiEvent.ShowSnackbar(UiText.StringResource(R.string.error_pinning_not_supported)))
            }
        }
    }

    private fun sendEvent(event: HomeUiEvent) {
        viewModelScope.launch { _uiEvent.send(event) }
    }
}
