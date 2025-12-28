package io.github.swiftstagrime.termuxrunner.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.swiftstagrime.termuxrunner.R
import io.github.swiftstagrime.termuxrunner.domain.repository.TermuxRepository
import io.github.swiftstagrime.termuxrunner.ui.extensions.UiText
import javax.inject.Inject
/**
 * Custom exceptions mapping Termux-specific integration failures to user-friendly strings.
 */
sealed class TermuxException(val uiText: UiText) : Exception()

class TermuxNotInstalledException : TermuxException(
    UiText.StringResource(R.string.error_termux_not_installed)
)

class TermuxPermissionException : TermuxException(
    UiText.StringResource(R.string.error_termux_permission_missing)
)

class TermuxBackgroundRestrictionException : TermuxException(
    UiText.StringResource(R.string.error_background_restriction)
)

/**
 * Handles communication with the Termux app using its 'RunCommand' API.
 */
class TermuxRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : TermuxRepository {

    companion object {
        const val TERMUX_PACKAGE = "com.termux"

        // Termux RunCommand API constants
        const val ACTION_RUN_COMMAND = "com.termux.RUN_COMMAND"

        const val PERMISSION_RUN_COMMAND = "com.termux.permission.RUN_COMMAND"
        const val EXTRA_COMMAND_PATH = "com.termux.RUN_COMMAND_PATH"
        const val EXTRA_ARGUMENTS = "com.termux.RUN_COMMAND_ARGUMENTS"
        const val EXTRA_BACKGROUND = "com.termux.RUN_COMMAND_BACKGROUND"
        const val EXTRA_SESSION_ACTION = "com.termux.RUN_COMMAND_SESSION_ACTION"
    }

    override fun isTermuxInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo(TERMUX_PACKAGE, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    override fun isPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            PERMISSION_RUN_COMMAND
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("SdCardPath")
    override fun runCommand(
        command: String,
        runInBackground: Boolean,
        sessionAction: String
    ) {
        if (!isTermuxInstalled()) {
            throw TermuxNotInstalledException()
        }

        if (!isPermissionGranted()) {
            throw TermuxPermissionException()
        }

        // We route all commands through bash -c to support complex scripts/pipes
        val intent = Intent(ACTION_RUN_COMMAND).apply {
            setClassName(TERMUX_PACKAGE, "com.termux.app.RunCommandService")
            putExtra(EXTRA_COMMAND_PATH, "/data/data/com.termux/files/usr/bin/bash")
            putExtra(EXTRA_ARGUMENTS, arrayOf("-c", command))
            putExtra(EXTRA_BACKGROUND, runInBackground)
            putExtra(EXTRA_SESSION_ACTION, sessionAction)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (_: SecurityException) {
            throw TermuxPermissionException()
        } catch (e: Exception) {
            // Specifically handle Android 12+ foreground service restrictions
            if (e.message?.contains("ForegroundServiceStartNotAllowedException") == true) {
                throw TermuxBackgroundRestrictionException()
            }
            throw e
        }
    }

    override fun requestTermuxOverlay() {
        try {
            // Attempt to open the "Draw over other apps" settings for Termux
            // This is often required for Termux to start sessions from the background
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                "package:com.termux".toUri()
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                // Fallback to app details if direct overlay settings fail
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    "package:com.termux".toUri()
                )
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)

                val msg =
                    UiText.StringResource(R.string.error_find_overlay_manually).asString(context)
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            } catch (_: Exception) {
                val msg =
                    UiText.StringResource(R.string.error_open_settings_manually).asString(context)
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun isTermuxBatteryOptimized(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(TERMUX_PACKAGE)
    }
}