package io.github.swiftstagrime.termuxrunner.ui.features.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.swiftstagrime.termuxrunner.domain.repository.TermuxRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel
    @Inject
    constructor(
        private val termuxRepository: TermuxRepository,
        private val userPreferencesRepository: UserPreferencesRepository,
    ) : ViewModel() {
        private val _isTermuxInstalled = MutableStateFlow(false)
        val isTermuxInstalled = _isTermuxInstalled.asStateFlow()

        private val _isPermissionGranted = MutableStateFlow(false)
        val isPermissionGranted = _isPermissionGranted.asStateFlow()
        private val _isTermuxOptimized = MutableStateFlow(false)
        val isTermuxOptimized = _isTermuxOptimized.asStateFlow()

        init {
            checkStatus()
        }

        fun requestTermuxOverlay() {
            termuxRepository.requestTermuxOverlay()
        }

        fun checkStatus() {
            _isTermuxInstalled.value = termuxRepository.isTermuxInstalled()
            _isPermissionGranted.value = termuxRepository.isPermissionGranted()
        }

        fun completeSetup() {
            viewModelScope.launch {
                userPreferencesRepository.setOnboardingCompleted(true)
            }
        }
    }
