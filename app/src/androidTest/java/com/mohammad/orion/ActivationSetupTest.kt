package com.mohammad.orion

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActivationSetupTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun setupIsReachableAndClearlyStatesUnavailable() {
        rule.onNodeWithContentDescription(rule.activity.getString(R.string.settings)).performClick()
        rule.onNodeWithText(rule.activity.getString(R.string.activation_title)).performScrollTo().performClick()
        rule.onNodeWithText(rule.activity.getString(R.string.activation_unavailable)).assertIsDisplayed()
        rule.onNodeWithText(rule.activity.getString(R.string.activation_model_note)).performScrollTo().assertIsDisplayed()
        rule.onNodeWithText(rule.activity.getString(R.string.activation_system_restrictions)).performScrollTo().assertIsDisplayed()
        rule.onNodeWithText(rule.activity.getString(R.string.activation_app_settings)).performScrollTo().assertIsDisplayed()
        rule.onNodeWithText(rule.activity.getString(R.string.activation_battery_settings)).performScrollTo().assertIsDisplayed()
        rule.onNodeWithContentDescription(rule.activity.getString(R.string.back)).performClick()
        rule.onNodeWithText(rule.activity.getString(R.string.settings_description)).assertIsDisplayed()
    }
}
