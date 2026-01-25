package io.github.swiftstagrime.termuxrunner.robots

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement

class HomeRobot(
    private val composeTestRule: ComposeTestRule,
) {
    fun clickAddScript() {
        composeTestRule.onNodeWithTag("fab_add_script").performClick()
    }

    fun verifyScriptExists(name: String) {
        composeTestRule.onNodeWithText(name).assertIsDisplayed()
    }
}

class EditorRobot(
    private val composeTestRule: ComposeTestRule,
) {
    fun typeCode(code: String) {
        composeTestRule.onNodeWithTag("code_editor_input").performTextReplacement(code)
    }

    fun clickSaveToConfig() {
        composeTestRule.onNodeWithTag("editor_save_btn").performClick()
    }
}

class ConfigRobot(
    private val composeTestRule: ComposeTestRule,
) {
    fun enterName(name: String) {
        composeTestRule.onNodeWithTag("config_name_input").performTextReplacement(name)
    }

    fun clickFinalSave() {
        composeTestRule.onNodeWithTag("config_save_btn").performClick()
    }
}

fun homeRobot(
    rule: ComposeTestRule,
    block: HomeRobot.() -> Unit,
) = HomeRobot(rule).block()

fun editorRobot(
    rule: ComposeTestRule,
    block: EditorRobot.() -> Unit,
) = EditorRobot(rule).block()

fun configRobot(
    rule: ComposeTestRule,
    block: ConfigRobot.() -> Unit,
) = ConfigRobot(rule).block()
