package io.github.swiftstagrime.termuxrunner.domain.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.core.net.toUri

/**
 * Needed for better service persistency and wakelock, otherwise script won't be restarted in sleeping state
 */
object BatteryUtils {
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pw = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pw.isIgnoringBatteryOptimizations(context.packageName)
    }

    @SuppressLint("BatteryLife")
    fun requestIgnoreBatteryOptimizations(context: Context) {
        if (isIgnoringBatteryOptimizations(context)) return

        try {
            val intent =
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = "package:${context.packageName}".toUri()
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            context.startActivity(intent)
        } catch (_: Exception) {
            val fallbackIntent =
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            context.startActivity(fallbackIntent)
        }
    }
}
