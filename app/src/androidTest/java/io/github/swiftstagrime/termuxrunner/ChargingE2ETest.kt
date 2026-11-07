package io.github.swiftstagrime.termuxrunner

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorkerFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.WorkManager
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.swiftstagrime.termuxrunner.data.receiver.EventReceiver
import io.github.swiftstagrime.termuxrunner.domain.model.Automation
import io.github.swiftstagrime.termuxrunner.domain.model.AutomationType
import io.github.swiftstagrime.termuxrunner.domain.model.Script
import io.github.swiftstagrime.termuxrunner.domain.repository.AutomationRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

// Regression test for issue #24: a charging event must run enabled
// CHARGING_STARTED automations through the real Termux chain. Self-skips
// without Termux/RUN_COMMAND.
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ChargingE2ETest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var scriptRepository: ScriptRepository

    @Inject
    lateinit var automationRepository: AutomationRepository

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    private lateinit var markerFile: File
    private lateinit var context: Context

    @Before
    fun setUp() {
        hiltRule.inject()
        context = ApplicationProvider.getApplicationContext()
        // The test app's WorkManager provider has no Hilt factory, so arm
        // WorkManager with one before the receiver enqueues Hilt workers.
        WorkManager.initialize(
            context,
            Configuration.Builder().setWorkerFactory(workerFactory).build(),
        )
        markerFile = File(context.getExternalFilesDir(null), "issue24_e2e_marker.txt")
        assumeTrue(
            "Termux not installed",
            runCatching {
                context.packageManager.getPackageInfo("com.termux", 0)
                true
            }.getOrDefault(false),
        )
        assumeTrue(
            "com.termux.permission.RUN_COMMAND not granted",
            ContextCompat.checkSelfPermission(
                context,
                "com.termux.permission.RUN_COMMAND",
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }

    @Test
    fun chargingEvent_runsChargingStartedAutomation() =
        runBlocking {
            markerFile.delete()

            val scriptId =
                scriptRepository.insertScript(
                    Script(
                        name = "Issue24 E2E",
                        codePages = listOf("echo CHARGING_E2E_OK > '${markerFile.absolutePath}'\n"),
                        runInBackground = true,
                    ),
                )
            automationRepository.saveAutomation(
                Automation(
                    id = 0,
                    scriptId = scriptId,
                    label = "Issue24 E2E",
                    type = AutomationType.CHARGING_STARTED,
                    scheduledTimestamp = System.currentTimeMillis(),
                    intervalMillis = 0,
                    daysOfWeek = emptyList(),
                    isEnabled = true,
                    lastRunTimestamp = null,
                    nextRunTimestamp = null,
                ),
            )

            try {
                // POWER_CONNECTED is protected from app-sent broadcasts, so
                // deliver it straight to the receiver the manifest wires it to.
                EventReceiver().onReceive(
                    context,
                    Intent(Intent.ACTION_POWER_CONNECTED).setPackage(context.packageName),
                )

                withTimeout(TERMUX_TIMEOUT_MS.milliseconds) {
                    while (!markerFile.exists()) {
                        delay(500.milliseconds)
                    }
                }
                assertEquals("CHARGING_E2E_OK", markerFile.readText().trim())

                val automation =
                    automationRepository.getAutomationsForScript(scriptId).firstOrNull()
                assertNotNull(automation?.lastRunTimestamp)
            } finally {
                markerFile.delete()
                val automations = automationRepository.getAutomationsForScript(scriptId)
                automations.forEach { automationRepository.deleteAutomation(it) }
                scriptRepository.getScriptById(scriptId)?.let { scriptRepository.deleteScript(it) }
            }
        }

    companion object {
        const val TERMUX_TIMEOUT_MS = 60_000L
    }
}
