package com.myhealth.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class OnboardingUITest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    private fun assumeOnOnboarding() {
        composeRule.waitForIdle()
        val onOnboarding = composeRule
            .onAllNodes(hasText("Welcome to MyHealth"))
            .fetchSemanticsNodes()
            .isNotEmpty()
        Assume.assumeTrue(
            "Skipped: onboarding already completed (run 'adb shell pm clear com.myhealth.app' to reset)",
            onOnboarding
        )
    }

    @Test
    fun welcomePageShowsTitleAndGetStarted() {
        assumeOnOnboarding()
        composeRule.onNodeWithText("Welcome to MyHealth").assertIsDisplayed()
        composeRule.onNodeWithText("Get started").assertIsDisplayed()
    }

    @Test
    fun getStartedNavigatesToLoginPage() {
        assumeOnOnboarding()
        composeRule.onNodeWithText("Get started").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Sign in or continue").assertIsDisplayed()
        composeRule.onNodeWithText("Continue as guest").assertIsDisplayed()
    }

    @Test
    fun loginPageShowsEmailAndPasswordFields() {
        assumeOnOnboarding()
        composeRule.onNodeWithText("Get started").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Email").assertIsDisplayed()
        composeRule.onNodeWithText("Password").assertIsDisplayed()
    }
}
