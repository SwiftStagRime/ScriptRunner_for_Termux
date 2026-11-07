package io.github.swiftstagrime.termuxrunner.journeys

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SettingsJourneyTest : BaseJourneyTest() {
    @Test
    fun settings_visitAllChildScreens() {
        launchToHome()

        clickTag(TAG_HOME_SETTINGS)
        waitUntilDisplayed("Settings")
        composeTestRule.onNodeWithText("Appearance").assertIsDisplayed()
        composeTestRule.onNodeWithText("Home Screen").assertIsDisplayed()

        composeTestRule
            .onNodeWithTag("accent_theme_row")
            .performScrollToIndex(ACCENT_ROW_CUSTOM_THEME_INDEX)
        waitUntil("settings_custom_theme_item")
        clickTag("settings_custom_theme_item")
        waitUntilDisplayed("Custom Themes")
        pressBack()
        waitUntilDisplayed("Settings")

        scrollToAndClickTag("settings_webhook_tile")
        waitUntilDisplayed("Enable webhook server")

        if (webhookSwitchIsOn()) {
            composeTestRule.onNodeWithTag("webhook_enable_switch").performClick()
            waitUntilText("Stopped")
        }
        composeTestRule.onNodeWithTag("webhook_enable_switch").performClick()
        waitUntilTextContains("Running on port")

        composeTestRule.onNodeWithTag("webhook_enable_switch").performClick()
        waitUntilText("Stopped")

        pressBack()
        waitUntilDisplayed("Settings")

        scrollToAndClickTag("settings_history_row")
        waitUntilDisplayed("Execution History")
        pressBack()
        waitUntilDisplayed("Settings")

        clickTag("settings_back_btn")
        waitUntilHome()
    }

    @After
    fun stopWebhookServer() {
        runBlocking { settingsRepository.setWebhookEnabled(false) }
    }

    private fun webhookSwitchIsOn(): Boolean =
        runCatching {
            composeTestRule.onNodeWithTag("webhook_enable_switch").assertIsOn()
            true
        }.getOrDefault(false)

    companion object {
        const val ACCENT_ROW_CUSTOM_THEME_INDEX = 7
    }
}
