package io.github.swiftstagrime.termuxrunner.ui.features.automation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.swiftstagrime.termuxrunner.domain.model.Automation
import io.github.swiftstagrime.termuxrunner.domain.model.AutomationChain
import io.github.swiftstagrime.termuxrunner.domain.model.ChainCondition
import io.github.swiftstagrime.termuxrunner.domain.model.ChainStep

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationChainEditorDialog(
    chain: AutomationChain?,
    triggerAutomationLabel: String,
    availableAutomations: List<Automation>,
    onDismiss: () -> Unit,
    onSave: (AutomationChain) -> Unit,
) {
    var name by remember(chain) { mutableStateOf(chain?.name ?: "") }
    val steps = remember { mutableStateListOf<ChainStep>() }
    LaunchedEffect(chain) {
        steps.clear()
        steps.addAll(chain?.steps ?: emptyList())
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column {
            TopAppBar(
                title = {
                    Text(
                        text = "Chain Editor",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null)
                    }
                },
                actions = {
                    FilledIconButton(
                        onClick = {
                            onSave(
                                AutomationChain(
                                    id = chain?.id ?: 0,
                                    name = name.trim(),
                                    triggerAutomationId = chain?.triggerAutomationId ?: 0,
                                    steps = steps.toList(),
                                ),
                            )
                        },
                        enabled = name.trim().isNotEmpty() && steps.isNotEmpty(),
                        colors =
                            IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                disabledContainerColor =
                                    MaterialTheme.colorScheme.surfaceVariant.copy(
                                        alpha = 0.3f,
                                    ),
                            ),
                    ) {
                        Icon(Icons.Default.Save, null)
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                    ),
            )

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                        .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                ChainNameField(name) { name = it }

                TriggerSection(triggerAutomationLabel)

                StepsSection(steps, availableAutomations)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChainNameField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    ConfigSection(title = "GENERAL") {
        androidx.compose.material3.TextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text("Chain Name") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = chainEditorTextFieldColors(),
        )
    }
}

@Composable
private fun TriggerSection(triggerAutomationLabel: String) {
    ConfigSection(title = "TRIGGER") {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
        ) {
            Row(
                modifier =
                    Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    Icons.Default.Link,
                    null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = triggerAutomationLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun StepsSection(
    steps: MutableList<ChainStep>,
    availableAutomations: List<Automation>,
) {
    ConfigSection(title = "STEPS") {
        if (steps.isEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    text = "No steps configured. Tap + to add a step.",
                    modifier = Modifier.padding(24.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(steps) { index, step ->
                    ChainStepCard(
                        step = step,
                        index = index,
                        availableAutomations = availableAutomations,
                        onUpdateTargetId = { newId ->
                            steps[index] = step.copy(targetAutomationId = newId)
                        },
                        onUpdateCondition = { newCondition ->
                            steps[index] = step.copy(condition = newCondition)
                        },
                        onDelete = {
                            steps.removeAt(index)
                        },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                val nextId = availableAutomations.firstOrNull()?.id ?: 0
                steps.add(
                    ChainStep(
                        targetAutomationId = nextId,
                        condition = ChainCondition.ON_SUCCESS,
                    ),
                )
            },
            enabled = availableAutomations.isNotEmpty(),
            shape = RoundedCornerShape(12.dp),
        ) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Step")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChainStepCard(
    step: ChainStep,
    index: Int,
    availableAutomations: List<Automation>,
    onUpdateTargetId: (Int) -> Unit,
    onUpdateCondition: (ChainCondition) -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Step ${index + 1}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )

                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }

            TargetAutomationDropdown(step, availableAutomations, onUpdateTargetId)

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ChainCondition.entries.forEachIndexed { segIndex, condition ->
                    SegmentedButton(
                        selected = step.condition == condition,
                        onClick = { onUpdateCondition(condition) },
                        shape =
                            SegmentedButtonDefaults.itemShape(
                                index = segIndex,
                                count = ChainCondition.entries.size,
                            ),
                    ) {
                        Text(getConditionLabel(condition))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TargetAutomationDropdown(
    step: ChainStep,
    availableAutomations: List<Automation>,
    onTargetChange: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = availableAutomations.find { it.id == step.targetAutomationId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        OutlinedTextField(
            value = selected?.label ?: "Select target automation",
            onValueChange = {},
            readOnly = true,
            label = { Text("Target Automation") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .menuAnchor(
                        type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                        enabled = true,
                    ),
            colors = chainEditorTextFieldColors(),
            shape = RoundedCornerShape(12.dp),
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            availableAutomations.forEach { automation ->
                val isSelected = step.targetAutomationId == automation.id
                DropdownMenuItem(
                    text = {
                        Text(
                            text = automation.label,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    onClick = {
                        onTargetChange(automation.id)
                        expanded = false
                    },
                    trailingIcon = {
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun getConditionLabel(condition: ChainCondition): String =
    when (condition) {
        ChainCondition.ON_SUCCESS -> "On Success"
        ChainCondition.ON_FAILURE -> "On Failure"
        ChainCondition.ALWAYS -> "Always"
    }

@Composable
private fun chainEditorTextFieldColors() =
    TextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
    )

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
