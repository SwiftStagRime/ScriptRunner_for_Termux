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
        var run = 1
        val targetPackage = InstrumentationRegistry.getArguments().getString("targetAppId")
            ?: "io.github.swiftstagrime.termuxrunner"

        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        rule.collect(packageName = targetPackage,
            includeInStartupProfile = true) {
            pressHome()
            startActivityAndWait()

            // 1. Wait for Splash/Initialization
            android.os.SystemClock.sleep(3000)


            // 2. Onboarding: Scroll and Click
            if (run == 1) {
                var maxSwipes = 5
                val onboardingTag = "onboarding_complete_button"
                while (maxSwipes > 0) {
                    if (device.hasObject(By.res(targetPackage, onboardingTag)) || device.hasObject(
                            By.res(onboardingTag)
                        )
                    ) {
                        break
                    }
                    device.swipe(
                        device.displayWidth / 2, (device.displayHeight * 0.8).toInt(),
                        device.displayWidth / 2, (device.displayHeight * 0.2).toInt(), 20
                    )
                    maxSwipes--
                }
                findAndClick(device, targetPackage, onboardingTag, "Finish Setup")
                run++
            }
            // 3. Home -> Editor (FAB)
            repeat(3) {
                device.swipe(
                    device.displayWidth / 2,
                    (device.displayHeight * 0.8).toInt(),
                    device.displayWidth / 2,
                    (device.displayHeight * 0.2).toInt(),
                    10
                )
                device.waitForIdle()
                device.swipe(
                    device.displayWidth / 2,
                    (device.displayHeight * 0.2).toInt(),
                    device.displayWidth / 2,
                    (device.displayHeight * 0.8).toInt(),
                    10
                )
                device.waitForIdle()
            }
            findAndClick(device, targetPackage, "fab_add_script", "Add Script")

            // 4. Editor -> Open Save Dialog
            findAndClick(device, targetPackage, "editor_save_btn", "Save")

            // 5. Config Dialog: Input Name
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

            // 6. Config Dialog: Final Save
            device.waitForIdle()
            findAndClick(device, targetPackage, "config_save_btn", "Save")

            device.waitForIdle()
        }
    }

    //I found out that only the text description works reliably
    private fun findAndClick(device: UiDevice, pkg: String, tag: String, label: String) {
        val obj = device.wait(Until.findObject(By.res(pkg, tag)), 3000)
            ?: device.wait(Until.findObject(By.res(tag)), 2000)
            ?: device.wait(Until.findObject(By.desc(label)), 2000)
            ?: device.wait(Until.findObject(By.text(label)), 2000)
            ?: device.wait(Until.findObject(By.text(label.uppercase())), 2000)

        if (obj != null) {
            obj.click()
            device.waitForIdle()
        } else {
            throw RuntimeException("Could not find element [$tag / $label]")
        }
    }
}