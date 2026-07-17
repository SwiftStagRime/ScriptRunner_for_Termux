package io.github.swiftstagrime.termuxrunner.domain.util

import android.app.AppOpsManager
import android.content.Context
import android.os.Process
import android.util.Log

object MiuiUtils {
    fun hasShortcutPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        return try {
            val method =
                AppOpsManager::class.java.getMethod(
                    "checkOpNoThrow",
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                    String::class.java,
                )
            // 10017 is the MIUI internal code for "Install Shortcut"
            val result =
                method.invoke(
                    appOps,
                    10017,
                    Process.myUid(),
                    context.packageName,
                ) as Int

            result == AppOpsManager.MODE_ALLOWED
        } catch (e: NoSuchMethodException) {
            true
        } catch (e: Exception) {
            Log.d("MiuiUtils", "Unexpected error checking shortcut permission", e)
            true
        }
    }
}
