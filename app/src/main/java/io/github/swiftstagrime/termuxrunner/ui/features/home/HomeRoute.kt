package io.github.swiftstagrime.termuxrunner.ui.features.home

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.swiftstagrime.termuxrunner.R
import io.github.swiftstagrime.termuxrunner.domain.util.MiuiUtils
import io.github.swiftstagrime.termuxrunner.ui.extensions.ObserveAsEvents
import io.github.swiftstagrime.termuxrunner.ui.extensions.UiText
import kotlinx.coroutines.launch

@Composable
fun HomeRoute(
    onNavigateToEditor: (scriptId: Int) -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.homeUiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.onPermissionResult(isGranted)
    }

    ObserveAsEvents(viewModel.uiEvent) { event ->
        when (event) {
            is HomeUiEvent.ShowSnackbar -> {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = event.message.asString(context)
                    )
                }
            }

            is HomeUiEvent.RequestTermuxPermission -> {
                permissionLauncher.launch("com.termux.permission.RUN_COMMAND")
            }

            is HomeUiEvent.CreateShortcut -> {
                if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = UiText.StringResource(R.string.error_pinning_not_supported)
                                .asString(context)
                        )
                    }
                    return@ObserveAsEvents
                }

                if (!MiuiUtils.hasShortcutPermission(context)) {
                    scope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = UiText.StringResource(R.string.msg_miui_shortcut_permission)
                                .asString(context),
                            actionLabel = UiText.StringResource(R.string.action_settings)
                                .asString(context),
                            duration = SnackbarDuration.Long
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            val intent =
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                            context.startActivity(intent)
                        }
                    }
                    return@ObserveAsEvents
                }

                try {
                    val pinned = ShortcutManagerCompat.requestPinShortcut(
                        context,
                        event.shortcutInfo,
                        null
                    )

                    if (!pinned) {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = UiText.StringResource(R.string.msg_shortcut_denied_system)
                                    .asString(context)
                            )
                        }
                    }
                } catch (e: Exception) {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = UiText.StringResource(
                                R.string.error_generic,
                                e.localizedMessage ?: ""
                            ).asString(context)
                        )
                    }
                }
            }
        }
    }

    HomeScreen(
        uiState = uiState,
        searchQuery = searchQuery,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        snackbarHostState = snackbarHostState,
        onScriptCodeClick = { script -> onNavigateToEditor(script.id) },
        onRunClick = viewModel::runScript,
        onUpdateScript = viewModel::updateScript,
        onDeleteScript = viewModel::deleteScript,
        onCreateShortcutClick = viewModel::createShortcut,
        onAddClick = { onNavigateToEditor(0) },
        onSettingsClick = onNavigateToSettings,
        onProcessImage = viewModel::processImage
    )
}

