package io.github.swiftstagrime.termuxrunner.ui.features.customtheme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.component1
import io.github.swiftstagrime.termuxrunner.R
import io.github.swiftstagrime.termuxrunner.domain.model.CustomTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomThemeScreen(
    state: CustomThemeUiState,
    actions: CustomThemeActions,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.custom_themes_title)) },
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (state.editingTheme != null) {
                        IconButton(onClick = actions.onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                        }
                        IconButton(onClick = actions.onSave) {
                            Icon(Icons.Default.Check, contentDescription = "Save")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            ThemeSelectionRow(
                savedThemes = state.savedThemes,
                selectedThemeId = state.selectedThemeId,
                onNewTheme = actions.onNewTheme,
                onThemeSelect = actions.onThemeSelect
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            if (state.editingTheme != null) {
                ThemeEditorForm(
                    theme = state.editingTheme,
                    onNameChange = actions.onNameChange,
                    onColorChange = actions.onColorChange,
                    onToggleDarkMode = actions.onToggleDarkMode
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.select_or_create_theme_hint),
                        style = MaterialTheme.typography.bodyLarge,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeSelectionRow(
    savedThemes: List<CustomTheme>,
    selectedThemeId: Int?,
    onNewTheme: () -> Unit,
    onThemeSelect: (CustomTheme) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(colorScheme.surfaceVariant)
                        .clickable { onNewTheme() }
                        .border(1.dp, colorScheme.outline, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.new_theme_label), style = MaterialTheme.typography.labelMedium)
            }
        }

        items(savedThemes, key = { it.id }) { theme ->
            val isSelected = selectedThemeId == theme.id
            ThemeCircleItem(
                theme = theme,
                isSelected = isSelected,
                onClick = { onThemeSelect(theme) }
            )
        }
    }
}

@Composable
private fun ThemeCircleItem(
    theme: CustomTheme,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(64.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color(theme.primary))
                .clickable { onClick() }
                .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) colorScheme.primary else MaterialTheme.outlineVariant(),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = theme.name,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ThemeEditorForm(
    theme: CustomTheme,
    onNameChange: (String) -> Unit,
    onColorChange: (String, Color) -> Unit,
    onToggleDarkMode: (Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            OutlinedTextField(
                value = theme.name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.theme_name_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.dark_mode_label), style = MaterialTheme.typography.titleMedium)
                Switch(checked = theme.isDark, onCheckedChange = onToggleDarkMode)
            }
        }

        item {
            Text(
                text = stringResource(R.string.colors_section_label),
                style = MaterialTheme.typography.titleSmall,
                color = colorScheme.primary
            )
        }

        val colorFields = listOf(
            "primary" to R.string.color_primary,
            "onPrimary" to R.string.color_on_primary,
            "primaryContainer" to R.string.color_primary_container,
            "onPrimaryContainer" to R.string.color_on_primary_container,
            "secondary" to R.string.color_secondary,
            "onSecondary" to R.string.color_on_secondary,
            "background" to R.string.color_background,
            "onBackground" to R.string.color_on_background,
            "surface" to R.string.color_surface,
            "onSurface" to R.string.color_on_surface,
            "error" to R.string.color_error
        )

        items(colorFields) { (key, labelRes) ->
            val colorValue = getThemeColorByKey(theme, key)
            ColorRow(
                label = stringResource(labelRes),
                color = colorValue,
                onColorChange = { newColor -> onColorChange(key, newColor) }
            )
        }
    }
}

@Composable
private fun ColorRow(
    label: String,
    color: Color,
    onColorChange: (Color) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { showPicker = true }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)

        Surface(
            modifier = Modifier.size(40.dp),
            shape = RoundedCornerShape(8.dp),
            color = color,
            border = BorderStroke(1.dp, colorScheme.outlineVariant)
        ) {}
    }

    if (showPicker) {
        SimpleColorPicker(
            initialColor = color,
            onDismiss = { showPicker = false },
            onColorSelected = {
                onColorChange(it)
                showPicker = false
            }
        )
    }
}

@Composable
fun SimpleColorPicker(
    initialColor: Color,
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit
) {
    var hsv by remember {
        val hsvArray = FloatArray(3)
        android.graphics.Color.colorToHSV(initialColor.toArgb(), hsvArray)
        mutableStateOf(Triple(hsvArray[0], hsvArray[1], hsvArray[2]))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onColorSelected(Color(android.graphics.Color.HSVToColor(floatArrayOf(hsv.first, hsv.second, hsv.third))))
            }) {
                Text("Select")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text("Pick Color") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(android.graphics.Color.HSVToColor(floatArrayOf(hsv.first, hsv.second, hsv.third))))
                        .border(1.dp, colorScheme.outline, RoundedCornerShape(8.dp))
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Hue", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = hsv.first,
                    onValueChange = { hsv = hsv.copy(first = it) },
                    valueRange = 0f..360f,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Saturation", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = hsv.second,
                    onValueChange = { hsv = hsv.copy(second = it) },
                    valueRange = 0f..1f,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Brightness", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = hsv.third,
                    onValueChange = { hsv = hsv.copy(third = it) },
                    valueRange = 0f..1f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}

private fun getThemeColorByKey(theme: CustomTheme, key: String): Color {
    val longVal = when (key) {
        "primary" -> theme.primary
        "onPrimary" -> theme.onPrimary
        "primaryContainer" -> theme.primaryContainer
        "onPrimaryContainer" -> theme.onPrimaryContainer
        "secondary" -> theme.secondary
        "onSecondary" -> theme.onSecondary
        "background" -> theme.background
        "onBackground" -> theme.onBackground
        "surface" -> theme.surface
        "onSurface" -> theme.onSurface
        "error" -> theme.error
        else -> theme.primary
    }
    return Color(longVal.toInt())
}

@Composable
fun MaterialTheme.outlineVariant() = colorScheme.outlineVariant

@Preview(showBackground = true, name = "Custom Theme Editor")
@Composable
private fun CustomThemeScreenPreview() {
    val mockThemes = listOf(
        CustomTheme(
            id = 1, name = "Matrix", primary = 0xFF00FF00, isDark = true, onPrimary = 0xFF000000,
            primaryContainer = 0xFF000000,
            onPrimaryContainer = 0xFF000000,
            secondary = 0xFF000000,
            onSecondary = 0xFF000000,
            secondaryContainer = 0xFF000000,
            onSecondaryContainer = 0xFF000000,
            tertiary = 0xFF000000,
            onTertiary = 0xFF000000,
            tertiaryContainer = 0xFF000000,
            onTertiaryContainer = 0xFF000000,
            error = 0xFF000000,
            onError = 0xFF000000,
            errorContainer = 0xFF000000,
            onErrorContainer = 0xFF000000,
            background = 0xFF000000,
            onBackground = 0xFF000000,
            surface = 0xFF000000,
            onSurface = 0xFF000000,
            surfaceVariant = 0xFF000000,
            onSurfaceVariant = 0xFF000000,
            outline = 0xFF000000,
            outlineVariant = 0xFF000000,
            surfaceContainer = 0xFF000000,
        ),
        CustomTheme(id = 2, name = "Nord", primary = 0xFF88C0D0, isDark = true, onPrimary = 0xFF2E3440,primaryContainer = 0xFF000000,
            onPrimaryContainer = 0xFF000000,
            secondary = 0xFF000000,
            onSecondary = 0xFF000000,
            secondaryContainer = 0xFF000000,
            onSecondaryContainer = 0xFF000000,
            tertiary = 0xFF000000,
            onTertiary = 0xFF000000,
            tertiaryContainer = 0xFF000000,
            onTertiaryContainer = 0xFF000000,
            error = 0xFF000000,
            onError = 0xFF000000,
            errorContainer = 0xFF000000,
            onErrorContainer = 0xFF000000,
            background = 0xFF000000,
            onBackground = 0xFF000000,
            surface = 0xFF000000,
            onSurface = 0xFF000000,
            surfaceVariant = 0xFF000000,
            onSurfaceVariant = 0xFF000000,
            outline = 0xFF000000,
            outlineVariant = 0xFF000000,
            surfaceContainer = 0xFF000000,
        ),
        CustomTheme(id = 3, name = "Solarized", primary = 0xFFB58900, isDark = false, onPrimary = 0xFFFFFFFF,primaryContainer = 0xFF000000,
            onPrimaryContainer = 0xFF000000,
            secondary = 0xFF000000,
            onSecondary = 0xFF000000,
            secondaryContainer = 0xFF000000,
            onSecondaryContainer = 0xFF000000,
            tertiary = 0xFF000000,
            onTertiary = 0xFF000000,
            tertiaryContainer = 0xFF000000,
            onTertiaryContainer = 0xFF000000,
            error = 0xFF000000,
            onError = 0xFF000000,
            errorContainer = 0xFF000000,
            onErrorContainer = 0xFF000000,
            background = 0xFF000000,
            onBackground = 0xFF000000,
            surface = 0xFF000000,
            onSurface = 0xFF000000,
            surfaceVariant = 0xFF000000,
            onSurfaceVariant = 0xFF000000,
            outline = 0xFF000000,
            outlineVariant = 0xFF000000,
            surfaceContainer = 0xFF000000,
        )
    )

    val state = CustomThemeUiState(
        savedThemes = mockThemes,
        selectedThemeId = 2,
        editingTheme = mockThemes[1]
    )

    val actions = CustomThemeActions(
        onBack = {},
        onNewTheme = {},
        onThemeSelect = {},
        onNameChange = {},
        onColorChange = { _, _ -> },
        onSave = {},
        onDelete = {},
        onToggleDarkMode = {}
    )

    MaterialTheme {
        CustomThemeScreen(
            state = state,
            actions = actions
        )
    }
}