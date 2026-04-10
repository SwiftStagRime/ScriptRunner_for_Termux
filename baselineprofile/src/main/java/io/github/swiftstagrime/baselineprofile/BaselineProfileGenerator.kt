package io.github.swiftstagrime.baselineprofile

import android.view.KeyEvent
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * This test class generates a basic startup baseline profile for the target package.
 *
 * We recommend you start with this but add important user flows to the profile to improve their performance.
 * Refer to the [baseline profile documentation](https://d.android.com/topic/performance/baselineprofiles)
 * for more information.
 *
 * You can run the generator with the "Generate Baseline Profile" run configuration in Android Studio or
 * the equivalent `generateBaselineProfile` gradle task:
 * ```
 * ./gradlew :app:generateReleaseBaselineProfile
 * ```
 * The run configuration runs the Gradle task and applies filtering to run only the generators.
 *
 * Check [documentation](https://d.android.com/topic/performance/benchmarking/macrobenchmark-instrumentation-args)
 * for more information about available instrumentation arguments.
 *
 * After you run the generator, you can verify the improvements running the [StartupBenchmarks] benchmark.
 *
 * When using this class to generate a baseline profile, only API 33+ or rooted API 28+ are supported.
 *
 * The minimum required version of androidx.benchmark to generate a baseline profile is 1.2.0.
 **/

@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() {
        val targetPackage = InstrumentationRegistry.getArguments().getString("targetAppId")
            ?: "io.github.swiftstagrime.termuxrunner"

        rule.collect(
            packageName = targetPackage,
            includeInStartupProfile = true,
            maxIterations = 4
        ) {
            pressHome()
            startActivityAndWait()

            device.waitForIdle()

            handleOnboarding(device, targetPackage)

            val scriptList = device.wait(Until.findObject(By.res(targetPackage, "script_list")), 5000)
            repeat(3) {
                device.swipe(
                    device.displayWidth / 2, (device.displayHeight * 0.7).toInt(),
                    device.displayWidth / 2, (device.displayHeight * 0.3).toInt(), 15
                )
                device.waitForIdle()
            }
            findAndClick(device, targetPackage, "fab_add_script", "Add Script")

            findAndClick(device, targetPackage, "editor_save_btn", "Save")

            val nameField = device.wait(Until.findObject(By.res(targetPackage, "config_name_input")), 5000)
                ?: device.wait(Until.findObject(By.res("config_name_input")), 2000)
                ?: device.wait(Until.findObject(By.text("Script Name")), 2000)

            if (nameField != null) {
                nameField.click()
                device.waitForIdle()
                device.pressKeyCode(KeyEvent.KEYCODE_T)
                device.pressKeyCode(KeyEvent.KEYCODE_E)
                device.pressKeyCode(KeyEvent.KEYCODE_S)
                device.pressKeyCode(KeyEvent.KEYCODE_T)
                device.waitForIdle()

                if (nameField.text.isNullOrEmpty()) {
                    device.executeShellCommand("input text Baseline_Test_Script")
                }
            } else {
                throw RuntimeException("Could not find the Script Name input field")
            }

            device.waitForIdle()
            findAndClick(device, targetPackage, "config_save_btn", "Save")

            device.waitForIdle()
        }
    }

    private fun handleOnboarding(device: UiDevice, pkg: String) {
        val nextButtonTag = "onboarding_next_button"

        for (i in 0..11) {
            val nextBtn = device.wait(Until.findObject(By.res(pkg, nextButtonTag)), 3000)
                ?: device.wait(Until.findObject(By.text("Next")), 1000)
                ?: device.wait(Until.findObject(By.text("Finish Setup")), 1000)

            if (nextBtn == null) break

            nextBtn.click()
            device.waitForIdle()

            if (nextBtn.text?.contains("Finish", ignoreCase = true) == true) {
                break
            }
        }
    }

    private fun findAndClick(device: UiDevice, pkg: String, tag: String, label: String) {
        val selector = By.res(pkg, tag)
        val obj = device.wait(Until.findObject(selector), 5000)
            ?: device.wait(Until.findObject(By.desc(label)), 2000)
            ?: device.wait(Until.findObject(By.desc(label.uppercase())), 1000)
            ?: device.wait(Until.findObject(By.text(label)), 1000)

        if (obj != null) {
            obj.click()
            device.waitForIdle()
        } else {
            device.dumpWindowHierarchy(System.out)
            throw RuntimeException("Could not find element [Tag: $tag | Label: $label]")
        }
    }
}