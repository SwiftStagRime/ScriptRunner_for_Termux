package io.github.swiftstagrime.termuxrunner.ui.features.onboarding

import android.content.ClipData
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.swiftstagrime.termuxrunner.R
import io.github.swiftstagrime.termuxrunner.ui.components.LanguageSelectorIcon
import io.github.swiftstagrime.termuxrunner.ui.preview.DevicePreviews
import io.github.swiftstagrime.termuxrunner.ui.theme.ScriptRunnerForTermuxTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onPermissionGranted: () -> Unit,
    onCheckAgain: () -> Unit,
    onOpenTermuxSettings: () -> Unit,
    onAlarmPermissionGranted: () -> Unit = {},
    onNotificationPermissionGranted: () -> Unit = {},
    isTermuxInstalled: Boolean,
    isPermissionGranted: Boolean,
    isBatteryUnrestricted: Boolean,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.onboarding_setup_title),
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                actions = {
                    LanguageSelectorIcon()
                },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.onboarding_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier
                        .padding(bottom = 8.dp)
                        .padding(horizontal = 8.dp),
            )

            // Step 1
            SetupStepCard(
                step = "1",
                title = stringResource(R.string.setup_step_1_title),
                description = stringResource(R.string.setup_step_1_desc),
                isDone = isTermuxInstalled,
                isError = !isTermuxInstalled,
            )

            // Step 2
            SetupStepCard(
                step = "2",
                title = stringResource(R.string.setup_step_2_title),
                description = stringResource(R.string.setup_step_2_desc),
                isDone = false,
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                CodeBlock(code = stringResource(R.string.setup_step_2_code))
                Spacer(modifier = Modifier.height(8.dp))
                CodeBlock(code = stringResource(R.string.setup_step_2_code_2))
            }

            // Step 3
            SetupStepCard(
                step = "3",
                title = stringResource(R.string.setup_step_3_title),
                description = stringResource(R.string.setup_step_3_desc),
                isDone = isPermissionGranted,
            ) {
                if (!isPermissionGranted) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onPermissionGranted,
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary,
                            ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.grant_permission_label))
                    }
                }
            }

            // Step 4
            SetupStepCard(
                step = "4",
                title = stringResource(R.string.setup_step_4_title),
                description = stringResource(R.string.setup_step_4_desc),
                isDone = false,
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onOpenTermuxSettings,
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                        ),
                ) {
                    Text(stringResource(R.string.open_termux_settings))
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            // Step 5
            SetupStepCard(
                step = "5",
                title = stringResource(R.string.setup_step_5_title),
                description = stringResource(R.string.setup_step_5_desc),
                isDone = isBatteryUnrestricted,
            ) {
                if (!isBatteryUnrestricted) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier =
                            Modifier
                                .background(
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                    RoundedCornerShape(8.dp),
                                ).padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.battery_warning_text),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onOpenTermuxSettings,
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary,
                            ),
                    ) {
                        Text(stringResource(R.string.open_termux_settings))
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            var step = 6
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                SetupStepCardOptional(
                    step = step.toString(),
                    title = stringResource(R.string.setup_step_6_title),
                    description = stringResource(R.string.setup_step_6_desc),
                    isDone = false,
                ) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onAlarmPermissionGranted,
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary,
                            ),
                    ) {
                        Text(stringResource(R.string.grant_alarm_permission_label))
                    }
                }
                step++
            }

            SetupStepCardOptional(
                step = step.toString(),
                title = stringResource(R.string.setup_step_7_title),
                description = stringResource(R.string.setup_step_7_desc),
                isDone = false,
            ) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onNotificationPermissionGranted,
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary,
                            ),
                    ) {
                        Text(stringResource(R.string.grant_notification_permission_label))
                    }
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.notification_permission_not_needed),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Button(
                onClick = onCheckAgain,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                        .testTag("onboarding_complete_button")
                        .windowInsetsPadding(WindowInsets.safeDrawing),
            ) {
                Text(
                    text = stringResource(R.string.finish_setup_label),
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
fun SetupStepCard(
    step: String,
    title: String,
    description: String,
    isDone: Boolean,
    isError: Boolean = false,
    content: @Composable () -> Unit = {},
) {
    val borderColor =
        when {
            isDone -> MaterialTheme.colorScheme.primary
            isError -> MaterialTheme.colorScheme.error
            else -> Color.Transparent
        }

    val containerColor =
        when {
            isDone -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            isError -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        }

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(16.dp),
        border = if (isDone || isError) BorderStroke(1.dp, borderColor) else null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = step,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    if (isDone) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = stringResource(R.string.done_description),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    } else if (isError) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = stringResource(R.string.error_description),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                content()
            }
        }
    }
}

@Composable
fun SetupStepCardOptional(
    step: String,
    title: String,
    description: String,
    isDone: Boolean,
    content: @Composable () -> Unit = {},
) {
    val borderColor =
        when {
            isDone -> MaterialTheme.colorScheme.primary
            else -> Color.Transparent
        }

    val containerColor =
        when {
            isDone -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        }

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(16.dp),
        border = if (isDone) BorderStroke(1.dp, borderColor) else null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = step,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (isDone) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = stringResource(R.string.done_description),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                content()
            }
        }
    }
}

@Composable
fun CodeBlock(
    code: String,
    onCopy: (String) -> Unit = {},
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SelectionContainer(modifier = Modifier.weight(1f)) {
                Text(
                    text = code,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                )
            }
            IconButton(onClick = {
                scope.launch {
                    val clipData = ClipData.newPlainText("Termux Script", code)
                    clipboard.setClipEntry(ClipEntry(clipData))
                    onCopy(code)
                }
            }) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = stringResource(R.string.copy_clipboard_description),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@DevicePreviews
@Composable
private fun PreviewOnboardingScreen() {
    ScriptRunnerForTermuxTheme {
        OnboardingScreen(
            onPermissionGranted = {},
            onCheckAgain = {},
            isTermuxInstalled = true,
            isPermissionGranted = false,
            onOpenTermuxSettings = {},
            onAlarmPermissionGranted = {},
            onNotificationPermissionGranted = {},
            isBatteryUnrestricted = true,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewStepCard() {
    ScriptRunnerForTermuxTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            SetupStepCard(
                step = "1",
                title = "Step Title",
                description = "This is a description of the step.",
                isDone = true,
            )
            Spacer(modifier = Modifier.height(8.dp))
            SetupStepCard(
                step = "2",
                title = "Pending Step",
                description = "This step is not done yet.",
                isDone = false,
            )
        }
    }
}
