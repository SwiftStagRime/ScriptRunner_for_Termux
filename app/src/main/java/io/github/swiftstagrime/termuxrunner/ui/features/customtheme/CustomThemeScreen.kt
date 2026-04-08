package io.github.swiftstagrime.termuxrunner.ui.features.customtheme

import android.annotation.SuppressLint
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import io.github.swiftstagrime.termuxrunner.R
import io.github.swiftstagrime.termuxrunner.domain.model.CustomTheme
import io.github.swiftstagrime.termuxrunner.ui.components.StyledTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomThemeScreen(
    state: CustomThemeUiState,
    actions: CustomThemeActions,
) {
    val outerBackgroundColor = MaterialTheme.colorScheme.surface
    val sheetContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest

    Scaffold(
        containerColor = outerBackgroundColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.custom_themes_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
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
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                    ),
            )
        },
    ) { padding ->
        Surface(
            modifier =
                Modifier
                    .padding(padding)
                    .padding(horizontal = 8.dp)
                    .padding(bottom = 8.dp)
                    .fillMaxSize(),
            color = sheetContainerColor,
            shape = RoundedCornerShape(32.dp),
            shadowElevation = 1.dp,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(vertical = 16.dp),
            ) {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ThemeSelectionRow(
                        savedThemes = state.savedThemes,
                        selectedThemeId = state.selectedThemeId,
                        onNewTheme = actions.onNewTheme,
                        onThemeSelect = actions.onThemeSelect,
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp, horizontal = 24.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                )

                if (state.editingTheme != null) {
                    ThemeEditorForm(
                        theme = state.editingTheme,
                        onNameChange = actions.onNameChange,
                        onColorChange = actions.onColorChange,
                        onToggleDarkMode = actions.onToggleDarkMode,
                    )
                } else {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(bottom = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.select_or_create_theme_hint),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
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
    onThemeSelect: (CustomTheme) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier =
                        Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(colorScheme.surfaceVariant)
                            .clickable { onNewTheme() }
                            .border(1.dp, colorScheme.outline, CircleShape),
                    contentAlignment = Alignment.Center,
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
                onClick = { onThemeSelect(theme) },
            )
        }
    }
}

@Composable
private fun ThemeCircleItem(
    theme: CustomTheme,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(64.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(theme.primary))
                    .clickable { onClick() }
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) colorScheme.primary else MaterialTheme.outlineVariant(),
                        shape = CircleShape,
                    ),
            contentAlignment = Alignment.Center,
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
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ThemeEditorForm(
    theme: CustomTheme,
    onNameChange: (String) -> Unit,
    onColorChange: (String, Color) -> Unit,
    onToggleDarkMode: (Boolean) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            StyledTextField(
                value = theme.name,
                onValueChange = onNameChange,
                label = stringResource(R.string.theme_name_label),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(R.string.dark_mode_label), style = MaterialTheme.typography.titleMedium)
                Switch(checked = theme.isDark, onCheckedChange = onToggleDarkMode)
            }
        }

        item { ColorGroupHeader(stringResource(R.string.color_primary)) }
        val primaryGroup =
            listOf(
                "primary" to R.string.color_primary,
                "onPrimary" to R.string.color_on_primary,
                "primaryContainer" to R.string.color_primary_container,
                "onPrimaryContainer" to R.string.color_on_primary_container,
            )
        items(primaryGroup) { (key, label) ->
            ColorRow(stringResource(label), getThemeColorByKey(theme, key)) { onColorChange(key, it) }
        }

        item { ColorGroupHeader(stringResource(R.string.color_secondary)) }
        val secondaryGroup =
            listOf(
                "secondary" to R.string.color_secondary,
                "onSecondary" to R.string.color_on_secondary,
                "secondaryContainer" to R.string.color_secondary_container,
                "onSecondaryContainer" to R.string.color_on_secondary_container,
            )
        items(secondaryGroup) { (key, label) ->
            ColorRow(stringResource(label), getThemeColorByKey(theme, key)) { onColorChange(key, it) }
        }

        item { ColorGroupHeader(stringResource(R.string.color_tertiary)) }
        val tertiaryGroup =
            listOf(
                "tertiary" to R.string.color_tertiary,
                "onTertiary" to R.string.color_on_tertiary,
                "tertiaryContainer" to R.string.color_tertiary_container,
                "onTertiaryContainer" to R.string.color_on_tertiary_container,
            )
        items(tertiaryGroup) { (key, label) ->
            ColorRow(stringResource(label), getThemeColorByKey(theme, key)) { onColorChange(key, it) }
        }

        item { ColorGroupHeader(stringResource(R.string.surfaces_background)) }
        val surfaceGroup =
            listOf(
                "background" to R.string.color_background,
                "onBackground" to R.string.color_on_background,
                "surface" to R.string.color_surface,
                "onSurface" to R.string.color_on_surface,
                "surfaceVariant" to R.string.color_surface_variant,
                "onSurfaceVariant" to R.string.color_on_surface_variant,
                "surfaceContainer" to R.string.color_surface_container,
                "surfaceContainerLowest" to R.string.surface_container_lowest,
            )
        items(surfaceGroup) { (key, label) ->
            ColorRow(stringResource(label), getThemeColorByKey(theme, key)) { onColorChange(key, it) }
        }

        item { ColorGroupHeader(stringResource(R.string.utility)) }
        val utilGroup =
            listOf(
                "outline" to R.string.color_outline,
                "outlineVariant" to R.string.color_outline_variant,
                "error" to R.string.color_error,
                "errorContainer" to R.string.color_error_container,
                "onError" to R.string.color_on_error,
                "onErrorContainer" to R.string.color_on_error_container,
            )
        items(utilGroup) { (key, label) ->
            ColorRow(stringResource(label), getThemeColorByKey(theme, key)) { onColorChange(key, it) }
        }
    }
}

@Composable
private fun ColorGroupHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun ColorRow(
    label: String,
    color: Color,
    onColorChange: (Color) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { showPicker = true }
                .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)

        Surface(
            modifier = Modifier.size(40.dp),
            shape = RoundedCornerShape(8.dp),
            color = color,
            border = BorderStroke(1.dp, colorScheme.outlineVariant),
        ) {}
    }

    if (showPicker) {
        SimpleColorPicker(
            initialColor = color,
            onDismiss = { showPicker = false },
            onColorSelected = {
                onColorChange(it)
                showPicker = false
            },
        )
    }
}

@SuppressLint("UseKtx")
@Composable
fun SimpleColorPicker(
    initialColor: Color,
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit,
) {
    var hsv by remember {
        val hsvArray = FloatArray(3)
        android.graphics.Color.colorToHSV(initialColor.toArgb(), hsvArray)
        mutableStateOf(Triple(hsvArray[0], hsvArray[1], hsvArray[2]))
    }

    val currentColor =
        remember(hsv) {
            Color(android.graphics.Color.HSVToColor(floatArrayOf(hsv.first, hsv.second, hsv.third)))
        }

    var hexText by remember {
        mutableStateOf(String.format("%06X", (0xFFFFFF and currentColor.toArgb())))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onColorSelected(currentColor) }) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
        title = { Text(stringResource(R.string.pick_color_title)) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(currentColor)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
                )

                StyledTextField(
                    value = hexText,
                    label = stringResource(R.string.hex_code),
                    placeholder = { Text("RRGGBB") },
                    onValueChange = { newHex ->
                        val filtered = newHex.filter { it.isDigit() || it.uppercaseChar() in 'A'..'F' }.take(6)
                        hexText = filtered

                        if (filtered.length == 6) {
                            try {
                                val parsedColor = "#$filtered".toColorInt()
                                val hsvArray = FloatArray(3)
                                android.graphics.Color.colorToHSV(parsedColor, hsvArray)
                                hsv = Triple(hsvArray[0], hsvArray[1], hsvArray[2])
                            } catch (_: Exception) {
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Tag, contentDescription = null) },
                    keyboardOptions =
                        KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Done,
                        ),
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.hue), style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = hsv.first,
                        onValueChange = {
                            hsv = hsv.copy(first = it)
                            hexText = String.format("%06X", (0xFFFFFF and currentColor.toArgb()))
                        },
                        valueRange = 0f..360f,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Text(stringResource(R.string.saturation), style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = hsv.second,
                        onValueChange = {
                            hsv = hsv.copy(second = it)
                            hexText = String.format("%06X", (0xFFFFFF and currentColor.toArgb()))
                        },
                        valueRange = 0f..1f,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Text(stringResource(R.string.brightness), style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = hsv.third,
                        onValueChange = {
                            hsv = hsv.copy(third = it)
                            hexText = String.format("%06X", (0xFFFFFF and currentColor.toArgb()))
                        },
                        valueRange = 0f..1f,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
    )
}

private fun getThemeColorByKey(
    theme: CustomTheme,
    key: String,
): Color {
    val longVal =
        when (key) {
            "primary" -> theme.primary
            "onPrimary" -> theme.onPrimary
            "primaryContainer" -> theme.primaryContainer
            "onPrimaryContainer" -> theme.onPrimaryContainer
            "secondary" -> theme.secondary
            "onSecondary" -> theme.onSecondary
            "secondaryContainer" -> theme.secondaryContainer
            "onSecondaryContainer" -> theme.onSecondaryContainer
            "tertiary" -> theme.tertiary
            "onTertiary" -> theme.onTertiary
            "tertiaryContainer" -> theme.tertiaryContainer
            "onTertiaryContainer" -> theme.onTertiaryContainer
            "error" -> theme.error
            "onError" -> theme.onError
            "errorContainer" -> theme.errorContainer
            "onErrorContainer" -> theme.onErrorContainer
            "background" -> theme.background
            "onBackground" -> theme.onBackground
            "surface" -> theme.surface
            "onSurface" -> theme.onSurface
            "surfaceVariant" -> theme.surfaceVariant
            "onSurfaceVariant" -> theme.onSurfaceVariant
            "outline" -> theme.outline
            "outlineVariant" -> theme.outlineVariant
            "surfaceContainer" -> theme.surfaceContainer
            else -> theme.primary
        }
    return Color(longVal.toInt())
}

@Composable
fun MaterialTheme.outlineVariant() = colorScheme.outlineVariant

@Preview(showBackground = true, name = "Custom Theme Editor")
@Composable
private fun CustomThemeScreenPreview() {
    val mockThemes =
        listOf(
            CustomTheme(
                id = 1,
                name = "Matrix",
                primary = 0xFF00FF00,
                isDark = true,
                onPrimary = 0xFF000000,
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
                surfaceContainerLowest = 0xFF00000,
            ),
            CustomTheme(
                id = 2,
                name = "Nord",
                primary = 0xFF88C0D0,
                isDark = true,
                onPrimary = 0xFF2E3440,
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
                surfaceContainerLowest = 0xFF00000,
            ),
            CustomTheme(
                id = 3,
                name = "Solarized",
                primary = 0xFFB58900,
                isDark = false,
                onPrimary = 0xFFFFFFFF,
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
                surfaceContainerLowest = 0xFF00000,
            ),
        )

    val state =
        CustomThemeUiState(
            savedThemes = mockThemes,
            selectedThemeId = 2,
            editingTheme = mockThemes[1],
        )

    val actions =
        CustomThemeActions(
            onBack = {},
            onNewTheme = {},
            onThemeSelect = {},
            onNameChange = {},
            onColorChange = { _, _ -> },
            onSave = {},
            onDelete = {},
            onToggleDarkMode = {},
        )

    MaterialTheme {
        CustomThemeScreen(
            state = state,
            actions = actions,
        )
    }
}
