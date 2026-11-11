package com.mohammad.orion

import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mohammad.orion.assistant.OrionRecognitionService
import com.mohammad.orion.assistant.OrionSessionService
import com.mohammad.orion.assistant.OrionVoiceService
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AssistantIntegrationTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @Suppress("DEPRECATION")
    @Test fun servicesAreDeclaredAndProtected() {
        val context = rule.activity
        val pm = context.packageManager
        val voice = pm.getServiceInfo(ComponentName(context, OrionVoiceService::class.java), PackageManager.GET_META_DATA)
        val session = pm.getServiceInfo(ComponentName(context, OrionSessionService::class.java), 0)
        val recognition = pm.getServiceInfo(ComponentName(context, OrionRecognitionService::class.java), PackageManager.GET_META_DATA)
        assertEquals("android.permission.BIND_VOICE_INTERACTION", voice.permission)
        assertEquals("android.permission.BIND_VOICE_INTERACTION", session.permission)
        assertTrue(voice.metaData.getInt("android.voice_interaction") != 0)
        assertTrue(recognition.metaData.getInt("android.speech") != 0)
    }
    @Test fun settingsLeadsToSetupWithLimitations() {
        rule.onNodeWithContentDescription(rule.activity.getString(R.string.settings)).performClick()
        rule.onNodeWithText(rule.activity.getString(R.string.assistant_setup_title)).performClick()
        rule.onNodeWithText(rule.activity.getString(R.string.assistant_setup_limitations)).assertIsDisplayed()
        rule.onNodeWithText(rule.activity.getString(R.string.assistant_open_settings)).assertIsDisplayed()
    }
}
