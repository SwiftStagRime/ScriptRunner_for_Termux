package io.github.swiftstagrime.termuxrunner.journeys

import androidx.compose.ui.test.assertHasClickAction
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
class EditorVersionsJourneyTest : BaseJourneyTest() {
    @Test
    fun editor_savingChangedCodeCreatesVersion() {
        val scriptName = "Journey Versions ${System.currentTimeMillis()}"

        launchToHome()
        createScript(scriptName, "echo 'v1'")

        clickText(scriptName)
        waitUntil(TAG_CODE_EDITOR)

        composeTestRule
            .onNodeWithTag(TAG_CODE_EDITOR)
            .performTextReplacement("echo 'v2'")
        composeTestRule.waitForIdle()

        clickTag("editor_save_btn")
        waitUntil(TAG_CONFIG_SAVE)
        clickTag(TAG_CONFIG_SAVE)
        waitUntilHome()

        clickText(scriptName)
        waitUntil(TAG_CODE_EDITOR)

        composeTestRule
            .onNodeWithTag("editor_versions_btn")
            .assertHasClickAction()
        clickTag("editor_versions_btn")
        waitUntilDisplayed("Code Versions")

        composeTestRule.onNodeWithText("Version 1").assertExists()

        pressBack()
        waitUntil(TAG_CODE_EDITOR)
        pressBack()
        waitUntilHome()
    }
}
