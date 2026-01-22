package io.github.swiftstagrime.termuxrunner

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.swiftstagrime.termuxrunner.domain.repository.UserPreferencesRepository
import io.github.swiftstagrime.termuxrunner.robots.configRobot
import io.github.swiftstagrime.termuxrunner.robots.editorRobot
import io.github.swiftstagrime.termuxrunner.robots.homeRobot
import io.github.swiftstagrime.termuxrunner.ui.MainActivity
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ScriptJourneyTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createEmptyComposeRule()

    @Inject
    lateinit var settingsRepository: UserPreferencesRepository

    @Before
    fun init() {
        hiltRule.inject()

        runBlocking {
            settingsRepository.setOnboardingCompleted(true)
        }

        ActivityScenario.launch(MainActivity::class.java)

        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            try {
                composeTestRule
                    .onNodeWithTag("fab_add_script")
                    .assertExists()
                true
            } catch (_: Throwable) {
                false
            }
        }
    }

    @Test
    fun testCreateNewScriptJourney() {
        val testScriptName = "Automation Test Script"
        val testCode = "echo 'Testing 123'"

        homeRobot(composeTestRule) {
            clickAddScript()
        }

        editorRobot(composeTestRule) {
            typeCode(testCode)
            clickSaveToConfig()
        }

        configRobot(composeTestRule) {
            enterName(testScriptName)
            clickFinalSave()
        }

        homeRobot(composeTestRule) {
            verifyScriptExists(testScriptName)
        }
    }
}