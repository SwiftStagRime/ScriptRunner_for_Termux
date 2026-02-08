package io.github.swiftstagrime.termuxrunner.ui.features.adbtrigger

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint
import io.github.swiftstagrime.termuxrunner.data.service.AdbScriptExecutionService

@AndroidEntryPoint
class AdbTriggerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val adbCode = intent.getStringExtra(AdbScriptExecutionService.EXTRA_ADB_CODE)

        if (!adbCode.isNullOrBlank()) {
            val serviceIntent = AdbScriptExecutionService.newIntent(this, adbCode)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        }

        finish()
    }
}
