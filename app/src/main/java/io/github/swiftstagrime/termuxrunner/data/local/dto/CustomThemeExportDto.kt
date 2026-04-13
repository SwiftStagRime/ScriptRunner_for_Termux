package io.github.swiftstagrime.termuxrunner.data.local.dto

import io.github.swiftstagrime.termuxrunner.data.local.entity.CustomThemeEntity
import kotlinx.serialization.Serializable

@Serializable
data class CustomThemeExportDto(
    val name: String,
    val isDark: Boolean,
    val primary: Long,
    val onPrimary: Long,
    val primaryContainer: Long,
    val onPrimaryContainer: Long,
    val secondary: Long,
    val onSecondary: Long,
    val secondaryContainer: Long,
    val onSecondaryContainer: Long,
    val tertiary: Long,
    val onTertiary: Long,
    val tertiaryContainer: Long,
    val onTertiaryContainer: Long,
    val error: Long,
    val onError: Long,
    val errorContainer: Long,
    val onErrorContainer: Long,
    val background: Long,
    val onBackground: Long,
    val surface: Long,
    val onSurface: Long,
    val surfaceVariant: Long,
    val onSurfaceVariant: Long,
    val outline: Long,
    val outlineVariant: Long,
    val surfaceContainer: Long,
    val surfaceContainerLowest: Long,
)
fun CustomThemeEntity.toExportDto() = CustomThemeExportDto(
    name = name, isDark = isDark, primary = primary, onPrimary = onPrimary,
    primaryContainer = primaryContainer, onPrimaryContainer = onPrimaryContainer,
    secondary = secondary, onSecondary = onSecondary,
    secondaryContainer = secondaryContainer, onSecondaryContainer = onSecondaryContainer,
    tertiary = tertiary, onTertiary = onTertiary,
    tertiaryContainer = tertiaryContainer, onTertiaryContainer = onTertiaryContainer,
    error = error, onError = onError, errorContainer = errorContainer,
    onErrorContainer = onErrorContainer, background = background,
    onBackground = onBackground, surface = surface, onSurface = onSurface,
    surfaceVariant = surfaceVariant, onSurfaceVariant = onSurfaceVariant,
    outline = outline, outlineVariant = outlineVariant,
    surfaceContainer = surfaceContainer, surfaceContainerLowest = surfaceContainerLowest
)

fun CustomThemeExportDto.toEntity() = CustomThemeEntity(
    name = name, isDark = isDark, primary = primary, onPrimary = onPrimary,
    primaryContainer = primaryContainer, onPrimaryContainer = onPrimaryContainer,
    secondary = secondary, onSecondary = onSecondary,
    secondaryContainer = secondaryContainer, onSecondaryContainer = onSecondaryContainer,
    tertiary = tertiary, onTertiary = onTertiary,
    tertiaryContainer = tertiaryContainer, onTertiaryContainer = onTertiaryContainer,
    error = error, onError = onError, errorContainer = errorContainer,
    onErrorContainer = onErrorContainer, background = background,
    onBackground = onBackground, surface = surface, onSurface = onSurface,
    surfaceVariant = surfaceVariant, onSurfaceVariant = onSurfaceVariant,
    outline = outline, outlineVariant = outlineVariant,
    surfaceContainer = surfaceContainer, surfaceContainerLowest = surfaceContainerLowest
)
