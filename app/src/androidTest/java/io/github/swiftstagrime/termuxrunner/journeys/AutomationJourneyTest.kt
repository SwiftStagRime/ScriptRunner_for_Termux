package io.github.swiftstagrime.termuxrunner.journeys

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AutomationJourneyTest : BaseJourneyTest() {
    @Test
    fun automation_createdThroughPickerAndConfigDialog() {
        val scriptName = "Journey Auto ${System.currentTimeMillis()}"
        val label = "Morning Sync Journey"

        launchToHome()
        createScript(scriptName, "echo 'journey'")

        clickTag(TAG_HOME_AUTOMATION)
        waitUntilDisplayed("Automation")

        clickTag("automation_fab")
        waitUntilDisplayed("Select Script")

        clickText(scriptName)
        waitUntil("automation_label_input")

        composeTestRule
            .onNodeWithTag("automation_label_input")
            .performTextReplacement(label)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Schedule Task").performClick()

        waitUntilText(label)

        pressBack()
        waitUntilHome()
    }
}
