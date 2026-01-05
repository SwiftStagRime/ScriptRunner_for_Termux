package io.github.swiftstagrime.termuxrunner.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.ShortText
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Terminal
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
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import io.github.swiftstagrime.termuxrunner.R
import io.github.swiftstagrime.termuxrunner.domain.model.Category
import io.github.swiftstagrime.termuxrunner.domain.model.InteractionMode
import io.github.swiftstagrime.termuxrunner.domain.model.Script
import io.github.swiftstagrime.termuxrunner.ui.preview.DevicePreviews
import io.github.swiftstagrime.termuxrunner.ui.preview.configSampleScript
import io.github.swiftstagrime.termuxrunner.ui.theme.ScriptRunnerForTermuxTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptConfigDialog(
    script: Script,
    onDismiss: () -> Unit,
    onSave: (Script) -> Unit,
    categories: List<Category>,
    onAddNewCategory: (String) -> Unit,
    isBatteryUnrestricted: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onRequestBatteryUnrestricted: () -> Unit,
    onHeartbeatToggle: (Boolean) -> Unit,
    onProcessImage: suspend (Uri) -> String?
) {
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(script.name) }
    var nameError by remember { mutableStateOf(false) }
    var interpreter by remember { mutableStateOf(script.interpreter) }
    var args by remember { mutableStateOf(script.executionParams) }
    var runInBackground by remember { mutableStateOf(script.runInBackground) }
    var keepOpen by remember { mutableStateOf(script.keepSessionOpen) }
    var fileExtension by remember { mutableStateOf(script.fileExtension) }
    var commandPrefix by remember { mutableStateOf(script.commandPrefix) }
    var currentIconPath by remember { mutableStateOf(script.iconPath) }
    var useHeartbeat by remember { mutableStateOf(script.useHeartbeat) }
    var selectedCategoryId by remember { mutableStateOf(script.categoryId) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var notifyOnResult by remember { mutableStateOf(script.notifyOnResult) }
    var heartbeatTimeoutStr by remember { mutableStateOf((script.heartbeatTimeout / 1000).toString()) }
    var heartbeatIntervalStr by remember { mutableStateOf((script.heartbeatInterval / 1000).toString()) }
    var interactionMode by remember { mutableStateOf(script.interactionMode) }
    val argPresetsList = remember { script.argumentPresets.toMutableStateList() }
    val prefixPresetsList = remember { script.prefixPresets.toMutableStateList() }
    val envVarPresets = remember { script.envVarPresets.toMutableStateList() }

    val envVarsList = remember {
        script.envVars.entries.map { it.key to it.value }.toMutableStateList()
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val savedPath = onProcessImage(uri)
                if (savedPath != null) currentIconPath = savedPath
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.config_dialog_title)) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.primary
                    ),
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.cd_close)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            if (name.isBlank()) {
                                nameError = true
                            } else {
                                val updated = script.copy(
                                    name = name,
                                    interpreter = interpreter,
                                    executionParams = args,
                                    fileExtension = fileExtension,
                                    commandPrefix = commandPrefix,
                                    runInBackground = runInBackground,
                                    keepSessionOpen = keepOpen,
                                    iconPath = currentIconPath,
                                    envVars = envVarsList.toMap(),
                                    useHeartbeat = useHeartbeat,
                                    heartbeatTimeout = heartbeatTimeoutStr.toLong() * 1000,
                                    heartbeatInterval = heartbeatIntervalStr.toLong() * 1000,
                                    categoryId = selectedCategoryId,
                                    notifyOnResult = notifyOnResult,
                                    interactionMode = interactionMode,
                                    argumentPresets = argPresetsList.toList(),
                                    prefixPresets = prefixPresetsList.toList(),
                                )
                                onSave(updated)
                            }
                        }) {
                            Icon(Icons.Default.Save, stringResource(R.string.cd_save))
                        }
                    }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    ConfigSection(title = stringResource(R.string.section_identity)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (currentIconPath != null) {
                                    AsyncImage(
                                        model = currentIconPath,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.AddPhotoAlternate,
                                        contentDescription = stringResource(R.string.cd_add_icon),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))

                            StyledTextField(
                                value = name,
                                onValueChange = {
                                    name = it
                                    if (it.isNotBlank()) nameError = false
                                },
                                label = stringResource(R.string.label_script_name),
                                placeholder = { Text(stringResource(R.string.placeholder_script_name)) },
                                modifier = Modifier.weight(1f),
                                isError = nameError,
                                supportingText = if (nameError) {
                                    { Text(stringResource(R.string.error_empty_name)) }
                                } else null
                            )
                        }
                    }
                }

                item {
                    ConfigSection(title = stringResource(R.string.section_execution)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StyledTextField(
                                value = interpreter,
                                onValueChange = { interpreter = it },
                                label = stringResource(R.string.label_interpreter),
                                placeholder = { Text(stringResource(R.string.placeholder_interpreter)) },
                                modifier = Modifier.weight(0.6f),
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Terminal,
                                        null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            )
                            StyledTextField(
                                value = fileExtension,
                                onValueChange = { fileExtension = it },
                                label = stringResource(R.string.label_extension),
                                placeholder = { Text(stringResource(R.string.placeholder_extension)) },
                                modifier = Modifier.weight(0.4f)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        StyledTextField(
                            value = commandPrefix,
                            onValueChange = { commandPrefix = it },
                            label = stringResource(R.string.label_prefix),
                            placeholder = { Text(stringResource(R.string.placeholder_prefix)) },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = {
                                Icon(
                                    Icons.AutoMirrored.Filled.ShortText,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        StyledTextField(
                            value = args,
                            onValueChange = { args = it },
                            label = stringResource(R.string.label_arguments),
                            placeholder = { Text(stringResource(R.string.placeholder_arguments)) },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = {
                                Icon(
                                    Icons.Default.DataObject,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        CategorySpinner(
                            categories = categories,
                            selectedCategoryId = selectedCategoryId,
                            onCategorySelected = { selectedCategoryId = it },
                            onAddNewClick = { showAddCategoryDialog = true }
                        )
                    }
                }

                item {
                    ConfigSection(title = stringResource(R.string.section_interactivity)) {
                        InteractionModeSpinner(
                            selectedMode = interactionMode,
                            onModeSelected = { interactionMode = it }
                        )

                        if (
                            interactionMode == InteractionMode.MULTI_CHOICE) {

                            Spacer(modifier = Modifier.height(16.dp))
                            PresetListManager(
                                title = stringResource(R.string.label_argument_presets),
                                textFieldLabel = stringResource(R.string.label_argument_presets),
                                presets = argPresetsList,
                                onAdd = { argPresetsList.add("") },
                                onRemove = { argPresetsList.removeAt(it) },
                                onUpdate = { index, value -> argPresetsList[index] = value }
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                            PresetListManager(
                                title = stringResource(R.string.label_prefix_presets),
                                textFieldLabel = stringResource(R.string.label_prefix_presets),
                                presets = prefixPresetsList,
                                onAdd = { prefixPresetsList.add("") },
                                onRemove = { prefixPresetsList.removeAt(it) },
                                onUpdate = { index, value -> prefixPresetsList[index] = value },
                                placeholder = "e.g. sudo"
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                            PresetListManager(
                                title = stringResource(R.string.label_runtime_env_vars),
                                textFieldLabel = stringResource(R.string.label_env_key),
                                presets = envVarPresets,
                                onAdd = { envVarPresets.add("") },
                                onRemove = { envVarPresets.removeAt(it) },
                                onUpdate = { index, value -> envVarPresets[index] = value },
                                placeholder = "e.g. API_KEY"
                            )
                        }
                    }
                }

                item {
                    ConfigSection(title = stringResource(R.string.section_behavior)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.label_bg_execution),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.desc_bg_execution),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = runInBackground,
                                onCheckedChange = { runInBackground = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }

                        if (!runInBackground) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.label_interactive_session),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = stringResource(R.string.desc_interactive_session),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = keepOpen,
                                    onCheckedChange = { isChecked ->
                                        keepOpen = isChecked
                                        if (isChecked) {
                                            notifyOnResult = false
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                )
                            }
                        }
                    }
                }

                //Hack to try and monitor service
                item {
                    ConfigSection(title = stringResource(R.string.reliability_monitoring)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = stringResource(R.string.experimental_warning),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.auto_restart_lazarus),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.restarts_script_if_termux_is_killed_by_system),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = useHeartbeat,
                                onCheckedChange = {
                                    useHeartbeat = it
                                    onHeartbeatToggle(it)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.execution_feedback),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (keepOpen) MaterialTheme.colorScheme.onSurface.copy(
                                        alpha = 0.5f
                                    ) else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (keepOpen) stringResource(R.string.not_available_in_interactive_mode) else stringResource(
                                        R.string.show_a_notification_with_the_result_success_fail_when_finished
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = notifyOnResult,
                                enabled = !keepOpen,
                                onCheckedChange = { isChecked ->
                                    notifyOnResult = isChecked
                                    if (isChecked) {
                                        keepOpen = false
                                        onRequestNotificationPermission()
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(enabled = !isBatteryUnrestricted) { onRequestBatteryUnrestricted() }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isBatteryUnrestricted) Icons.Default.CheckCircle else Icons.Default.BatteryAlert,
                                contentDescription = null,
                                tint = if (isBatteryUnrestricted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isBatteryUnrestricted) stringResource(R.string.battery_unrestricted) else stringResource(
                                        R.string.battery_optimized_restricted
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
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
                            if (!isBatteryUnrestricted) {
                                Icon(
                                    Icons.AutoMirrored.Filled.OpenInNew,
                                    null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = useHeartbeat,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(modifier = Modifier.padding(top = 12.dp)) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(bottom = 12.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )

                                Text(
                                    text = stringResource(R.string.advanced_timings),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    StyledTextField(
                                        value = heartbeatIntervalStr,
                                        onValueChange = {
                                            if (it.all { char -> char.isDigit() }) heartbeatIntervalStr =
                                                it
                                        },
                                        label = stringResource(R.string.pulse_interval_s),
                                        placeholder = { Text("10") },
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                    )

                                    StyledTextField(
                                        value = heartbeatTimeoutStr,
                                        onValueChange = {
                                            if (it.all { char -> char.isDigit() }) heartbeatTimeoutStr =
                                                it
                                        },
                                        label = stringResource(R.string.timeout_limit_s),
                                        placeholder = { Text("30") },
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                    )
                                }
                                Text(
                                    text = stringResource(R.string.pulse_how_often_the_script_signals_it_is_alive_timeout_restart_if_no_signal_received_after_this_time),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(top = 8.dp, start = 4.dp),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                item {
                    ConfigSection(title = stringResource(R.string.section_env_vars)) {
                        if (envVarsList.isEmpty()) {
                            Text(
                                text = stringResource(R.string.empty_env_vars),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        envVarsList.forEachIndexed { index, pair ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StyledTextField(
                                    value = pair.first,
                                    onValueChange = { envVarsList[index] = it to pair.second },
                                    label = stringResource(R.string.label_key),
                                    placeholder = { Text(stringResource(R.string.placeholder_key)) },
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    "=",
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                StyledTextField(
                                    value = pair.second,
                                    onValueChange = { envVarsList[index] = pair.first to it },
                                    label = stringResource(R.string.label_value),
                                    placeholder = { Text(stringResource(R.string.placeholder_value)) },
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { envVarsList.removeAt(index) }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.cd_remove),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                        Button(
                            onClick = { envVarsList.add("" to "") },
                            modifier = Modifier.align(Alignment.End),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.btn_add_variable))
                        }
                    }
                }
            }
        }
    }

    if (showAddCategoryDialog) {
        NewCategoryDialog(
            onDismiss = { showAddCategoryDialog = false },
            onConfirm = { newName ->
                onAddNewCategory(newName)
                showAddCategoryDialog = false
            }
        )
    }
}

@Composable
fun ConfigSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        ElevatedCard(
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                content = content
            )
        }
    }
}

@Composable
fun PresetListManager(
    title: String,
    textFieldLabel: String,
    presets: SnapshotStateList<String>,
    onAdd: () -> Unit,
    onRemove: (Int) -> Unit,
    onUpdate: (Int, String) -> Unit,
    placeholder: String = ""
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        presets.forEachIndexed { index, value ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StyledTextField(
                    value = value,
                    onValueChange = { onUpdate(index, it) },
                    label = textFieldLabel,
                    placeholder = { Text(placeholder) },
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { onRemove(index) }) {
                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                }
            }
        }
        TextButton(
            onClick = onAdd,
            modifier = Modifier.align(Alignment.Start)
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
    onModeSelected: (InteractionMode) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val modes = InteractionMode.entries

    val modeLabel = when (selectedMode) {
        InteractionMode.NONE -> stringResource(R.string.interaction_none)
        InteractionMode.TEXT_INPUT -> stringResource(R.string.interaction_text)
        InteractionMode.MULTI_CHOICE -> stringResource(R.string.interaction_multi)
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        StyledTextField(
            value = modeLabel,
            onValueChange = {},
            readOnly = true,
            label = stringResource(R.string.label_interaction_mode),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                    enabled = true)
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            modes.forEach { mode ->
                DropdownMenuItem(
                    text = {
                        Text(
                            when (mode) {
                                InteractionMode.NONE -> stringResource(R.string.interaction_none)
                                InteractionMode.TEXT_INPUT -> stringResource(R.string.interaction_text)
                                InteractionMode.MULTI_CHOICE -> stringResource(R.string.interaction_multi)
                            }
                        )
                    },
                    onClick = {
                        onModeSelected(mode)
                        expanded = false
                    }
                )
            }
        }
    }
}

@DevicePreviews
@Composable
private fun PreviewConfigDialogLight() {
    ScriptRunnerForTermuxTheme {
        ScriptConfigDialog(
            script = configSampleScript,
            onDismiss = {},
            onSave = {},
            onProcessImage = { null },
            onHeartbeatToggle = {},
            isBatteryUnrestricted = false,
            onRequestBatteryUnrestricted = {},
            categories = emptyList(),
            onAddNewCategory = {},
            onRequestNotificationPermission = {}
        )
    }
}
