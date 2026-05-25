package com.suilearn

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SuiLearnSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun startSequentialPracticeFromHome() {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("顺序练习").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("顺序练习").performClick()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("练习 1/50").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("练习 1/50").assertIsDisplayed()
        composeRule.onNodeWithText("收藏").assertIsDisplayed()
    }
}
