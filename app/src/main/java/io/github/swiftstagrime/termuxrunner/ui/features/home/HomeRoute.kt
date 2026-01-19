package io.github.swiftstagrime.termuxrunner.ui.features.home

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.swiftstagrime.termuxrunner.R
import io.github.swiftstagrime.termuxrunner.domain.model.InteractionMode
import io.github.swiftstagrime.termuxrunner.domain.model.Script
import io.github.swiftstagrime.termuxrunner.domain.util.BatteryUtils
import io.github.swiftstagrime.termuxrunner.domain.util.MiuiUtils
import io.github.swiftstagrime.termuxrunner.ui.components.ScriptRuntimePromptDialog
import io.github.swiftstagrime.termuxrunner.ui.extensions.ObserveAsEvents
import io.github.swiftstagrime.termuxrunner.ui.extensions.UiText
import kotlinx.coroutines.launch

@Composable
fun HomeRoute(
    onNavigateToEditor: (scriptId: Int) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToTileSettings: () -> Unit,
    onNavigateToAutomation: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.homeUiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    val selectedCategoryId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val termuxPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            viewModel.onPermissionResult(isGranted)
        }

    var scriptToPrompt by remember { mutableStateOf<Script?>(null) }

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
            is HomeUiEvent.ShowSnackbar -> {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = event.message.asString(context),
                    )
                }
            }

            is HomeUiEvent.RequestTermuxPermission -> {
                termuxPermissionLauncher.launch("com.termux.permission.RUN_COMMAND")
            }

            is HomeUiEvent.CreateShortcut -> {
                if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message =
                                UiText
                                    .StringResource(R.string.error_pinning_not_supported)
                                    .asString(context),
                        )
                    }
                    return@ObserveAsEvents
                }

                if (!MiuiUtils.hasShortcutPermission(context)) {
                    scope.launch {
                        val result =
                            snackbarHostState.showSnackbar(
                                message =
                                    UiText
                                        .StringResource(R.string.msg_miui_shortcut_permission)
                                        .asString(context),
                                actionLabel =
                                    UiText
                                        .StringResource(R.string.action_settings)
                                        .asString(context),
                                duration = SnackbarDuration.Long,
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
                    val pinned =
                        ShortcutManagerCompat.requestPinShortcut(
                            context,
                            event.shortcutInfo,
                            null,
                        )

                    if (!pinned) {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message =
                                    UiText
                                        .StringResource(R.string.msg_shortcut_denied_system)
                                        .asString(context),
                            )
                        }
                    }
                } catch (e: Exception) {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message =
                                UiText
                                    .StringResource(
                                        R.string.error_generic,
                                        e.localizedMessage ?: "",
                                    ).asString(context),
                        )
                    }
                }
            }
        }
    }

    val actions = remember(viewModel, context) {
        HomeActions(
            onSearchQueryChange = viewModel::onSearchQueryChange,
            onOpenConfig = viewModel::openConfig,
            onDismissConfig = viewModel::dismissConfig,
            onAddClick = { onNavigateToEditor(0) },
            onSettingsClick = onNavigateToSettings,
            onScriptCodeClick = { onNavigateToEditor(it.id) },
            onRunClick = { script ->
                if (script.interactionMode == InteractionMode.NONE) viewModel.runScript(script)
                else scriptToPrompt = script
            },
            onDeleteScript = viewModel::deleteScript,
            onCreateShortcutClick = viewModel::createShortcut,
            onUpdateScript = viewModel::updateScript,
            onHeartbeatToggle = { if (it) requestNotifications() },
            onRequestBatteryUnrestricted = { BatteryUtils.requestIgnoreBatteryOptimizations(context) },
            onRequestNotificationPermission = { requestNotifications() },
            onProcessImage = viewModel::processImage,
            onCategorySelect = viewModel::selectCategory,
            onSortOptionChange = viewModel::setSortOption,
            onAddNewCategory = viewModel::addCategory,
            onDeleteCategory = viewModel::deleteCategory,
            onMove = viewModel::moveScript,
            onTileSettingsClick = onNavigateToTileSettings,
            onNavigateToAutomation = onNavigateToAutomation
        )
    }

    HomeScreen(
        uiState = uiState,
        searchQuery = searchQuery,
        configState = viewModel.configState,
        originalScript = viewModel.originalScriptForConfig,
        isBatteryUnrestricted = isBatteryUnrestricted,
        selectedCategoryId = selectedCategoryId,
        sortOption = sortOption,
        snackbarHostState = snackbarHostState,
        actions = actions
    )

    scriptToPrompt?.let { script ->
        ScriptRuntimePromptDialog(
            script = script,
            onDismiss = { scriptToPrompt = null },
            onConfirm = { args, prefix, env ->
                viewModel.runScript(script, args, prefix, env)
                scriptToPrompt = null
            }
        )
    }
}
