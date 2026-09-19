package io.github.swiftstagrime.termuxrunner.ui.features.onboarding

data class OnboardingUiState(
    val isTermuxInstalled: Boolean = false,
    val isPermissionGranted: Boolean = false,
    val isPermissionGrantable: Boolean = true,
    val isBatteryUnrestricted: Boolean = false,
    val isLoading: Boolean = true,
)
