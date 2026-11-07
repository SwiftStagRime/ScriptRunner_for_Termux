package io.github.swiftstagrime.termuxrunner.journeys

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.core.os.LocaleListCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.HiltAndroidRule
import io.github.swiftstagrime.termuxrunner.domain.repository.UserPreferencesRepository
import io.github.swiftstagrime.termuxrunner.robots.configRobot
import io.github.swiftstagrime.termuxrunner.robots.editorRobot
import io.github.swiftstagrime.termuxrunner.robots.homeRobot
import io.github.swiftstagrime.termuxrunner.ui.MainActivity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import javax.inject.Inject

abstract class BaseJourneyTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createEmptyComposeRule()

    @Inject
    lateinit var settingsRepository: UserPreferencesRepository

    @Before
    fun baseSetUp() {
        hiltRule.inject()
        grantRuntimePermissions()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
        }
    }

    @After
    fun baseTearDown() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
        }
    }

    protected fun launchToHome() {
        runBlocking { settingsRepository.setOnboardingCompleted(true) }
        ActivityScenario.launch(MainActivity::class.java)
        waitUntilHome()
    }

    protected fun waitUntilHome(timeoutMs: Long = DEFAULT_TIMEOUT_MS) {
        waitUntil(timeoutMs) {
            composeTestRule.onNodeWithTag(TAG_HOME_FAB).assertIsDisplayed()
        }
    }

    protected fun waitUntil(tag: String, timeoutMs: Long = DEFAULT_TIMEOUT_MS) {
        waitUntil(timeoutMs) { composeTestRule.onNodeWithTag(tag).assertExists() }
    }

    protected fun waitUntilGone(tag: String, timeoutMs: Long = DEFAULT_TIMEOUT_MS) {
        waitUntil(timeoutMs) { composeTestRule.onNodeWithTag(tag).assertDoesNotExist() }
    }

    protected fun waitUntilText(text: String, timeoutMs: Long = DEFAULT_TIMEOUT_MS) {
        waitUntil(timeoutMs) { composeTestRule.onNodeWithText(text).assertExists() }
    }

    protected fun waitUntilDisplayed(text: String, timeoutMs: Long = DEFAULT_TIMEOUT_MS) {
        waitUntil(timeoutMs) { composeTestRule.onNodeWithText(text).assertIsDisplayed() }
    }

    protected fun waitUntilTextContains(fragment: String, timeoutMs: Long = DEFAULT_TIMEOUT_MS) {
        waitUntil(timeoutMs) {
            composeTestRule
                .onNode(
                    SemanticsMatcher("text containing $fragment") { node ->
                        node.config.getOrNull(SemanticsProperties.Text)
                            .orEmpty()
                            .any { it.text.contains(fragment) }
                    },
                )
                .assertExists()
        }
    }

    protected fun clickTag(tag: String) {
        composeTestRule.onNodeWithTag(tag).performClick()
        composeTestRule.waitForIdle()
    }

    /** Off-screen items stay composed but taps at their coordinates land nowhere. */
    protected fun scrollToAndClickTag(tag: String) {
        composeTestRule.onNodeWithTag(tag).performScrollTo()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(tag).performClick()
        composeTestRule.waitForIdle()
    }

    protected fun scrollToText(text: String) {
        composeTestRule.onNodeWithText(text).performScrollTo()
        composeTestRule.waitForIdle()
    }

    protected fun clickText(text: String) {
        scrollToText(text)
        composeTestRule.onNodeWithText(text).performClick()
        composeTestRule.waitForIdle()
    }

    protected fun pressBack() {
        Espresso.pressBack()
        composeTestRule.waitForIdle()
    }

    protected fun createScript(name: String, code: String) {
        homeRobot(composeTestRule) {
            clickAddScript()
        }
        waitUntil(TAG_CODE_EDITOR)
        editorRobot(composeTestRule) {
            typeCode(code)
            clickSaveToConfig()
        }
        waitUntil(TAG_CONFIG_NAME)
        configRobot(composeTestRule) {
            enterName(name)
            clickFinalSave()
        }
        waitUntilHome()
        waitUntilText(name)
        scrollToText(name)
        composeTestRule.onNodeWithText(name).assertIsDisplayed()
    }

    private fun waitUntil(timeoutMs: Long, condition: () -> Unit) {
        try {
            composeTestRule.waitUntil(timeoutMs) { runCatching { condition() }.isSuccess }
        } catch (e: AssertionError) {
            throw AssertionError("Timed out after ${timeoutMs}ms waiting for condition", e)
        }
    }

    private fun grantRuntimePermissions() {
        val targetPackage =
            try {
                InstrumentationRegistry.getInstrumentation().targetContext.packageName
            } catch (_: Throwable) {
                return
            }
        runCatching {
            val process =
                Runtime
                    .getRuntime()
                    .exec("pm grant $targetPackage android.permission.POST_NOTIFICATIONS")
            process.waitFor()
        }
    }

    companion object {
        const val DEFAULT_TIMEOUT_MS = 20_000L

        const val TAG_HOME_FAB = "fab_add_script"
        const val TAG_CODE_EDITOR = "code_editor_input"
        const val TAG_CONFIG_NAME = "config_name_input"
        const val TAG_CONFIG_SAVE = "config_save_btn"
        const val TAG_HOME_AUTOMATION = "home_automation_btn"
        const val TAG_HOME_SETTINGS = "home_settings_btn"
    }
}
