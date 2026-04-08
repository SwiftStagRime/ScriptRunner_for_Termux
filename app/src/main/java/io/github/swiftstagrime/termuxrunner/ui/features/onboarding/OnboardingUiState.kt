package io.github.swiftstagrime.termuxrunner.ui.features.onboarding

data class OnboardingUiState(
    val isTermuxInstalled: Boolean = false,
    val isPermissionGranted: Boolean = false,
    val isBatteryUnrestricted: Boolean = false,
    val isLoading: Boolean = true
)