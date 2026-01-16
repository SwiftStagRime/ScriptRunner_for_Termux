package io.github.swiftstagrime.termuxrunner.ui.features.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun OnboardingRoute(
    onSetupFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val isTermuxInstalled by viewModel.isTermuxInstalled.collectAsStateWithLifecycle()
    val isPermissionGranted by viewModel.isPermissionGranted.collectAsStateWithLifecycle()
    val isTermuxOptimized by viewModel.isTermuxOptimized.collectAsStateWithLifecycle()
    val isBatteryUnrestricted = !isTermuxOptimized

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { _ ->
            viewModel.checkStatus()
        }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    viewModel.checkStatus()
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    OnboardingScreen(
        isTermuxInstalled = isTermuxInstalled,
        isPermissionGranted = isPermissionGranted,
        onPermissionGranted = {
            permissionLauncher.launch("com.termux.permission.RUN_COMMAND")
        },
        onCheckAgain = {
            viewModel.completeSetup()
            onSetupFinished()
        },
        onOpenTermuxSettings = {
            viewModel.requestTermuxOverlay()
        },
        isBatteryUnrestricted = isBatteryUnrestricted,
    )
}
