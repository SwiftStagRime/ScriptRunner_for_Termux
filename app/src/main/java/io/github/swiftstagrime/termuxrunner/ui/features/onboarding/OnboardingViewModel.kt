package io.github.swiftstagrime.termuxrunner.ui.features.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.swiftstagrime.termuxrunner.di.IoDispatcher
import io.github.swiftstagrime.termuxrunner.domain.repository.TermuxRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val termuxRepository: TermuxRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState = _uiState.asStateFlow()

    init {
        checkStatus()
    }

    fun checkStatus() {
        viewModelScope.launch(ioDispatcher) {
            val installed = termuxRepository.isTermuxInstalled()
            val permitted = termuxRepository.isPermissionGranted()
            val batteryUnrestricted = termuxRepository.isTermuxBatteryOptimized()

            _uiState.update {
                it.copy(
                    isTermuxInstalled = installed,
                    isPermissionGranted = permitted,
                    isBatteryUnrestricted = batteryUnrestricted,
                    isLoading = false
                )
            }
        }
    }

    fun requestTermuxOverlay() {
        termuxRepository.requestTermuxOverlay()
    }

    fun completeOnboarding(onFinished: () -> Unit) {
        viewModelScope.launch(ioDispatcher) {
            userPreferencesRepository.setOnboardingCompleted(true)
            withContext(Dispatchers.Main) {
                onFinished()
            }
        }
    }
}
