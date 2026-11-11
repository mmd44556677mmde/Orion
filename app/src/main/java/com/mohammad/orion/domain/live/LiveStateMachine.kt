package com.mohammad.orion.domain.live

import java.util.UUID

/** Pure transition model. The caller must serialize transitions and execute effects in order. */
enum class LivePhase { IDLE, LISTENING, THINKING, SPEAKING, RECONNECTING, ERROR, ENDED }
enum class LiveError { PERMISSION, EMPTY_RESPONSE, PROVIDER, AUDIO }
data class LiveState(
    val phase: LivePhase = LivePhase.IDLE,
    val sessionId: UUID? = null,
    val turn: Long = 0,
    val muted: Boolean = false,
    val error: LiveError? = null,
)

sealed interface LiveEvent {
    /** providerReady must come from a current provider health check, not merely network presence. */
    data class Start(val microphoneGranted: Boolean, val providerReady: Boolean) : LiveEvent
    data class Mute(val sessionId: UUID, val muted: Boolean) : LiveEvent
    class Transcript(val sessionId: UUID, val turn: Long, val text: String) : LiveEvent {
        override fun toString() = "Transcript([redacted])"
    }
    class Response(val sessionId: UUID, val turn: Long, val text: String) : LiveEvent {
        override fun toString() = "Response([redacted])"
    }
    data class PlaybackDone(val sessionId: UUID, val turn: Long) : LiveEvent
    data class BargeIn(val sessionId: UUID) : LiveEvent
    data class Disconnected(val sessionId: UUID) : LiveEvent
    /** Emit only after a fresh provider health check, not merely a network callback. */
    data class Reconnected(val sessionId: UUID) : LiveEvent
    data class Failure(val sessionId: UUID, val turn: Long, val error: LiveError) : LiveEvent
    data class End(val sessionId: UUID) : LiveEvent
    data class Backgrounded(val sessionId: UUID) : LiveEvent
}

sealed interface LiveEffect {
    data class StartListening(val sessionId: UUID, val turn: Long) : LiveEffect
    data object StopListening : LiveEffect
    data object StopSpeaking : LiveEffect
    data object CancelResponse : LiveEffect
    class Submit(val sessionId: UUID, val turn: Long, val text: String) : LiveEffect {
        override fun toString() = "Submit([redacted])"
    }
    class Speak(val sessionId: UUID, val turn: Long, val text: String) : LiveEffect {
        override fun toString() = "Speak([redacted])"
    }
}

data class LiveTransition(val state: LiveState, val effects: List<LiveEffect> = emptyList())

/** No audio, network, persistence, or platform operations occur in this reducer. */
class LiveStateMachine {
    fun reduce(state: LiveState, event: LiveEvent): LiveTransition {
        if (event is LiveEvent.Start) {
            if (state.phase !in setOf(LivePhase.IDLE, LivePhase.ENDED, LivePhase.ERROR)) return LiveTransition(state)
            val sessionId = UUID.randomUUID()
            if (!event.microphoneGranted) return LiveTransition(LiveState(LivePhase.ERROR, sessionId, error = LiveError.PERMISSION))
            if (!event.providerReady) return LiveTransition(LiveState(LivePhase.RECONNECTING, sessionId))
            val next = LiveState(LivePhase.LISTENING, sessionId)
            return LiveTransition(next, listOf(LiveEffect.StartListening(sessionId, next.turn)))
        }
        val id = state.sessionId ?: return LiveTransition(state)
        if (state.phase in setOf(LivePhase.IDLE, LivePhase.ENDED)) return LiveTransition(state)
        return when (event) {
            is LiveEvent.Start -> LiveTransition(state)
            is LiveEvent.End -> if (event.sessionId == id) end(state) else LiveTransition(state)
            is LiveEvent.Backgrounded -> if (event.sessionId == id) end(state) else LiveTransition(state)
            is LiveEvent.Mute -> {
                if (event.sessionId != id || event.muted == state.muted || state.phase == LivePhase.ERROR) LiveTransition(state)
                else {
                    val next = state.copy(muted = event.muted, turn = if (state.phase == LivePhase.LISTENING && event.muted) state.turn + 1 else state.turn)
                    val effects = if (state.phase != LivePhase.LISTENING) emptyList() else if (event.muted) listOf(LiveEffect.StopListening) else listOf(LiveEffect.StartListening(id, next.turn))
                    LiveTransition(next, effects)
                }
            }
            is LiveEvent.Transcript -> {
                if (event.sessionId != id || event.turn != state.turn || state.phase != LivePhase.LISTENING || state.muted || event.text.isBlank()) LiveTransition(state)
                else {
                    val next = state.copy(phase = LivePhase.THINKING, turn = state.turn + 1)
                    LiveTransition(next, listOf(LiveEffect.StopListening, LiveEffect.Submit(id, next.turn, event.text.trim())))
                }
            }
            is LiveEvent.Response -> {
                if (event.sessionId != id || event.turn != state.turn || state.phase != LivePhase.THINKING) LiveTransition(state)
                else if (event.text.isBlank()) fail(state, LiveError.EMPTY_RESPONSE)
                else LiveTransition(state.copy(phase = LivePhase.SPEAKING), listOf(LiveEffect.Speak(id, state.turn, event.text)))
            }
            is LiveEvent.PlaybackDone -> {
                if (event.sessionId != id || event.turn != state.turn || state.phase != LivePhase.SPEAKING) LiveTransition(state)
                else listening(state)
            }
            is LiveEvent.BargeIn -> {
                if (event.sessionId != id || state.muted || state.phase !in setOf(LivePhase.THINKING, LivePhase.SPEAKING)) LiveTransition(state)
                else {
                    val next = state.copy(phase = LivePhase.LISTENING, turn = state.turn + 1)
                    LiveTransition(next, listOf(LiveEffect.CancelResponse, LiveEffect.StopSpeaking, LiveEffect.StartListening(id, next.turn)))
                }
            }
            is LiveEvent.Disconnected -> {
                if (event.sessionId != id || state.phase in setOf(LivePhase.RECONNECTING, LivePhase.ERROR)) LiveTransition(state)
                else LiveTransition(state.copy(phase = LivePhase.RECONNECTING, turn = state.turn + 1), stopEffects())
            }
            is LiveEvent.Reconnected -> {
                if (event.sessionId != id || state.phase != LivePhase.RECONNECTING) LiveTransition(state) else listening(state)
            }
            is LiveEvent.Failure -> {
                if (event.sessionId != id || event.turn != state.turn || state.phase !in setOf(LivePhase.LISTENING, LivePhase.THINKING, LivePhase.SPEAKING)) LiveTransition(state)
                else fail(state, event.error)
            }
        }
    }

    private fun listening(state: LiveState): LiveTransition {
        val next = state.copy(phase = LivePhase.LISTENING)
        return LiveTransition(next, if (next.muted) emptyList() else listOf(LiveEffect.StartListening(requireNotNull(next.sessionId), next.turn)))
    }
    private fun stopEffects() = listOf(LiveEffect.StopListening, LiveEffect.StopSpeaking, LiveEffect.CancelResponse)
    private fun end(state: LiveState) = LiveTransition(state.copy(phase = LivePhase.ENDED, sessionId = null, turn = state.turn + 1), stopEffects())
    private fun fail(state: LiveState, error: LiveError) = LiveTransition(state.copy(phase = LivePhase.ERROR, error = error, turn = state.turn + 1), stopEffects())
}
