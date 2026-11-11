package com.mohammad.orion

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mohammad.orion.ui.design.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FoundationUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private fun text(id: Int) = rule.activity.getString(id)

    @Test fun startupAndSettingsNavigation() {
        rule.onNodeWithText(text(R.string.home_title)).assertIsDisplayed()
        rule.onNodeWithContentDescription(text(R.string.settings)).performClick()
        rule.onNodeWithText(text(R.string.settings_description)).assertIsDisplayed()
        rule.onNodeWithContentDescription(text(R.string.back)).performClick()
        rule.onNodeWithText(text(R.string.home_title)).assertIsDisplayed()
    }

    @Test fun rtlLargePersianAndSemanticStates() {
        val longText = "این یک متن فارسی طولانی برای بررسی خوانایی و چیدمان درست در اندازه قلم بزرگ است و باید بدون قطع شدن نمایش داده شود."
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl, LocalDensity provides Density(1f, 1.5f)) {
                OrionTheme { Column { OrionEmptyState(longText, "جزئیات"); OrionStatusIndicator(OrionStatus.OFFLINE) } }
            }
        }
        rule.onNodeWithText(longText).assertExists()
        rule.onNodeWithContentDescription(rule.activity.getString(R.string.status_description, text(R.string.status_offline))).assertExists()
    }

    @Test fun ltrStatusColorsAndDescriptions() {
        val colors = mutableMapOf<OrionStatus, androidx.compose.ui.graphics.Color>()
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                OrionTheme { Column { OrionStatus.entries.forEach { status -> colors[status] = statusColor(status); OrionStatusIndicator(status) } } }
            }
        }
        rule.runOnIdle {
            assertEquals(OrionColors.Green, colors[OrionStatus.SUCCESS])
            assertEquals(OrionColors.Cyan, colors[OrionStatus.CONNECTING])
            assertNotEquals(colors[OrionStatus.ERROR], colors[OrionStatus.WARNING])
        }
        OrionStatus.entries.forEach { status ->
            rule.onNodeWithContentDescription(rule.activity.getString(R.string.status_description, text(status.label))).assertExists()
        }
    }
}
