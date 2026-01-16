package io.github.swiftstagrime.termuxrunner.ui.features.editor

import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.swiftstagrime.termuxrunner.R
import io.github.swiftstagrime.termuxrunner.domain.util.BatteryUtils
import io.github.swiftstagrime.termuxrunner.ui.extensions.ObserveAsEvents
import io.github.swiftstagrime.termuxrunner.ui.extensions.UiText
import kotlinx.coroutines.launch

@Composable
fun EditorRoute(
    onBack: () -> Unit,
    viewModel: EditorViewModel = hiltViewModel(),
) {
    val script by viewModel.currentScript.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var isBatteryUnrestricted by remember {
        mutableStateOf(BatteryUtils.isIgnoringBatteryOptimizations(context))
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    isBatteryUnrestricted = BatteryUtils.isIgnoringBatteryOptimizations(context)
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            if (!isGranted) {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message =
                            UiText
                                .StringResource(R.string.msg_notification_needed_for_heartbeat)
                                .asString(context),
                    )
                }
            }
        }
    val requestNotifications =
        remember {
            {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val hasPermission =
                        ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.POST_NOTIFICATIONS,
                        ) == PackageManager.PERMISSION_GRANTED

                    if (!hasPermission) {
                        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }
        }
    ObserveAsEvents(viewModel.uiEvent) { event ->
        when (event) {
            is EditorUiEvent.SaveSuccess -> onBack()
            is EditorUiEvent.ShowSnackbar -> {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = event.message.asString(context),
                    )
                }
            }
        }
    }

    if (script == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        EditorScreen(
            script = script!!,
            categories = categories,
            onBack = onBack,
            onSave = viewModel::saveScript,
            onAddNewCategory = viewModel::addCategory,
            onProcessImage = viewModel::processSelectedImage,
            onHeartbeatToggle = { enabled ->
                if (enabled) requestNotifications()
            },
            snackbarHostState = snackbarHostState,
            isBatteryUnrestricted = isBatteryUnrestricted,
            onRequestBatteryUnrestricted = {
                BatteryUtils.requestIgnoreBatteryOptimizations(context)
            },
            onRequestNotificationPermission = {
                requestNotifications()
            },
        )
    }
}
