package com.mohammad.orion

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mohammad.orion.domain.settings.AssistantNameState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AssistantNameUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @Test fun nameIsSavedAndShownOnDashboard() {
        val repo = (rule.activity.application as OrionApplication).container.assistantNameRepository
        val previous = runBlocking { (repo.state.first() as AssistantNameState.Ready).name }
        try {
            rule.onNodeWithContentDescription(rule.activity.getString(R.string.settings)).performClick()
            rule.onNodeWithText(rule.activity.getString(R.string.assistant_name_title)).performScrollTo().performClick()
            rule.onNodeWithText(rule.activity.getString(R.string.assistant_name_label)).performTextClearance()
            rule.onNodeWithText(rule.activity.getString(R.string.assistant_name_label)).performTextInput("Atlas")
            rule.onNodeWithText(rule.activity.getString(R.string.assistant_name_save)).performClick()
            rule.waitUntil(5_000) { runBlocking { (repo.state.first() as? AssistantNameState.Ready)?.name == "Atlas" } }
            rule.onNodeWithContentDescription(rule.activity.getString(R.string.back)).performClick()
            rule.onNodeWithContentDescription(rule.activity.getString(R.string.back)).performClick()
            rule.onNodeWithText("Atlas").assertIsDisplayed()
        } finally {
            runBlocking { if (previous == null) repo.reset() else repo.setName(previous) }
        }
    }
}
