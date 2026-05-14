package com.myhealth.app

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class NavigationUITest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val isTab = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)

    private fun tabNode(label: String) = composeRule.onNode(hasText(label) and isTab)

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    private fun ensureDashboard() {
        composeRule.waitForIdle()
        // Check if a Tab node with "Care" exists (dashboard is showing)
        val tabNodes = composeRule.onAllNodes(hasText("Care") and isTab).fetchSemanticsNodes()
        if (tabNodes.isNotEmpty()) return

        // We're on onboarding — swipe through HorizontalPager (6 pages)
        repeat(5) {
            composeRule.onRoot().performTouchInput { swipeLeft() }
            composeRule.waitForIdle()
            Thread.sleep(400)
        }
        // Last page — click Finish
        composeRule.onNodeWithText("Finish").performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun bottomNavShowsAllFourTabs() {
        ensureDashboard()
        tabNode("Care").assertIsDisplayed()
        tabNode("Diet").assertIsDisplayed()
        tabNode("Train").assertIsDisplayed()
        tabNode("Workout").assertIsDisplayed()
    }

    @Test
    fun careTabShowsConnectMyChart() {
        ensureDashboard()
        tabNode("Care").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Connect MyChart").assertIsDisplayed()
    }

    @Test
    fun tabNavigationCycles() {
        ensureDashboard()

        tabNode("Diet").performClick()
        composeRule.waitForIdle()

        tabNode("Train").performClick()
        composeRule.waitForIdle()

        tabNode("Workout").performClick()
        composeRule.waitForIdle()

        tabNode("Care").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Connect MyChart").assertIsDisplayed()
    }

    @Test
    fun careTabShowsCarePlanSection() {
        ensureDashboard()
        tabNode("Care").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Care plan").assertIsDisplayed()
    }
}
