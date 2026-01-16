package io.github.swiftstagrime.termuxrunner.ui.features.automation.components

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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import io.github.swiftstagrime.termuxrunner.R
import io.github.swiftstagrime.termuxrunner.data.local.entity.AutomationEntity
import io.github.swiftstagrime.termuxrunner.domain.model.AutomationType
import io.github.swiftstagrime.termuxrunner.domain.model.Script
import io.github.swiftstagrime.termuxrunner.domain.util.AutomationTimeCalculator
import io.github.swiftstagrime.termuxrunner.ui.components.DayOfWeekPicker
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationConfigDialog(
    script: Script,
    onDismiss: () -> Unit,
    onSave: (label: String, type: AutomationType, timestamp: Long, interval: Long, days: List<Int>, runIfMissed: Boolean, wifi: Boolean, charging: Boolean, batteryThreshold: Int) -> Unit
) {
    var label by rememberSaveable { mutableStateOf(script.name) }
    var type by rememberSaveable { mutableStateOf(AutomationType.ONE_TIME) }
    var runIfMissed by rememberSaveable { mutableStateOf(true) }

    var selectedDate by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    var selectedHour by rememberSaveable {
        mutableIntStateOf(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))
    }
    var selectedMinute by rememberSaveable {
        mutableIntStateOf(Calendar.getInstance().get(Calendar.MINUTE))
    }

    var selectedDays by rememberSaveable { mutableStateOf(emptyList<Int>()) }

    var intervalValue by rememberSaveable { mutableStateOf("60") }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var requireWifi by rememberSaveable { mutableStateOf(false) }
    var requireCharging by rememberSaveable { mutableStateOf(false) }
    var batteryThreshold by rememberSaveable { mutableIntStateOf(0) }

    val upcomingRuns = remember(
        type,
        selectedDate,
        selectedHour,
        selectedMinute,
        intervalValue,
        selectedDays
    ) {
        val temp = AutomationEntity(
            scriptId = script.id,
            label = "",
            type = type,
            scheduledTimestamp = selectedDate,
            intervalMillis = (intervalValue.toLongOrNull() ?: 60) * 60_000L,
            daysOfWeek = selectedDays
        )
        AutomationTimeCalculator.getNextRuns(temp, 3)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .imePadding(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    text = stringResource(R.string.automation_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                ConfigSection(title = stringResource(R.string.automation_section_general)) {
                    TextField(
                        value = label,
                        onValueChange = { label = it },
                        label = { Text(stringResource(R.string.automation_label_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                }

                ConfigSection(title = stringResource(R.string.automation_section_frequency)) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        AutomationType.entries.forEachIndexed { index, automationType ->
                            SegmentedButton(
                                selected = type == automationType,
                                onClick = { type = automationType },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index,
                                    AutomationType.entries.size
                                )
                            ) {
                                Text(
                                    text = when (automationType) {
                                        AutomationType.ONE_TIME -> stringResource(R.string.automation_type_one_time)
                                        AutomationType.PERIODIC -> stringResource(R.string.automation_type_periodic)
                                        AutomationType.WEEKLY -> stringResource(R.string.automation_type_weekly)
                                    },
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }

                ConfigSection(title = stringResource(R.string.automation_section_schedule)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (type == AutomationType.ONE_TIME) {
                            DateTimeButton(
                                icon = Icons.Default.CalendarToday,
                                text = SimpleDateFormat("MMM dd", Locale.getDefault()).format(
                                    Date(
                                        selectedDate
                                    )
                                ),
                                modifier = Modifier.weight(1f),
                                onClick = { showDatePicker = true }
                            )
                        }

                        DateTimeButton(
                            icon = Icons.Default.AccessTime,
                            text = String.format(
                                Locale.getDefault(),
                                "%02d:%02d",
                                selectedHour,
                                selectedMinute
                            ),
                            modifier = Modifier.weight(1f),
                            onClick = { showTimePicker = true }
                        )
                    }

                    if (type == AutomationType.PERIODIC) {
                        TextField(
                            value = intervalValue,
                            onValueChange = { if (it.all { c -> c.isDigit() }) intervalValue = it },
                            label = { Text(stringResource(R.string.automation_interval_hint)) },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Timer,
                                    null,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )
                    }

                    if (type == AutomationType.WEEKLY) {
                        DayOfWeekPicker(
                            selectedDays = selectedDays,
                            onToggleDay = { day ->
                                selectedDays = if (selectedDays.contains(day)) {
                                    selectedDays - day
                                } else {
                                    selectedDays + day
                                }
                            }
                        )
                    }
                }

                ConfigSection(title = stringResource(R.string.automation_section_conditions)) {
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    ) {
                        AutomationOptionTile(
                            title = stringResource(R.string.automation_run_if_missed),
                            checked = runIfMissed,
                            onCheckedChange = { runIfMissed = it }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                        AutomationOptionTile(
                            title = stringResource(R.string.automation_condition_wifi),
                            checked = requireWifi,
                            onCheckedChange = { requireWifi = it }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                        AutomationOptionTile(
                            title = stringResource(R.string.automation_condition_charging),
                            checked = requireCharging,
                            onCheckedChange = { requireCharging = it }
                        )

                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = stringResource(R.string.label_battery_threshold),
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "$batteryThreshold%",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Slider(
                                value = batteryThreshold.toFloat(),
                                onValueChange = { batteryThreshold = it.toInt() },
                                valueRange = 0f..100f,
                                steps = 19
                            )
                        }
                    }
                }

                if (type != AutomationType.ONE_TIME) {
                    ConfigSection(title = stringResource(R.string.automation_section_upcoming)) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth()
                            ) {
                                upcomingRuns.forEach { time ->
                                    Row(
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Schedule,
                                            null,
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = SimpleDateFormat(
                                                "MMM dd, HH:mm",
                                                Locale.getDefault()
                                            ).format(Date(time)),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        shape = RoundedCornerShape(12.dp),
                        onClick = {
                            val calendar = Calendar.getInstance().apply {
                                timeInMillis = selectedDate
                                set(Calendar.HOUR_OF_DAY, selectedHour)
                                set(Calendar.MINUTE, selectedMinute)
                                set(Calendar.SECOND, 0)
                            }
                            onSave(
                                label.ifBlank { script.name },
                                type,
                                calendar.timeInMillis,
                                (intervalValue.toLongOrNull() ?: 60L) * 60_000L,
                                selectedDays,
                                runIfMissed,
                                requireWifi,
                                requireCharging,
                                batteryThreshold
                            )
                        }
                    ) {
                        Text(stringResource(R.string.automation_save_button))
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedDate = it }
                    showDatePicker = false
                }) { Text(stringResource(R.string.ok)) }
            }
        ) { DatePicker(state = datePickerState) }
    }

    if (showTimePicker) {
        val timePickerState =
            rememberTimePickerState(initialHour = selectedHour, initialMinute = selectedMinute)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedHour = timePickerState.hour
                    selectedMinute = timePickerState.minute
                    showTimePicker = false
                }) { Text(stringResource(R.string.ok)) }
            },
            text = { TimePicker(state = timePickerState) }
        )
    }
}

@Composable
private fun ConfigSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.2.sp
        )
        content()
    }
}

@Composable
private fun DateTimeButton(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                icon,
                null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
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
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        onClick = { onCheckedChange(!checked) },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}
