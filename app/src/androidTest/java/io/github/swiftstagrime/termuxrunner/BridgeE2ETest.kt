package io.github.swiftstagrime.termuxrunner

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.swiftstagrime.termuxrunner.domain.model.Script
import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptRepository
import io.github.swiftstagrime.termuxrunner.domain.usecase.RunScriptUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

// Regression test for issue #25: oversized scripts must reach Termux through
// the bridge file in public Downloads. Self-skips without Termux/RUN_COMMAND.
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class BridgeE2ETest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var scriptRepository: ScriptRepository

    @Inject
    lateinit var runScriptUseCase: RunScriptUseCase

    private lateinit var markerFile: File

    @Before
    fun setUp() {
        hiltRule.inject()
        val context = ApplicationProvider.getApplicationContext<Context>()
        markerFile =
            File(
                context.getExternalFilesDir(null),
                "issue25_e2e_marker.txt",
            )
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
    fun largeScript_executesThroughPublicDownloadsBridge() =
        runBlocking {
            markerFile.delete()

            val padding =
                "# pad pad pad pad pad pad pad pad pad pad pad pad pad\n".repeat(10_000)
            val code = padding + "echo BRIDGE_E2E_OK > '${markerFile.absolutePath}'\n"
            assertTrue("script must exceed the inline threshold", code.length > 300_000)

            val id =
                scriptRepository.insertScript(
                    Script(
                        name = "Issue25 E2E",
                        codePages = listOf(code),
                        runInBackground = true,
                    ),
                )
            try {
                val script = scriptRepository.getScriptById(id)!!
                runScriptUseCase(script)

                withTimeout(TERMUX_TIMEOUT_MS.milliseconds) {
                    while (!markerFile.exists()) {
                        delay(500.milliseconds)
                    }
                }
                assertEquals("BRIDGE_E2E_OK", markerFile.readText().trim())
            } finally {
                markerFile.delete()
                scriptRepository.getScriptById(id)?.let { scriptRepository.deleteScript(it) }
            }
        }

    companion object {
        const val TERMUX_TIMEOUT_MS = 60_000L
    }
}
