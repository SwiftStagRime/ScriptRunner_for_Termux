package io.github.swiftstagrime.termuxrunner.ui.features.automation

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.swiftstagrime.termuxrunner.domain.model.Automation
import io.github.swiftstagrime.termuxrunner.domain.model.InteractionMode
import io.github.swiftstagrime.termuxrunner.domain.model.Script
import io.github.swiftstagrime.termuxrunner.domain.model.ScriptRuntimeParams
import io.github.swiftstagrime.termuxrunner.domain.util.AutomationFormatter
import io.github.swiftstagrime.termuxrunner.ui.components.ScriptPickerDialog
import io.github.swiftstagrime.termuxrunner.ui.components.ScriptRuntimePromptDialog
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationRoute(
    onBackClick: () -> Unit,
    viewModel: AutomationViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val colorError = MaterialTheme.colorScheme.error.toArgb()
    val colorSuccess = MaterialTheme.colorScheme.tertiary.toArgb()
    val colorIdle = MaterialTheme.colorScheme.outline.toArgb()

    val automations by viewModel.automations.collectAsStateWithLifecycle()
    val allScripts by viewModel.allScripts.collectAsStateWithLifecycle()
    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()

    var hasExactAlarmPermission by rememberSaveable { mutableStateOf(true) }
    var showScriptPicker by rememberSaveable { mutableStateOf(false) }
    var showRuntimeDialog by rememberSaveable { mutableStateOf(false) }
    var showScheduleConfig by rememberSaveable { mutableStateOf(false) }

    var selectedScriptForAutomation by rememberSaveable { mutableStateOf<Script?>(null) }
    var capturedRuntimeParams by rememberSaveable { mutableStateOf<ScriptRuntimeParams?>(null) }
    var selectedAutomationForHistory by rememberSaveable { mutableStateOf<Automation?>(null) }

    fun checkPermission() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        hasExactAlarmPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else true
    }

    val timeTicker by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(60_000)
            value = System.currentTimeMillis()
        }
    }

    val uiItems = remember(automations, allScripts, timeTicker) {
        val scriptMap = allScripts.associateBy { it.id }
        automations.map { automation ->
            val script = scriptMap[automation.scriptId]
            AutomationUiItem(
                automation = automation,
                scriptName = script?.name ?: "Unknown",
                scriptIconPath = script?.iconPath,
                nextRunText = AutomationFormatter.formatNextRun(
                    context,
                    automation.nextRunTimestamp
                ),
                lastRunText = AutomationFormatter.formatLastRun(
                    context,
                    automation.lastRunTimestamp,
                    automation.lastExitCode
                ),
                statusColor = when (automation.lastExitCode) {
                    0 -> colorSuccess
                    null -> colorIdle
                    else -> colorError
                }
            )
        }
    }

    val historyLogs by produceState(emptyList(), selectedAutomationForHistory) {
        selectedAutomationForHistory?.let {
            viewModel.getAutomationLogs(it.id).collect { value = it }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) checkPermission()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showScriptPicker) {
        ScriptPickerDialog(
            scripts = allScripts,
            categories = allCategories,
            onDismiss = { showScriptPicker = false },
            onScriptSelected = { script ->
                selectedScriptForAutomation = script
                showScriptPicker = false
                if (script.interactionMode != InteractionMode.NONE) {
                    showRuntimeDialog = true
                } else {
                    capturedRuntimeParams = ScriptRuntimeParams(
                        arguments = script.executionParams,
                        prefix = script.commandPrefix,
                        envVars = script.envVars
                    )
                    showScheduleConfig = true
                }
            }
        )
    }

    if (showRuntimeDialog && selectedScriptForAutomation != null) {
        ScriptRuntimePromptDialog(
            script = selectedScriptForAutomation!!,
            onDismiss = {
                showRuntimeDialog = false
                selectedScriptForAutomation = null
            },
            onConfirm = { args, prefix, env ->
                capturedRuntimeParams = ScriptRuntimeParams(args, prefix, env)
                showRuntimeDialog = false
                showScheduleConfig = true
            }
        )
    }

    if (showScheduleConfig && selectedScriptForAutomation != null) {
        AutomationConfigDialog(
            script = selectedScriptForAutomation!!,
            onDismiss = {
                showScheduleConfig = false
                selectedScriptForAutomation = null
                capturedRuntimeParams = null
            },
            onSave = { label, type, time, interval, days, missed, wifi, charging, batteryThreshold ->
                viewModel.saveAutomation(
                    scriptId = selectedScriptForAutomation!!.id,
                    label = label, type = type, timestamp = time,
                    interval = interval, days = days, runIfMissed = missed,
                    requireWifi = wifi, requireCharging = charging,
                    batteryThreshold = batteryThreshold,
                    runtime = capturedRuntimeParams ?: ScriptRuntimeParams()
                )
                showScheduleConfig = false
                selectedScriptForAutomation = null
                capturedRuntimeParams = null
            }
        )
    }

    AutomationScreen(
        uiState = AutomationUiState(
            items = uiItems,
            isExactAlarmPermissionGranted = hasExactAlarmPermission
        ),
        onBackClick = onBackClick,
        onToggleAutomation = viewModel::toggleAutomation,
        onDeleteAutomation = viewModel::deleteAutomation,
        onAddAutomationClick = { showScriptPicker = true },
        onRunNow = viewModel::runAutomationNow,
        onShowHistory = { selectedAutomationForHistory = it },
        onRequestPermission = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val intent = try {
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                } catch (e: Exception) {
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                }
                context.startActivity(intent)
            }
        }
    )

    if (selectedAutomationForHistory != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedAutomationForHistory = null },
            sheetState = rememberModalBottomSheetState(),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            AutomationHistorySheet(
                automationName = selectedAutomationForHistory?.label ?: "",
                logs = historyLogs
            )
        }
    }
}