package io.github.swiftstagrime.termuxrunner.journeys

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class TileSettingsJourneyTest : BaseJourneyTest() {
    @Test
    fun tileSettings_bannerOpensTileConfigAndBack() {
        launchToHome()

        waitUntilText("Quick Settings Tiles")
        composeTestRule
            .onNodeWithText("Quick Settings Tiles")
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag("banner_tile_1").performClick()
        waitUntilGone(TAG_HOME_FAB)

        composeTestRule
            .onNodeWithText("Quick Settings Tiles")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Tile 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tile 5").assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("Unassigned (Tap to set)")
            .assertCountEquals(5)

        pressBack()
        waitUntilHome()
    }
}
