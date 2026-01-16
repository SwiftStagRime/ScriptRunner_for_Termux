package io.github.swiftstagrime.termuxrunner.data.repository

import android.app.PendingIntent
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
import io.github.swiftstagrime.termuxrunner.data.receiver.TermuxResultReceiver
import io.github.swiftstagrime.termuxrunner.domain.repository.TermuxRepository
import io.github.swiftstagrime.termuxrunner.ui.extensions.UiText
import javax.inject.Inject

/**
 * Custom exceptions mapping Termux-specific integration failures to user-friendly strings.
 */
sealed class TermuxException(
    val uiText: UiText,
) : Exception()

class TermuxNotInstalledException :
    TermuxException(
        UiText.StringResource(R.string.error_termux_not_installed),
    )

class TermuxPermissionException :
    TermuxException(
        UiText.StringResource(R.string.error_termux_permission_missing),
    )

class TermuxBackgroundRestrictionException :
    TermuxException(
        UiText.StringResource(R.string.error_background_restriction),
    )

/**
 * Handles communication with the Termux app using its 'RunCommand' API.
 */
class TermuxRepositoryImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
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
            const val EXTRA_PENDING_INTENT = "com.termux.RUN_COMMAND_PENDING_INTENT"
        }

        override fun isTermuxInstalled(): Boolean =
            try {
                context.packageManager.getPackageInfo(TERMUX_PACKAGE, 0)
                true
            } catch (_: PackageManager.NameNotFoundException) {
                false
            }

        override fun isPermissionGranted(): Boolean =
            ContextCompat.checkSelfPermission(
                context,
                PERMISSION_RUN_COMMAND,
            ) == PackageManager.PERMISSION_GRANTED

        override fun runCommand(
            command: String,
            runInBackground: Boolean,
            sessionAction: String,
            scriptId: Int,
            scriptName: String,
            notifyOnResult: Boolean,
            automationId: Int?,
        ) {
            if (!isTermuxInstalled()) throw TermuxNotInstalledException()
            if (!isPermissionGranted()) throw TermuxPermissionException()

            val dataDirPrefix =
                context.filesDir.absolutePath
                    .split("/" + context.packageName)
                    .first()
            val termuxBashPath = "$dataDirPrefix/$TERMUX_PACKAGE/files/usr/bin/bash"

            val intent =
                Intent(ACTION_RUN_COMMAND).apply {
                    setClassName(TERMUX_PACKAGE, "com.termux.app.RunCommandService")
                    putExtra(EXTRA_COMMAND_PATH, termuxBashPath)
                    putExtra(EXTRA_ARGUMENTS, arrayOf("-c", command))
                    putExtra(EXTRA_BACKGROUND, runInBackground)
                    putExtra(EXTRA_SESSION_ACTION, sessionAction)

                    if (notifyOnResult) {
                        val resultIntent =
                            Intent(context, TermuxResultReceiver::class.java).apply {
                                action = "io.github.swiftstagrime.SCRIPT_RESULT"
                                data = "script://result/$scriptId".toUri()

                                putExtra("script_id", scriptId)
                                putExtra("script_name", scriptName)
                                if (automationId != null) {
                                    putExtra("automation_id", automationId)
                                }
                            }

                        val flags =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                            } else {
                                PendingIntent.FLAG_UPDATE_CURRENT
                            }

                        val requestCode = automationId ?: scriptId

                        val pendingIntent =
                            PendingIntent.getBroadcast(
                                context,
                                requestCode,
                                resultIntent,
                                flags,
                            )

                        putExtra(EXTRA_PENDING_INTENT, pendingIntent)
                    }
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
                val intent =
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        "package:com.termux".toUri(),
                    )
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (_: Exception) {
                try {
                    // Fallback to app details if direct overlay settings fail
                    val intent =
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            "package:com.termux".toUri(),
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
