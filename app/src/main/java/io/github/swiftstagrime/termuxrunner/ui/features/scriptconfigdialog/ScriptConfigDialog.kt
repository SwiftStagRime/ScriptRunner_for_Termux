package io.github.swiftstagrime.termuxrunner.ui.features.scriptconfigdialog

import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import io.github.swiftstagrime.termuxrunner.R
import io.github.swiftstagrime.termuxrunner.domain.model.Category
import io.github.swiftstagrime.termuxrunner.domain.model.InteractionMode
import io.github.swiftstagrime.termuxrunner.domain.model.Script
import io.github.swiftstagrime.termuxrunner.ui.components.CategorySpinner
import io.github.swiftstagrime.termuxrunner.ui.components.NewCategoryDialog
import io.github.swiftstagrime.termuxrunner.ui.components.StyledTextField
import io.github.swiftstagrime.termuxrunner.ui.preview.DevicePreviews
import io.github.swiftstagrime.termuxrunner.ui.preview.configSampleScript
import io.github.swiftstagrime.termuxrunner.ui.theme.ScriptRunnerForTermuxTheme
import kotlinx.coroutines.launch

private const val ICON_SIZE = 72
private const val PADDING_STANDARD = 16
private const val SPACING_ITEM = 12
private const val ICON_CORNER_RADIUS = 12

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptConfigDialog(
    state: ScriptConfigState,
    script: Script,
    categories: List<Category>,
    isBatteryUnrestricted: Boolean,
    onDismiss: () -> Unit,
    onSave: (Script) -> Unit,
    onAddNewCategory: (String) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRequestBatteryUnrestricted: () -> Unit,
    onHeartbeatToggle: (Boolean) -> Unit,
    onProcessImage: suspend (Uri) -> String?,
) {
    val scope = rememberCoroutineScope()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri?.let {
            scope.launch {
                onProcessImage(it)?.let { savedPath -> state.iconPath = savedPath }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Scaffold(
            topBar = {
                ConfigTopBar(
                    onDismiss = onDismiss,
                    onSave = {
                        if (state.validate()) {
                            onSave(state.toScript(script))
                        }
                    }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = PADDING_STANDARD.dp),
                verticalArrangement = Arrangement.spacedBy(PADDING_STANDARD.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                item {
                    IdentitySection(state, photoPickerLauncher)
                }
                item {
                    ExecutionSection(state, categories)
                }
                item {
                    InteractivitySection(state)
                }
                item {
                    BehaviorSection(state)
                }
                item {
                    ReliabilitySection(
                        state = state,
                        isBatteryUnrestricted = isBatteryUnrestricted,
                        onRequestBatteryUnrestricted = onRequestBatteryUnrestricted,
                        onHeartbeatToggle = onHeartbeatToggle,
                        onRequestNotificationPermission = onRequestNotificationPermission
                    )
                }
                item {
                    EnvironmentSection(state)
                }
            }
        }
    }

    if (state.showAddCategoryDialog) {
        NewCategoryDialog(
            onDismiss = { state.showAddCategoryDialog = false },
            onConfirm = {
                onAddNewCategory(it)
                state.showAddCategoryDialog = false
            }
        )
    }
}

@Composable
private fun InteractivitySection(state: ScriptConfigState) {
    ConfigSection(title = stringResource(R.string.section_interactivity)) {
        InteractionModeSpinner(
            selectedMode = state.interactionMode,
            onModeSelected = { state.interactionMode = it },
        )

        if (state.interactionMode == InteractionMode.MULTI_CHOICE) {
            Spacer(modifier = Modifier.height(PADDING_STANDARD.dp))

            PresetListManager(
                title = stringResource(R.string.label_argument_presets),
                textFieldLabel = stringResource(R.string.label_argument_presets),
                presets = state.argumentPresets,
                onAdd = { state.argumentPresets.add("") },
                onRemove = { state.argumentPresets.removeAt(it) },
                onUpdate = { index, value -> state.argumentPresets[index] = value },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = PADDING_STANDARD.dp))

            PresetListManager(
                title = stringResource(R.string.label_prefix_presets),
                textFieldLabel = stringResource(R.string.label_prefix_presets),
                presets = state.prefixPresets,
                onAdd = { state.prefixPresets.add("") },
                onRemove = { state.prefixPresets.removeAt(it) },
                onUpdate = { index, value -> state.prefixPresets[index] = value },
                placeholder = "e.g. sudo",
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = PADDING_STANDARD.dp))

            KeyValuePresetManager(
                title = stringResource(R.string.label_runtime_env_vars),
                presets = state.envVarPresets,
                onAdd = { state.envVarPresets.add("=") },
                onRemove = { state.envVarPresets.removeAt(it) }
            )
        }
    }
}

@Composable
private fun BehaviorSection(state: ScriptConfigState) {
    ConfigSection(title = stringResource(R.string.section_behavior)) {
        SwitchRow(
            title = stringResource(R.string.label_bg_execution),
            description = stringResource(R.string.desc_bg_execution),
            checked = state.runInBackground,
            onCheckedChange = { state.runInBackground = it }
        )

        if (!state.runInBackground) {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            SwitchRow(
                title = stringResource(R.string.label_interactive_session),
                description = stringResource(R.string.desc_interactive_session),
                checked = state.keepOpen,
                onCheckedChange = { isChecked ->
                    state.keepOpen = isChecked
                    if (isChecked) state.notifyOnResult = false
                }
            )
        }
    }
}

@Composable
private fun KeyValuePresetManager(
    title: String,
    presets: SnapshotStateList<String>,
    onAdd: () -> Unit,
    onRemove: (Int) -> Unit,
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        presets.forEachIndexed { index, rawString ->
            val parts = rawString.split("=", limit = 2)
            val key = parts.getOrNull(0) ?: ""
            val value = parts.getOrNull(1) ?: ""

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StyledTextField(
                    value = key,
                    onValueChange = { newKey ->
                        presets[index] = "$newKey=$value"
                    },
                    label = stringResource(R.string.label_key),
                    modifier = Modifier.weight(1f),
                )

                Text(
                    "=",
                    modifier = Modifier.padding(horizontal = 8.dp),
                    style = MaterialTheme.typography.bodyLarge
                )

                // Value Field
                StyledTextField(
                    value = value,
                    onValueChange = { newValue ->
                        presets[index] = "$key=$newValue"
                    },
                    label = stringResource(R.string.label_value),
                    modifier = Modifier.weight(1.2f),
                )

                IconButton(onClick = { onRemove(index) }) {
                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                }
            }
        }

        TextButton(
            onClick = onAdd,
            modifier = Modifier.align(Alignment.Start),
        ) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.btn_add_preset))
        }
    }
}

@Composable
private fun EnvironmentSection(state: ScriptConfigState) {
    ConfigSection(title = stringResource(R.string.section_env_vars)) {
        if (state.envVars.isEmpty()) {
            Text(
                text = stringResource(R.string.empty_env_vars),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        state.envVars.forEachIndexed { index, pair ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StyledTextField(
                    value = pair.first,
                    onValueChange = { state.envVars[index] = it to pair.second },
                    label = stringResource(R.string.label_key),
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "=",
                    modifier = Modifier.padding(horizontal = 4.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                StyledTextField(
                    value = pair.second,
                    onValueChange = { state.envVars[index] = pair.first to it },
                    label = stringResource(R.string.label_value),
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { state.envVars.removeAt(index) }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.cd_remove),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        Button(
            onClick = { state.envVars.add("" to "") },
            modifier = Modifier.align(Alignment.End),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
        ) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.btn_add_variable))
        }
    }
}

@Composable
private fun ConfigSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        ElevatedCard(
            colors =
                CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                content = content,
            )
        }
    }
}

@Composable
private fun PresetListManager(
    title: String,
    textFieldLabel: String,
    presets: SnapshotStateList<String>,
    onAdd: () -> Unit,
    onRemove: (Int) -> Unit,
    onUpdate: (Int, String) -> Unit,
    placeholder: String = "",
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        presets.forEachIndexed { index, value ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StyledTextField(
                    value = value,
                    onValueChange = { onUpdate(index, it) },
                    label = textFieldLabel,
                    placeholder = { Text(placeholder) },
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { onRemove(index) }) {
                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                }
            }
        }
        TextButton(
            onClick = onAdd,
            modifier = Modifier.align(Alignment.Start),
        ) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.btn_add_preset))
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InteractionModeSpinner(
    selectedMode: InteractionMode,
    onModeSelected: (InteractionMode) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val modes = InteractionMode.entries

    val modeLabel =
        when (selectedMode) {
            InteractionMode.NONE -> stringResource(R.string.interaction_none)
            InteractionMode.TEXT_INPUT -> stringResource(R.string.interaction_text)
            InteractionMode.MULTI_CHOICE -> stringResource(R.string.interaction_multi)
        }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        StyledTextField(
            value = modeLabel,
            onValueChange = {},
            readOnly = true,
            label = stringResource(R.string.label_interaction_mode),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier =
                Modifier
                    .menuAnchor(
                        type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                        enabled = true,
                    ).fillMaxWidth(),
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            modes.forEach { mode ->
                DropdownMenuItem(
                    text = {
                        Text(
                            when (mode) {
                                InteractionMode.NONE -> stringResource(R.string.interaction_none)
                                InteractionMode.TEXT_INPUT -> stringResource(R.string.interaction_text)
                                InteractionMode.MULTI_CHOICE -> stringResource(R.string.interaction_multi)
                            },
                        )
                    },
                    onClick = {
                        onModeSelected(mode)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfigTopBar(onDismiss: () -> Unit, onSave: () -> Unit) {
    TopAppBar(
        title = { Text(stringResource(R.string.config_dialog_title)) },
        navigationIcon = {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, stringResource(R.string.cd_close))
            }
        },
        actions = {
            IconButton(onClick = onSave) {
                Icon(Icons.Default.Save, stringResource(R.string.cd_save))
            }
        }
    )
}

@Composable
private fun IdentitySection(
    state: ScriptConfigState,
    launcher: ManagedActivityResultLauncher<PickVisualMediaRequest, Uri?>
) {
    ConfigSection(title = stringResource(R.string.section_identity)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(ICON_SIZE.dp)
                    .clip(RoundedCornerShape(ICON_CORNER_RADIUS.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable {
                        launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                contentAlignment = Alignment.Center
            ) {
                if (state.iconPath != null) {
                    AsyncImage(
                        model = state.iconPath,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(Icons.Default.AddPhotoAlternate, stringResource(R.string.cd_add_icon))
                }
            }
            Spacer(modifier = Modifier.width(PADDING_STANDARD.dp))
            StyledTextField(
                value = state.name,
                onValueChange = { state.name = it; state.nameError = false },
                label = stringResource(R.string.label_script_name),
                isError = state.nameError,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ExecutionSection(
    state: ScriptConfigState,
    categories: List<Category>
) {
    ConfigSection(title = stringResource(R.string.section_execution)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StyledTextField(
                value = state.interpreter,
                onValueChange = { state.interpreter = it },
                label = stringResource(R.string.label_interpreter),
                modifier = Modifier.weight(0.6f)
            )
            StyledTextField(
                value = state.fileExtension,
                onValueChange = { state.fileExtension = it },
                label = stringResource(R.string.label_extension),
                modifier = Modifier.weight(0.4f)
            )
        }
        Spacer(modifier = Modifier.height(SPACING_ITEM.dp))
        StyledTextField(
            value = state.commandPrefix,
            onValueChange = { state.commandPrefix = it },
            label = stringResource(R.string.label_prefix),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(SPACING_ITEM.dp))
        StyledTextField(
            value = state.executionParams,
            onValueChange = { state.executionParams = it },
            label = stringResource(R.string.label_arguments),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(SPACING_ITEM.dp))
        CategorySpinner(
            categories = categories,
            selectedCategoryId = state.selectedCategoryId,
            onCategorySelected = { state.selectedCategoryId = it },
            onAddNewClick = { state.showAddCategoryDialog = true }
        )
    }
}

@Composable
private fun ReliabilitySection(
    state: ScriptConfigState,
    isBatteryUnrestricted: Boolean,
    onRequestBatteryUnrestricted: () -> Unit,
    onHeartbeatToggle: (Boolean) -> Unit,
    onRequestNotificationPermission: () -> Unit
) {
    ConfigSection(title = stringResource(R.string.reliability_monitoring)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .background(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(12.dp)
                ).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.experimental_warning),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontWeight = FontWeight.Medium
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable(!isBatteryUnrestricted) { onRequestBatteryUnrestricted() }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isBatteryUnrestricted) Icons.Default.CheckCircle else Icons.Default.BatteryAlert,
                contentDescription = null,
                tint = if (isBatteryUnrestricted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(SPACING_ITEM.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isBatteryUnrestricted) stringResource(R.string.battery_unrestricted)
                    else stringResource(R.string.battery_optimized_restricted),
                    color = if (isBatteryUnrestricted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                )
                if (!isBatteryUnrestricted) {
                    Text(
                        text = stringResource(R.string.tap_to_allow_background_activity_for_better_stability),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        SwitchRow(
            title = stringResource(R.string.auto_restart_lazarus),
            description = stringResource(R.string.restarts_script_if_termux_is_killed_by_system),
            checked = state.useHeartbeat,
            onCheckedChange = {
                state.useHeartbeat = it
                onHeartbeatToggle(it)
            }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
        SwitchRow(
            title = stringResource(R.string.execution_feedback),
            description = if (state.keepOpen) stringResource(R.string.not_available_in_interactive_mode)
            else stringResource(R.string.show_a_notification_with_the_result_success_fail_when_finished),
            checked = state.notifyOnResult,
            enabled = !state.keepOpen,
            onCheckedChange = { isChecked ->
                state.notifyOnResult = isChecked
                if (isChecked) {
                    state.keepOpen = false
                    onRequestNotificationPermission()
                }
            }
        )

        AnimatedVisibility(visible = state.useHeartbeat) {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                Text(
                    text = stringResource(R.string.advanced_timings),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StyledTextField(
                        value = state.heartbeatInterval,
                        onValueChange = { if (it.all { c -> c.isDigit() }) state.heartbeatInterval = it },
                        label = stringResource(R.string.pulse_interval_s),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    StyledTextField(
                        value = state.heartbeatTimeout,
                        onValueChange = { if (it.all { c -> c.isDigit() }) state.heartbeatTimeout = it },
                        label = stringResource(R.string.timeout_limit_s),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    text = stringResource(R.string.pulse_how_often_the_script_signals_it_is_alive_timeout_restart_if_no_signal_received_after_this_time),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    val alpha = if (enabled) 1f else 0.5f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@DevicePreviews
@Composable
private fun PreviewConfigDialogLight() {
    val sampleScript = configSampleScript
    val previewState = remember { ScriptConfigState(sampleScript) }

    ScriptRunnerForTermuxTheme {
        ScriptConfigDialog(
            state = previewState,
            script = sampleScript,
            categories = listOf(
                Category(id = 1, name = "Automation"),
                Category(id = 2, name = "Utility")
            ),
            onDismiss = {},
            onSave = {},
            onProcessImage = { null },
            onHeartbeatToggle = {},
            isBatteryUnrestricted = false,
            onRequestBatteryUnrestricted = {},
            onAddNewCategory = {},
            onRequestNotificationPermission = {}
        )
    }
}
