package com.mohammad.orion.assistant

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.service.voice.VoiceInteractionService
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService
import android.speech.RecognitionService
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.mohammad.orion.R

/** The system invokes the corresponding session service; no background listening is started. */
class OrionVoiceService : VoiceInteractionService()

class OrionSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(args: Bundle?): VoiceInteractionSession = OrionSession(this)
}

/** Recognition is deliberately unavailable; no microphone data is acquired or transmitted. */
class OrionRecognitionService : RecognitionService() {
    override fun onStartListening(recognizerIntent: Intent, listener: Callback) {
        listener.error(SpeechRecognizer.ERROR_CLIENT)
    }
    override fun onCancel(listener: Callback) = Unit
    override fun onStopListening(listener: Callback) = Unit
}

/** A minimal, honest session UI until the chat and voice implementations are available. */
class OrionSession(context: Context) : VoiceInteractionSession(context) {
    private val sessionContext = context
    private fun dp(value: Int) = (value * sessionContext.resources.displayMetrics.density).toInt()

    override fun onCreateContentView(): View {
        window.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }
        val backdrop = FrameLayout(sessionContext).apply {
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { hide() }
        }
        val panel = LinearLayout(sessionContext).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(24), dp(24), dp(32))
            background = GradientDrawable().apply {
                setColor(Color.rgb(15, 25, 22))
                cornerRadius = dp(24).toFloat()
                setStroke(dp(1), Color.rgb(120, 246, 169))
            }
            isClickable = true
        }
        fun label(textRes: Int, size: Float, color: Int, bold: Boolean = false) = TextView(sessionContext).apply {
            setText(textRes)
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(6), 0, dp(6))
        }
        panel.addView(label(R.string.app_name, 24f, Color.rgb(120, 246, 169), true))
        panel.addView(label(R.string.assistant_overlay_state, 16f, Color.WHITE, true))
        panel.addView(label(R.string.assistant_overlay_description, 16f, Color.rgb(210, 225, 219)))
        panel.addView(label(R.string.assistant_overlay_dismiss, 16f, Color.rgb(122, 220, 229)).apply {
            minHeight = dp(48)
            gravity = Gravity.CENTER_VERTICAL
            isClickable = true
            isFocusable = true
            setOnClickListener { hide() }
        })
        val scroll = ScrollView(sessionContext).apply { addView(panel) }
        backdrop.addView(scroll, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM).apply {
            leftMargin = dp(12); rightMargin = dp(12); bottomMargin = dp(12)
        })
        return backdrop
    }
}
