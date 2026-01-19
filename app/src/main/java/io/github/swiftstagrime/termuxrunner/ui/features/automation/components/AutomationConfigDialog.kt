package io.github.swiftstagrime.termuxrunner.ui.features.automation.components

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import io.github.swiftstagrime.termuxrunner.R
import io.github.swiftstagrime.termuxrunner.data.local.entity.AutomationEntity
import io.github.swiftstagrime.termuxrunner.domain.model.AutomationType
import io.github.swiftstagrime.termuxrunner.domain.model.Script
import io.github.swiftstagrime.termuxrunner.domain.util.AutomationTimeCalculator
import io.github.swiftstagrime.termuxrunner.ui.components.DayOfWeekPicker
import io.github.swiftstagrime.termuxrunner.ui.features.automation.AutomationConfigState
import io.github.swiftstagrime.termuxrunner.ui.features.automation.AutomationSaveParams
import io.github.swiftstagrime.termuxrunner.ui.preview.sampleScripts
import io.github.swiftstagrime.termuxrunner.ui.theme.ScriptRunnerForTermuxTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val MILLIS_IN_MINUTE = 60_000L
private const val DEFAULT_INTERVAL_MINUTES = 60L
private const val MAX_BATTERY_LEVEL = 100f
private const val SLIDER_STEPS = 19

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationConfigDialog(
    script: Script,
    onDismiss: () -> Unit,
    onSave: (AutomationSaveParams) -> Unit,
) {
    val state = rememberAutomationConfigState(script)
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).imePadding(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Column(
                modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Text(
                    text = stringResource(R.string.automation_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )

                GeneralSection(state)
                FrequencySection(state)
                ScheduleSection(state, onShowDate = { showDatePicker = true }, onShowTime = { showTimePicker = true })
                ConditionsSection(state)

                if (state.type != AutomationType.ONE_TIME) {
                    UpcomingRunsSection(script, state)
                }

                DialogActionButtons(
                    onDismiss = onDismiss,
                    onConfirm = {
                        onSave(state.toSaveParams(script.id))
                    }
                )
            }
        }
    }

    if (showDatePicker) {
        AutomationDatePicker(state) { showDatePicker = false }
    }

    if (showTimePicker) {
        AutomationTimePicker(state) { showTimePicker = false }
    }
}

@Composable
private fun GeneralSection(state: AutomationConfigState) {
    ConfigSection(title = stringResource(R.string.automation_section_general)) {
        TextField(
            value = state.label,
            onValueChange = { state.label = it },
            label = { Text(stringResource(R.string.automation_label_hint)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = transparentTextFieldColors(),
        )
    }
}

@Composable
private fun FrequencySection(state: AutomationConfigState) {
    ConfigSection(title = stringResource(R.string.automation_section_frequency)) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            AutomationType.entries.forEachIndexed { index, automationType ->
                SegmentedButton(
                    selected = state.type == automationType,
                    onClick = { state.type = automationType },
                    shape = SegmentedButtonDefaults.itemShape(index, AutomationType.entries.size),
                ) {
                    Text(
                        text = getAutomationTypeLabel(automationType),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduleSection(
    state: AutomationConfigState,
    onShowDate: () -> Unit,
    onShowTime: () -> Unit
) {
    ConfigSection(title = stringResource(R.string.automation_section_schedule)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.type == AutomationType.ONE_TIME) {
                DateTimeButton(
                    icon = Icons.Default.CalendarToday,
                    text = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(state.selectedDate)),
                    modifier = Modifier.weight(1f),
                    onClick = onShowDate,
                )
            }
            DateTimeButton(
                icon = Icons.Default.AccessTime,
                text = String.format(Locale.getDefault(), "%02d:%02d", state.selectedHour, state.selectedMinute),
                modifier = Modifier.weight(1f),
                onClick = onShowTime,
            )
        }

        if (state.type == AutomationType.PERIODIC) {
            TextField(
                value = state.intervalValue,
                onValueChange = { if (it.all { c -> c.isDigit() }) state.intervalValue = it },
                label = { Text(stringResource(R.string.automation_interval_hint)) },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Timer, null, modifier = Modifier.size(20.dp)) },
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = transparentTextFieldColors(),
            )
        }

        if (state.type == AutomationType.WEEKLY) {
            DayOfWeekPicker(
                selectedDays = state.selectedDays,
                onToggleDay = { day ->
                    state.selectedDays = if (state.selectedDays.contains(day)) {
                        state.selectedDays - day
                    } else {
                        state.selectedDays + day
                    }
                },
            )
        }
    }
}

@Composable
private fun ConditionsSection(state: AutomationConfigState) {
    ConfigSection(title = stringResource(R.string.automation_section_conditions)) {
        Column(
            modifier = Modifier.clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLowest),
        ) {
            AutomationOptionTile(
                title = stringResource(R.string.automation_run_if_missed),
                checked = state.runIfMissed,
                onCheckedChange = { state.runIfMissed = it },
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = dividerColor())
            AutomationOptionTile(
                title = stringResource(R.string.automation_condition_wifi),
                checked = state.requireWifi,
                onCheckedChange = { state.requireWifi = it },
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = dividerColor())
            AutomationOptionTile(
                title = stringResource(R.string.automation_condition_charging),
                checked = state.requireCharging,
                onCheckedChange = { state.requireCharging = it },
            )

            BatteryThresholdSlider(state)
        }
    }
}

@Composable
private fun UpcomingRunsSection(script: Script, state: AutomationConfigState) {
    val upcomingRuns = remember(state.type, state.selectedDate, state.selectedHour, state.selectedMinute, state.intervalValue, state.selectedDays) {
        val temp = AutomationEntity(
            scriptId = script.id,
            label = "",
            type = state.type,
            scheduledTimestamp = state.selectedDate,
            intervalMillis = (state.intervalValue.toLongOrNull() ?: DEFAULT_INTERVAL_MINUTES) * MILLIS_IN_MINUTE,
            daysOfWeek = state.selectedDays,
        )
        AutomationTimeCalculator.getNextRuns(temp, 3)
    }

    ConfigSection(title = stringResource(R.string.automation_section_upcoming)) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
        ) {
            Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                upcomingRuns.forEach { time ->
                    UpcomingRunRow(time)
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutomationDatePicker(
    state: AutomationConfigState,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = state.selectedDate)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { state.selectedDate = it }
                onDismiss()
            }) { Text(stringResource(R.string.ok)) }
        },
    ) {
        DatePicker(state = datePickerState)
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutomationTimePicker(
    state: AutomationConfigState,
    onDismiss: () -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = state.selectedHour,
        initialMinute = state.selectedMinute
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedHour = timePickerState.hour
                state.selectedMinute = timePickerState.minute
                onDismiss()
            }) { Text(stringResource(R.string.ok)) }
        },
        text = { TimePicker(state = timePickerState) },
    )
}
@Composable
private fun UpcomingRunRow(time: Long) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.Schedule,
            null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(time)),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}
@Composable
private fun BatteryThresholdSlider(state: AutomationConfigState) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.label_battery_threshold),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${state.batteryThreshold}%",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }
        Slider(
            value = state.batteryThreshold.toFloat(),
            onValueChange = { state.batteryThreshold = it.toInt() },
            valueRange = 0f..MAX_BATTERY_LEVEL,
            steps = SLIDER_STEPS,
        )
    }
}

@Composable
private fun DialogActionButtons(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            shape = RoundedCornerShape(12.dp),
            onClick = onConfirm,
        ) {
            Text(stringResource(R.string.automation_save_button))
        }
    }
}

@Composable
fun rememberAutomationConfigState(script: Script): AutomationConfigState {
    return rememberSaveable(script, saver = AutomationConfigState.Saver(script)) {
        AutomationConfigState(script)
    }
}

@Composable
private fun transparentTextFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
)

@Composable
private fun dividerColor() = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)

@Composable
private fun getAutomationTypeLabel(type: AutomationType) = when (type) {
    AutomationType.ONE_TIME -> stringResource(R.string.automation_type_one_time)
    AutomationType.PERIODIC -> stringResource(R.string.automation_type_periodic)
    AutomationType.WEEKLY -> stringResource(R.string.automation_type_weekly)
}

@Composable
private fun ConfigSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.2.sp,
        )
        content()
    }
}

@Composable
private fun DateTimeButton(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            Icon(
                icon,
                null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(text, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun AutomationOptionTile(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Surface(
        onClick = { onCheckedChange(!checked) },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        }
    }
}

@Preview(
    name = "One-Time Automation - Night",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun PreviewAutomationConfigOneTime() {
    ScriptRunnerForTermuxTheme {
        Surface {
            AutomationConfigDialog(
                script = sampleScripts[0],
                onDismiss = {},
                onSave = {}
            )
        }
    }
}
