package com.mohammad.orion

import android.content.ComponentName
import android.content.pm.PackageManager
import android.security.NetworkSecurityPolicy
import android.view.WindowManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mohammad.orion.assistant.OrionSessionService
import com.mohammad.orion.assistant.OrionVoiceService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SecurityRegressionTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @Suppress("DEPRECATION")
    @Test fun componentsAndCleartextPolicyAreRestricted() {
        val context = rule.activity
        val pm = context.packageManager
        assertFalse(NetworkSecurityPolicy.getInstance().isCleartextTrafficPermitted)
        assertEquals("android.permission.BIND_VOICE_INTERACTION", pm.getServiceInfo(ComponentName(context, OrionVoiceService::class.java), 0).permission)
        assertEquals("android.permission.BIND_VOICE_INTERACTION", pm.getServiceInfo(ComponentName(context, OrionSessionService::class.java), 0).permission)
        assertEquals(0, context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_ALLOW_BACKUP)
    }
    @Test fun disablingScreenProtectionRequiresConfirmation() {
        val repo = (rule.activity.application as OrionApplication).container.secureScreenRepository
        runBlocking { repo.setEnabled(true) }
        try {
            rule.waitUntil(5_000) { rule.activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0 }
            rule.onNodeWithContentDescription(rule.activity.getString(R.string.settings)).performClick()
            rule.onNodeWithText(rule.activity.getString(R.string.security_title)).performScrollTo().performClick()
            rule.onNodeWithText(rule.activity.getString(R.string.secure_screen_label)).performClick()
            rule.onNodeWithText(rule.activity.getString(R.string.secure_screen_disable_title)).assertIsDisplayed()
            rule.onNodeWithText(rule.activity.getString(R.string.dismiss)).performClick()
            assertTrue(rule.activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0)
            rule.onNodeWithText(rule.activity.getString(R.string.secure_screen_label)).performClick()
            rule.onNodeWithText(rule.activity.getString(R.string.secure_screen_disable_confirm)).performClick()
            rule.waitUntil(5_000) { rule.activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE == 0 }
            rule.onNodeWithText(rule.activity.getString(R.string.secure_screen_label)).performClick()
            rule.waitUntil(5_000) { rule.activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0 }
        } finally { runBlocking { repo.setEnabled(true) } }
    }
}
