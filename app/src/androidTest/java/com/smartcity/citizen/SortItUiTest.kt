package com.smartcity.citizen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smartcity.citizen.ui.screens.HomeScreen
import com.smartcity.citizen.ui.theme.SortItTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * SortIt Jetpack Compose UI Tests
 * Fulfills CSE226 Unit VI: Basic Jetpack Compose UI Testing
 */
@RunWith(AndroidJUnit4::class)
class SortItUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testHomeScreenDisplaysHeroTitleAndScanButton() {
        composeTestRule.setContent {
            SortItTheme {
                HomeScreen(
                    onNavigateToScan = {},
                    onNavigateToGuide = {},
                    onNavigateToLocator = {},
                    onNavigateToHistory = {}
                )
            }
        }

        // Verify Hero Branding & CTA
        composeTestRule.onNodeWithText("SortIt • Campus Eco Hub").assertIsDisplayed()
        composeTestRule.onNodeWithText("Identify Waste (AI Camera)").assertIsDisplayed()

        // Verify Impact Section
        composeTestRule.onNodeWithText("Your Segregation Impact").assertIsDisplayed()
        composeTestRule.onNodeWithText("Items Sorted").assertIsDisplayed()
    }

    @Test
    fun testHomeScreenDisplaysDailyEcoTip() {
        composeTestRule.setContent {
            SortItTheme {
                HomeScreen(
                    onNavigateToScan = {},
                    onNavigateToGuide = {},
                    onNavigateToLocator = {},
                    onNavigateToHistory = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Daily Eco Tip & Student Challenge").assertIsDisplayed()
    }
}
