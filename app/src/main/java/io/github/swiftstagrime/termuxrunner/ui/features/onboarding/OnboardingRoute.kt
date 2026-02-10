package io.github.swiftstagrime.termuxrunner.ui.features.onboarding

import android.app.AlarmManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current

    val notificationLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { _ ->
        }

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
        onAlarmPermissionGranted = {
            launchExactAlarmSettings(context)
        },
        onNotificationPermissionGranted = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
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

private fun launchExactAlarmSettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val intent =
            try {
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
            } catch (e: ActivityNotFoundException) {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
            }
        context.startActivity(intent)
    }
}
