package io.github.swiftstagrime.termuxrunner.ui.features.automation

import android.app.AlarmManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Parcelable
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
import io.github.swiftstagrime.termuxrunner.domain.model.AutomationLog
import io.github.swiftstagrime.termuxrunner.domain.model.AutomationType
import io.github.swiftstagrime.termuxrunner.domain.model.Category
import io.github.swiftstagrime.termuxrunner.domain.model.InteractionMode
import io.github.swiftstagrime.termuxrunner.domain.model.Script
import io.github.swiftstagrime.termuxrunner.domain.model.ScriptRuntimeParams
import io.github.swiftstagrime.termuxrunner.domain.util.AutomationFormatter
import io.github.swiftstagrime.termuxrunner.ui.components.ScriptPickerDialog
import io.github.swiftstagrime.termuxrunner.ui.components.ScriptRuntimePromptDialog
import io.github.swiftstagrime.termuxrunner.ui.features.automation.components.AutomationConfigDialog
import io.github.swiftstagrime.termuxrunner.ui.features.automation.components.AutomationHistorySheet
import kotlinx.coroutines.delay
import kotlinx.parcelize.Parcelize

private const val MINUTE_IN_MILLIS = 60_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationRoute(
    onBackClick: () -> Unit,
    viewModel: AutomationViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val automations by viewModel.automations.collectAsStateWithLifecycle()
    val allScripts by viewModel.allScripts.collectAsStateWithLifecycle()
    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()

    var hasExactAlarmPermission by rememberSaveable { mutableStateOf(true) }
    var selectedAutomationForHistory by rememberSaveable { mutableStateOf<Automation?>(null) }

    var flowState by rememberSaveable { mutableStateOf<AutomationFlowState>(AutomationFlowState.Idle) }

    HandleLifecyclePermissions {
        hasExactAlarmPermission = checkExactAlarmPermission(context)
    }

    val uiItems = rememberAutomationUiItems(automations, allScripts)

    AutomationScreen(
        uiState = AutomationUiState(uiItems, hasExactAlarmPermission),
        onBackClick = onBackClick,
        onToggleAutomation = viewModel::toggleAutomation,
        onDeleteAutomation = viewModel::deleteAutomation,
        onAddAutomationClick = { flowState = AutomationFlowState.PickingScript },
        onRunNow = viewModel::runAutomationNow,
        onShowHistory = { selectedAutomationForHistory = it },
        onRequestPermission = { launchExactAlarmSettings(context) },
    )

    AutomationCreationFlow(
        flowState = flowState,
        allScripts = allScripts,
        allCategories = allCategories,
        onDismiss = { flowState = AutomationFlowState.Idle },
        onStateChange = { flowState = it },
        onSave = { params ->
            viewModel.saveAutomation(params)
            flowState = AutomationFlowState.Idle
        }
    )

    AutomationHistoryView(
        selectedAutomation = selectedAutomationForHistory,
        onDismiss = { selectedAutomationForHistory = null },
        viewModel = viewModel
    )
}

@Composable
private fun rememberAutomationUiItems(
    automations: List<Automation>,
    allScripts: List<Script>
): List<AutomationUiItem> {
    val context = LocalContext.current
    val colorError = MaterialTheme.colorScheme.error.toArgb()
    val colorSuccess = MaterialTheme.colorScheme.tertiary.toArgb()
    val colorIdle = MaterialTheme.colorScheme.outline.toArgb()

    val timeTicker by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(MINUTE_IN_MILLIS)
            value = System.currentTimeMillis()
        }
    }

    return remember(automations, allScripts, timeTicker) {
        val scriptMap = allScripts.associateBy { it.id }
        automations.map { automation ->
            val script = scriptMap[automation.scriptId]
            val statusColor = when (automation.lastExitCode) {
                0 -> colorSuccess
                null -> colorIdle
                else -> colorError
            }
            AutomationUiItem(
                automation = automation,
                scriptName = script?.name ?: "Unknown",
                scriptIconPath = script?.iconPath,
                nextRunText = AutomationFormatter.formatNextRun(context, automation.nextRunTimestamp),
                lastRunText = AutomationFormatter.formatLastRun(context, automation.lastRunTimestamp, automation.lastExitCode),
                statusColor = statusColor,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutomationCreationFlow(
    flowState: AutomationFlowState,
    allScripts: List<Script>,
    allCategories: List<Category>,
    onDismiss: () -> Unit,
    onStateChange: (AutomationFlowState) -> Unit,
    onSave: (AutomationSaveParams) -> Unit
) {
    when (flowState) {
        is AutomationFlowState.PickingScript -> {
            ScriptPickerDialog(
                scripts = allScripts,
                categories = allCategories,
                onDismiss = onDismiss,
                onScriptSelected = { script ->
                    if (script.interactionMode != InteractionMode.NONE) {
                        onStateChange(AutomationFlowState.PromptingRuntime(script))
                    } else {
                        val params = ScriptRuntimeParams(script.executionParams, script.commandPrefix, script.envVars)
                        onStateChange(AutomationFlowState.Configuring(script, params))
                    }
                }
            )
        }
        is AutomationFlowState.PromptingRuntime -> {
            ScriptRuntimePromptDialog(
                script = flowState.script,
                onDismiss = onDismiss,
                onConfirm = { args, prefix, env ->
                    onStateChange(AutomationFlowState.Configuring(flowState.script, ScriptRuntimeParams(args, prefix, env)))
                }
            )
        }
        is AutomationFlowState.Configuring -> {
            AutomationConfigDialog(
                script = flowState.script,
                onDismiss = onDismiss,
                onSave = { uiParams ->
                    onSave(uiParams.copy(runtime = flowState.runtime))
                }
            )
        }
        else -> Unit
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutomationHistoryView(
    selectedAutomation: Automation?,
    onDismiss: () -> Unit,
    viewModel: AutomationViewModel
) {
    if (selectedAutomation == null) return

    val historyLogs by produceState(emptyList<AutomationLog>(), selectedAutomation) {
        viewModel.getAutomationLogs(selectedAutomation.id).collect { value = it }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        AutomationHistorySheet(
            automationName = selectedAutomation.label,
            logs = historyLogs,
        )
    }
}

@Composable
private fun HandleLifecyclePermissions(onTrigger: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) onTrigger()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

private fun checkExactAlarmPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
    } else {
        true
    }
}

private fun launchExactAlarmSettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val intent = try {
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

sealed class AutomationFlowState : Parcelable {
    @Parcelize
    object Idle : AutomationFlowState()
    @Parcelize object PickingScript : AutomationFlowState()
    @Parcelize data class PromptingRuntime(val script: Script) : AutomationFlowState()
    @Parcelize data class Configuring(val script: Script, val runtime: ScriptRuntimeParams) : AutomationFlowState()
}

data class AutomationSaveParams(
    val scriptId: Int,
    val label: String,
    val type: AutomationType,
    val timestamp: Long,
    val interval: Long = 0,
    val days: List<Int> = emptyList(),
    val runIfMissed: Boolean = true,
    val requireWifi: Boolean = false,
    val requireCharging: Boolean = false,
    val batteryThreshold: Int = 0,
    val runtime: ScriptRuntimeParams = ScriptRuntimeParams()
)
