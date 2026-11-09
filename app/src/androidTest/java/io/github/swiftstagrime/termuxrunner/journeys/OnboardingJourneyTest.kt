package io.github.swiftstagrime.termuxrunner.journeys

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.swiftstagrime.termuxrunner.ui.MainActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class OnboardingJourneyTest : BaseJourneyTest() {
    @Test
    fun onboarding_walksEveryPageAndLandsOnHome() {
        runBlocking { settingsRepository.setOnboardingCompleted(false) }
        ActivityScenario.launch(MainActivity::class.java)

        waitUntilText("Welcome to Script Runner")

        repeat(15) {
            if (homeIsVisible()) return
            composeTestRule.onNodeWithTag("onboarding_next_button").performClick()
            composeTestRule.waitForIdle()
        }

        waitUntilHome()

        val completed = runBlocking { settingsRepository.hasCompletedOnboarding.first() }
        assertTrue("Onboarding completion should be persisted", completed)
    }

    private fun homeIsVisible(): Boolean =
        runCatching {
            composeTestRule.onNodeWithTag(TAG_HOME_FAB).assertExists()
            true
        }.getOrDefault(false)
}
