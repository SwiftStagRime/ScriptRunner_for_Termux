package io.github.swiftstagrime.termuxrunner.ui.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.swiftstagrime.termuxrunner.R
import io.github.swiftstagrime.termuxrunner.ui.components.LanguageSelectorButton
import io.github.swiftstagrime.termuxrunner.ui.preview.DevicePreviews
import io.github.swiftstagrime.termuxrunner.ui.theme.AppTheme
import io.github.swiftstagrime.termuxrunner.ui.theme.ScriptRunnerForTermuxTheme
import io.github.swiftstagrime.termuxrunner.ui.theme.ThemeMode

private const val SURFACE_ALPHA = 0.3f
private const val SECONDARY_ALPHA = 0.7f
private const val OVERLAY_ALPHA = 0.4f
private const val DYNAMIC_ICON_ALPHA = 0.6f

private val PADDING_MEDIUM = 12.dp
private val PADDING_LARGE = 16.dp
private val PADDING_EXTRA_LARGE = 24.dp

private val RADIUS_MEDIUM = 12.dp
private val CIRCLE_SIZE = 56.dp
private val CIRCLE_ICON_SIZE = 28.dp
private val CHECK_ICON_SIZE = 20.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    selectedAccent: AppTheme,
    selectedMode: ThemeMode,
    actions: SettingsActions,
) {
    Scaffold(
        topBar = { SettingsTopBar(actions.onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(PADDING_LARGE),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                AppearanceSection(selectedAccent, selectedMode, actions)
                Spacer(modifier = Modifier.height(PADDING_EXTRA_LARGE))
                DataManagementSection(actions)
            }
            DeveloperCard(actions.onDeveloperClick)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsTopBar(onBack: () -> Unit) {
    TopAppBar(
        title = { Text(stringResource(R.string.settings_title)) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back_description),
                )
            }
        },
    )
}

@Composable
private fun AppearanceSection(
    selectedAccent: AppTheme,
    selectedMode: ThemeMode,
    actions: SettingsActions,
) {
    Text(
        text = stringResource(R.string.appearance_label),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
    Spacer(modifier = Modifier.height(8.dp))
    LanguageRow()
    Spacer(modifier = Modifier.height(PADDING_MEDIUM))

    Text(stringResource(R.string.accent_color_label), style = MaterialTheme.typography.labelLarge)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(PADDING_LARGE)) {
        items(AppTheme.entries) { accent ->
            ThemeSelectorItem(
                theme = accent,
                isSelected = selectedAccent == accent,
                onClick = { actions.onAccentChange(accent) },
            )
        }
    }

    Spacer(modifier = Modifier.height(PADDING_EXTRA_LARGE))

    Text(stringResource(R.string.display_mode_label), style = MaterialTheme.typography.labelLarge)
    DisplayModeSelector(selectedMode, actions.onModeChange)
}

@Composable
private fun LanguageRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RADIUS_MEDIUM))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = SURFACE_ALPHA))
            .padding(PADDING_LARGE),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.language_label),
            style = MaterialTheme.typography.titleMedium,
        )
        LanguageSelectorButton()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DisplayModeSelector(selectedMode: ThemeMode, onModeChange: (ThemeMode) -> Unit) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        ThemeMode.entries.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = selectedMode == mode,
                onClick = { onModeChange(mode) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = ThemeMode.entries.size,
                ),
            ) {
                Text(stringResource(mode.labelRes))
            }
        }
    }
}

@Composable
private fun DataManagementSection(actions: SettingsActions) {
    Text(
        text = stringResource(R.string.data_management_label),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PADDING_LARGE),
    ) {
        ActionButton(
            onClick = actions.onTriggerImport,
            icon = Icons.Default.Download,
            label = stringResource(R.string.import_label),
            modifier = Modifier.weight(1f)
        )
        ActionButton(
            onClick = actions.onTriggerExport,
            icon = Icons.Default.Upload,
            label = stringResource(R.string.export_label),
            modifier = Modifier.weight(1f),
            isTonal = true
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
    ActionButton(
        onClick = actions.onTriggerScriptImport,
        icon = Icons.Default.Description,
        label = stringResource(R.string.import_script_from_file),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ActionButton(
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    isTonal: Boolean = false,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(RADIUS_MEDIUM),
        colors = if (isTonal) ButtonDefaults.filledTonalButtonColors() else ButtonDefaults.buttonColors()
    ) {
        Icon(icon, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(label)
    }
}

@Composable
private fun DeveloperCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(PADDING_LARGE),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Code,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(modifier = Modifier.width(PADDING_LARGE))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.developed_by),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = SECONDARY_ALPHA),
                )
                Text(
                    text = "SwiftStagRime",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = stringResource(R.string.open_github_description),
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
fun ThemeSelectorItem(
    theme: AppTheme,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val circleColor = if (theme == AppTheme.DYNAMIC) MaterialTheme.colorScheme.primary else theme.primaryColor

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(64.dp),
    ) {
        Box(
            modifier = Modifier
                .size(CIRCLE_SIZE)
                .clip(CircleShape)
                .background(circleColor)
                .clickable { onClick() }
                .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (isSelected) {
                SelectionOverlay()
            }
            if (theme == AppTheme.DYNAMIC) {
                DynamicThemeIcon()
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = stringResource(theme.labelRes),
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
        )
    }
}

@Composable
private fun SelectionOverlay() {
    Box(
        modifier = Modifier
            .size(CIRCLE_ICON_SIZE)
            .background(color = Color.Black.copy(alpha = OVERLAY_ALPHA), shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(CHECK_ICON_SIZE),
        )
    }
}

@Composable
private fun BoxScope.DynamicThemeIcon() {
    Icon(
        imageVector = Icons.Default.AutoAwesome,
        contentDescription = null,
        tint = Color.White.copy(alpha = DYNAMIC_ICON_ALPHA),
        modifier = Modifier
            .size(16.dp)
            .align(Alignment.BottomEnd)
            .padding(bottom = 8.dp, end = 8.dp),
    )
}

@DevicePreviews
@Composable
private fun PreviewSettingsScreen() {
    ScriptRunnerForTermuxTheme {
        SettingsScreen(
            selectedAccent = AppTheme.GREEN,
            selectedMode = ThemeMode.SYSTEM,
            actions = SettingsActions(
                onAccentChange = {},
                onModeChange = {},
                onTriggerExport = {},
                onTriggerImport = {},
                onTriggerScriptImport = {},
                onDeveloperClick = {},
                onBack = {},
            ))
    }
}
