package io.github.swiftstagrime.termuxrunner.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Green
private val DarkGreenColorScheme =
    darkColorScheme(
        primary = DarkGreenPrimary,
        onPrimary = DarkGreenOnPrimary,
        primaryContainer = DarkGreenPrimaryContainer,
        onPrimaryContainer = DarkGreenOnPrimaryContainer,
        secondary = DarkGreenSecondary,
        onSecondary = DarkGreenOnSecondary,
        secondaryContainer = DarkGreenSecondaryContainer,
        onSecondaryContainer = DarkGreenOnSecondaryContainer,
        tertiary = DarkGreenTertiary,
        onTertiary = DarkGreenOnTertiary,
        tertiaryContainer = DarkGreenTertiaryContainer,
        onTertiaryContainer = DarkGreenOnTertiaryContainer,
        error = DarkGreenError,
        errorContainer = DarkGreenErrorContainer,
        onError = DarkGreenOnError,
        onErrorContainer = DarkGreenOnErrorContainer,
        background = DarkGreenBackground,
        onBackground = DarkGreenOnBackground,
        surface = DarkGreenSurface,
        onSurface = DarkGreenOnSurface,
        surfaceVariant = DarkGreenSurfaceVariant,
        onSurfaceVariant = DarkGreenOnSurfaceVariant,
        outline = DarkGreenOutline,
        outlineVariant = DarkGreenOutlineVariant,
        surfaceContainer = DarkGreenSurfaceContainer,
    )

private val LightGreenColorScheme =
    lightColorScheme(
        primary = LightGreenPrimary,
        onPrimary = LightGreenOnPrimary,
        primaryContainer = LightGreenPrimaryContainer,
        onPrimaryContainer = LightGreenOnPrimaryContainer,
        secondary = LightGreenSecondary,
        onSecondary = LightGreenOnSecondary,
        secondaryContainer = LightGreenSecondaryContainer,
        onSecondaryContainer = LightGreenOnSecondaryContainer,
        tertiary = LightGreenTertiary,
        onTertiary = LightGreenOnTertiary,
        tertiaryContainer = LightGreenTertiaryContainer,
        onTertiaryContainer = LightGreenOnTertiaryContainer,
        error = LightGreenError,
        errorContainer = LightGreenErrorContainer,
        onError = LightGreenOnError,
        onErrorContainer = LightGreenOnErrorContainer,
        background = LightGreenBackground,
        onBackground = LightGreenOnBackground,
        surface = LightGreenSurface,
        onSurface = LightGreenOnSurface,
        surfaceVariant = LightGreenSurfaceVariant,
        onSurfaceVariant = LightGreenOnSurfaceVariant,
        outline = LightGreenOutline,
        outlineVariant = LightGreenOutlineVariant,
        surfaceContainer = LightGreenSurfaceContainer,
    )

private val DarkBlueColorScheme =
    darkColorScheme(
        primary = DarkBluePrimary,
        onPrimary = DarkBlueOnPrimary,
        primaryContainer = DarkBluePrimaryContainer,
        onPrimaryContainer = DarkBlueOnPrimaryContainer,
        secondary = DarkBlueSecondary,
        onSecondary = DarkBlueOnSecondary,
        secondaryContainer = DarkBlueSecondaryContainer,
        onSecondaryContainer = DarkBlueOnSecondaryContainer,
        tertiary = DarkBlueTertiary,
        onTertiary = DarkBlueOnTertiary,
        tertiaryContainer = DarkBlueTertiaryContainer,
        onTertiaryContainer = DarkBlueOnTertiaryContainer,
        error = DarkBlueError,
        errorContainer = DarkBlueErrorContainer,
        onError = DarkBlueOnError,
        onErrorContainer = DarkBlueOnErrorContainer,
        background = DarkBlueBackground,
        onBackground = DarkBlueOnBackground,
        surface = DarkBlueSurface,
        onSurface = DarkBlueOnSurface,
        surfaceVariant = DarkBlueSurfaceVariant,
        onSurfaceVariant = DarkBlueOnSurfaceVariant,
        outline = DarkBlueOutline,
        outlineVariant = DarkBlueOutlineVariant,
        surfaceContainer = DarkBlueSurfaceContainer,
    )

private val LightBlueColorScheme =
    lightColorScheme(
        primary = LightBluePrimary,
        onPrimary = LightBlueOnPrimary,
        primaryContainer = LightBluePrimaryContainer,
        onPrimaryContainer = LightBlueOnPrimaryContainer,
        secondary = LightBlueSecondary,
        onSecondary = LightBlueOnSecondary,
        secondaryContainer = LightBlueSecondaryContainer,
        onSecondaryContainer = LightBlueOnSecondaryContainer,
        tertiary = LightBlueTertiary,
        onTertiary = LightBlueOnTertiary,
        tertiaryContainer = LightBlueTertiaryContainer,
        onTertiaryContainer = LightBlueOnTertiaryContainer,
        error = LightBlueError,
        errorContainer = LightBlueErrorContainer,
        onError = LightBlueOnError,
        onErrorContainer = LightBlueOnErrorContainer,
        background = LightBlueBackground,
        onBackground = LightBlueOnBackground,
        surface = LightBlueSurface,
        onSurface = LightBlueOnSurface,
        surfaceVariant = LightBlueSurfaceVariant,
        onSurfaceVariant = LightBlueOnSurfaceVariant,
        outline = LightBlueOutline,
        outlineVariant = LightBlueOutlineVariant,
        surfaceContainer = LightBlueSurfaceContainer,
    )

private val DarkRedColorScheme =
    darkColorScheme(
        primary = DarkRedPrimary,
        onPrimary = DarkRedOnPrimary,
        primaryContainer = DarkRedPrimaryContainer,
        onPrimaryContainer = DarkRedOnPrimaryContainer,
        secondary = DarkRedSecondary,
        onSecondary = DarkRedOnSecondary,
        secondaryContainer = DarkRedSecondaryContainer,
        onSecondaryContainer = DarkRedOnSecondaryContainer,
        tertiary = DarkRedTertiary,
        onTertiary = DarkRedOnTertiary,
        tertiaryContainer = DarkRedTertiaryContainer,
        onTertiaryContainer = DarkRedOnTertiaryContainer,
        error = DarkRedError,
        errorContainer = DarkRedErrorContainer,
        onError = DarkRedOnError,
        onErrorContainer = DarkRedOnErrorContainer,
        background = DarkRedBackground,
        onBackground = DarkRedOnBackground,
        surface = DarkRedSurface,
        onSurface = DarkRedOnSurface,
        surfaceVariant = DarkRedSurfaceVariant,
        onSurfaceVariant = DarkRedOnSurfaceVariant,
        outline = DarkRedOutline,
        outlineVariant = DarkRedOutlineVariant,
        surfaceContainer = DarkRedSurfaceContainer,
    )

private val LightRedColorScheme =
    lightColorScheme(
        primary = LightRedPrimary,
        onPrimary = LightRedOnPrimary,
        primaryContainer = LightRedPrimaryContainer,
        onPrimaryContainer = LightRedOnPrimaryContainer,
        secondary = LightRedSecondary,
        onSecondary = LightRedOnSecondary,
        secondaryContainer = LightRedSecondaryContainer,
        onSecondaryContainer = LightRedOnSecondaryContainer,
        tertiary = LightRedTertiary,
        onTertiary = LightRedOnTertiary,
        tertiaryContainer = LightRedTertiaryContainer,
        onTertiaryContainer = LightRedOnTertiaryContainer,
        error = LightRedError,
        errorContainer = LightRedErrorContainer,
        onError = LightRedOnError,
        onErrorContainer = LightRedOnErrorContainer,
        background = LightRedBackground,
        onBackground = LightRedOnBackground,
        surface = LightRedSurface,
        onSurface = LightRedOnSurface,
        surfaceVariant = LightRedSurfaceVariant,
        onSurfaceVariant = LightRedOnSurfaceVariant,
        outline = LightRedOutline,
        outlineVariant = LightRedOutlineVariant,
        surfaceContainer = LightRedSurfaceContainer,
    )

private val DarkAmoledColorScheme =
    darkColorScheme(
        primary = DarkAmoledPrimary,
        onPrimary = DarkAmoledOnPrimary,
        primaryContainer = DarkAmoledPrimaryContainer,
        onPrimaryContainer = DarkAmoledOnPrimaryContainer,
        secondary = DarkAmoledSecondary,
        onSecondary = DarkAmoledOnSecondary,
        secondaryContainer = DarkAmoledSecondaryContainer,
        onSecondaryContainer = DarkAmoledOnSecondaryContainer,
        tertiary = DarkAmoledTertiary,
        onTertiary = DarkAmoledOnTertiary,
        tertiaryContainer = DarkAmoledTertiaryContainer,
        onTertiaryContainer = DarkAmoledOnTertiaryContainer,
        error = DarkAmoledError,
        onError = DarkAmoledOnError,
        errorContainer = DarkAmoledErrorContainer,
        onErrorContainer = DarkAmoledOnErrorContainer,
        background = DarkAmoledBackground,
        onBackground = DarkAmoledOnBackground,
        surface = DarkAmoledSurface,
        onSurface = DarkAmoledOnSurface,
        surfaceVariant = DarkAmoledSurfaceVariant,
        onSurfaceVariant = DarkAmoledOnSurfaceVariant,
        outline = DarkAmoledOutline,
        outlineVariant = DarkAmoledOutlineVariant,
        surfaceContainer = DarkAmoledSurfaceContainer,
    )

private val LightAmoledColorScheme =
    lightColorScheme(
        primary = LightAmoledPrimary,
        onPrimary = LightAmoledOnPrimary,
        primaryContainer = LightAmoledPrimaryContainer,
        onPrimaryContainer = LightAmoledOnPrimaryContainer,
        secondary = LightAmoledSecondary,
        onSecondary = LightAmoledOnSecondary,
        secondaryContainer = LightAmoledSecondaryContainer,
        onSecondaryContainer = LightAmoledOnSecondaryContainer,
        tertiary = LightAmoledTertiary,
        onTertiary = LightAmoledOnTertiary,
        tertiaryContainer = LightAmoledTertiaryContainer,
        onTertiaryContainer = LightAmoledOnTertiaryContainer,
        error = LightAmoledError,
        onError = LightAmoledOnError,
        errorContainer = LightAmoledErrorContainer,
        onErrorContainer = LightAmoledOnErrorContainer,
        background = LightAmoledBackground,
        onBackground = LightAmoledOnBackground,
        surface = LightAmoledSurface,
        onSurface = LightAmoledOnSurface,
        surfaceVariant = LightAmoledSurfaceVariant,
        onSurfaceVariant = LightAmoledOnSurfaceVariant,
        outline = LightAmoledOutline,
        outlineVariant = LightAmoledOutlineVariant,
        surfaceContainer = LightAmoledSurfaceContainer,
    )

private val DarkColorfulAmoledColorScheme =
    darkColorScheme(
        primary = DarkColorfulAmoledPrimary,
        onPrimary = DarkColorfulAmoledOnPrimary,
        primaryContainer = DarkColorfulAmoledPrimaryContainer,
        onPrimaryContainer = DarkColorfulAmoledOnPrimaryContainer,
        secondary = DarkColorfulAmoledSecondary,
        onSecondary = DarkColorfulAmoledOnSecondary,
        secondaryContainer = DarkColorfulAmoledSecondaryContainer,
        onSecondaryContainer = DarkColorfulAmoledOnSecondaryContainer,
        tertiary = DarkColorfulAmoledTertiary,
        onTertiary = DarkColorfulAmoledOnTertiary,
        tertiaryContainer = DarkColorfulAmoledTertiaryContainer,
        onTertiaryContainer = DarkColorfulAmoledOnTertiaryContainer,
        error = DarkColorfulAmoledError,
        onError = DarkColorfulAmoledOnError,
        errorContainer = DarkColorfulAmoledErrorContainer,
        onErrorContainer = DarkColorfulAmoledOnErrorContainer,
        background = DarkColorfulAmoledBackground,
        onBackground = DarkColorfulAmoledOnBackground,
        surface = DarkColorfulAmoledSurface,
        onSurface = DarkColorfulAmoledOnSurface,
        surfaceVariant = DarkColorfulAmoledSurfaceVariant,
        onSurfaceVariant = DarkColorfulAmoledOnSurfaceVariant,
        outline = DarkColorfulAmoledOutline,
        outlineVariant = DarkColorfulAmoledOutlineVariant,
        surfaceContainer = DarkColorfulAmoledSurfaceContainer,
    )

private val LightColorfulAmoledColorScheme =
    lightColorScheme(
        primary = LightColorfulAmoledPrimary,
        onPrimary = LightColorfulAmoledOnPrimary,
        primaryContainer = LightColorfulAmoledPrimaryContainer,
        onPrimaryContainer = LightColorfulAmoledOnPrimaryContainer,
        secondary = LightColorfulAmoledSecondary,
        onSecondary = LightColorfulAmoledOnSecondary,
        secondaryContainer = LightColorfulAmoledSecondaryContainer,
        onSecondaryContainer = LightColorfulAmoledOnSecondaryContainer,
        tertiary = LightColorfulAmoledTertiary,
        onTertiary = LightColorfulAmoledOnTertiary,
        tertiaryContainer = LightColorfulAmoledTertiaryContainer,
        onTertiaryContainer = LightColorfulAmoledOnTertiaryContainer,
        error = LightColorfulAmoledError,
        onError = LightColorfulAmoledOnError,
        errorContainer = LightColorfulAmoledErrorContainer,
        onErrorContainer = LightColorfulAmoledOnErrorContainer,
        background = LightColorfulAmoledBackground,
        onBackground = LightColorfulAmoledOnBackground,
        surface = LightColorfulAmoledSurface,
        onSurface = LightColorfulAmoledOnSurface,
        surfaceVariant = LightColorfulAmoledSurfaceVariant,
        onSurfaceVariant = LightColorfulAmoledOnSurfaceVariant,
        outline = LightColorfulAmoledOutline,
        outlineVariant = LightColorfulAmoledOutlineVariant,
        surfaceContainer = LightColorfulAmoledSurfaceContainer,
    )

@Composable
fun ScriptRunnerForTermuxTheme(
    accent: AppTheme = AppTheme.GREEN,
    mode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val isDark =
        when (mode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
        }
    val colorScheme =
        when (accent) {
            AppTheme.DYNAMIC -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                } else {
                    if (isDark) DarkGreenColorScheme else LightGreenColorScheme
                }
            }

            AppTheme.GREEN -> if (isDark) DarkGreenColorScheme else LightGreenColorScheme
            AppTheme.BLUE -> if (isDark) DarkBlueColorScheme else LightBlueColorScheme
            AppTheme.RED -> if (isDark) DarkRedColorScheme else LightRedColorScheme
            AppTheme.AMOLED -> if (isDark) DarkAmoledColorScheme else LightAmoledColorScheme
            AppTheme.CYBER -> if (isDark) DarkColorfulAmoledColorScheme else LightColorfulAmoledColorScheme
        }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context.findActivity()
            if (activity != null) {
                val window = activity.window
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !isDark
                insetsController.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}

tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
