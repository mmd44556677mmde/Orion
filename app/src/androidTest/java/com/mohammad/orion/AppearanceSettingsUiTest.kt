package com.mohammad.orion

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mohammad.orion.domain.settings.AppearanceLoad
import com.mohammad.orion.domain.settings.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppearanceSettingsUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @Test fun chooseLightAndRestorePreviousTheme() {
        val repo = (rule.activity.application as OrionApplication).container.appearanceRepository
        val initial = runBlocking { (repo.state.first { it is AppearanceLoad.Ready } as AppearanceLoad.Ready).settings }
        try {
            rule.onNodeWithContentDescription(rule.activity.getString(R.string.settings)).performClick()
            rule.onNodeWithText(rule.activity.getString(R.string.appearance_title)).performScrollTo().performClick()
            rule.onNodeWithText(rule.activity.getString(R.string.appearance_light)).assertIsDisplayed().performClick()
            rule.waitUntil(5_000) { runBlocking { (repo.state.first { it is AppearanceLoad.Ready } as AppearanceLoad.Ready).settings.theme == ThemeMode.LIGHT } }
            rule.onNodeWithText(rule.activity.getString(R.string.appearance_light)).assertIsSelected()
        } finally {
            runBlocking { repo.setTheme(initial.theme); repo.setDynamicColor(initial.dynamicColor) }
        }
    }
}
