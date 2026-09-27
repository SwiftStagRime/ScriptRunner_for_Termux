package io.github.swiftstagrime.termuxrunner.domain.util

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager

/**
 * Reads the live device state that automation conditions depend on
 * (WiFi connectivity, charging state, battery level). Shared by the
 * worker (which decides whether a run may proceed) and the UI formatter
 * (which explains why a run is waiting), so both always agree.
 */
object DeviceConditions {
    fun isWifiConnected(context: Context): Boolean {
        val cm =
            context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = cm.getNetworkCapabilities(cm.activeNetwork)
        return capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    fun isCharging(context: Context): Boolean {
        val bm =
            context.applicationContext.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        if (bm.isCharging) return true

        val status = batteryState(context)?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    }

    /** Returns the battery level in percent, or -1 when it cannot be read. */
    fun getBatteryLevel(context: Context): Int {
        val bm =
            context.applicationContext.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return if (level > 0) level else batteryState(context)?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    }

    /**
     * Checks all conditions of an automation against the current device state.
     * [batteryThreshold] of 0 disables the battery level check.
     */
    fun isConditionMet(
        context: Context,
        requireWifi: Boolean,
        requireCharging: Boolean,
        batteryThreshold: Int,
    ): Boolean {
        if (requireWifi && !isWifiConnected(context)) return false
        if (requireCharging && !isCharging(context)) return false
        if (batteryThreshold > 0) {
            val level = getBatteryLevel(context)
            if (level in 0..100 && level < batteryThreshold) return false
        }
        return true
    }

    private fun batteryState(context: Context): Intent? =
        context.applicationContext.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
        )
}
