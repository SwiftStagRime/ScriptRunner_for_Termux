package io.github.swiftstagrime.termuxrunner.ui.features.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.swiftstagrime.termuxrunner.R
import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.UserPreferencesRepository
import io.github.swiftstagrime.termuxrunner.ui.extensions.UiText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val scriptRepository: ScriptRepository
) : ViewModel() {

    val useDynamicColors = userPreferencesRepository.useDynamicColors
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setDynamicColors(enabled)
        }
    }

    private val _ioState = MutableStateFlow<UiText?>(null)
    val ioState = _ioState.asStateFlow()

    fun exportData(uri: Uri) {
        viewModelScope.launch {
            _ioState.value = UiText.StringResource(R.string.exporting)
            scriptRepository.exportScripts(uri)
                .onSuccess {
                    _ioState.value = UiText.StringResource(R.string.export_success)
                }
                .onFailure {
                    _ioState.value = UiText.StringResource(
                        R.string.export_failed,
                        it.localizedMessage ?: "Unknown Error"
                    )
                }
        }
    }

    fun importData(uri: Uri) {
        viewModelScope.launch {
            _ioState.value = UiText.StringResource(R.string.importing)
            scriptRepository.importScripts(uri)
                .onSuccess {
                    _ioState.value = UiText.StringResource(R.string.import_success)
                }
                .onFailure {
                    _ioState.value = UiText.StringResource(
                        R.string.import_failed,
                        it.localizedMessage ?: "Unknown Error"
                    )
                }
        }
    }

    fun clearMessage() {
        _ioState.value = null
    }
}