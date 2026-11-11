package com.mohammad.orion.domain.live

import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class LiveStateMachineTest {
    private val machine = LiveStateMachine()
    private fun start() = machine.reduce(LiveState(), LiveEvent.Start(true, true)).state
    private fun submit(s: LiveState) = machine.reduce(s, LiveEvent.Transcript(s.sessionId!!, s.turn, "hello"))
    @Test fun normalTurnSerializesStopSubmitSpeakAndResume() {
        val started = machine.reduce(LiveState(), LiveEvent.Start(true, true))
        assertEquals(LivePhase.LISTENING, started.state.phase)
        assertTrue(started.effects.single() is LiveEffect.StartListening)
        val sent = submit(started.state)
        assertEquals(LivePhase.THINKING, sent.state.phase)
        assertTrue(sent.effects[0] is LiveEffect.StopListening)
        assertTrue(sent.effects[1] is LiveEffect.Submit)
        val speaking = machine.reduce(sent.state, LiveEvent.Response(sent.state.sessionId!!, sent.state.turn, "answer"))
        assertEquals(LivePhase.SPEAKING, speaking.state.phase)
        assertTrue(speaking.effects.single() is LiveEffect.Speak)
        val done = machine.reduce(speaking.state, LiveEvent.PlaybackDone(speaking.state.sessionId!!, speaking.state.turn))
        assertEquals(LivePhase.LISTENING, done.state.phase)
        assertTrue(done.effects.single() is LiveEffect.StartListening)
    }
    @Test fun staleEventsAndInvalidTransitionsAreIgnored() {
        val state = start(); val id = state.sessionId!!
        assertEquals(state, machine.reduce(state, LiveEvent.Transcript(UUID.randomUUID(), state.turn, "other")).state)
        assertEquals(state, machine.reduce(state, LiveEvent.Response(id, state.turn, "late")).state)
        assertEquals(state, machine.reduce(state, LiveEvent.Transcript(id, state.turn, " ")).state)
        assertEquals(state, machine.reduce(state, LiveEvent.Start(true, true)).state)
        assertTrue(machine.reduce(state, LiveEvent.Start(true, true)).effects.isEmpty())
    }
    @Test fun muteUnmuteRejectsOldRecognitionCallbacks() {
        val state = start(); val id = state.sessionId!!
        val muted = machine.reduce(state, LiveEvent.Mute(id, true))
        assertEquals(listOf(LiveEffect.StopListening), muted.effects)
        val unmuted = machine.reduce(muted.state, LiveEvent.Mute(id, false))
        assertTrue(unmuted.effects.single() is LiveEffect.StartListening)
        assertEquals(unmuted.state, machine.reduce(unmuted.state, LiveEvent.Transcript(id, state.turn, "stale")).state)
        assertEquals(LivePhase.THINKING, machine.reduce(unmuted.state, LiveEvent.Transcript(id, unmuted.state.turn, "fresh")).state.phase)
    }
    @Test fun bargeInInvalidatesOldResponseAndPlayback() {
        val thinking = submit(start()).state; val id = thinking.sessionId!!
        val speaking = machine.reduce(thinking, LiveEvent.Response(id, thinking.turn, "answer")).state
        val barge = machine.reduce(speaking, LiveEvent.BargeIn(id))
        assertEquals(LivePhase.LISTENING, barge.state.phase)
        assertEquals(listOf(LiveEffect.CancelResponse, LiveEffect.StopSpeaking), barge.effects.take(2))
        assertEquals(barge.state, machine.reduce(barge.state, LiveEvent.PlaybackDone(id, speaking.turn)).state)
        assertEquals(barge.state, machine.reduce(barge.state, LiveEvent.Response(id, thinking.turn, "late")).state)
    }
    @Test fun reconnectCancelsStaleTurnAndRespectsMute() {
        val thinking = submit(start()).state; val id = thinking.sessionId!!
        val lost = machine.reduce(thinking, LiveEvent.Disconnected(id))
        assertEquals(LivePhase.RECONNECTING, lost.state.phase)
        assertEquals(3, lost.effects.size)
        val muted = machine.reduce(lost.state, LiveEvent.Mute(id, true)).state
        val connected = machine.reduce(muted, LiveEvent.Reconnected(id))
        assertEquals(LivePhase.LISTENING, connected.state.phase)
        assertTrue(connected.effects.isEmpty())
        assertEquals(connected.state, machine.reduce(connected.state, LiveEvent.Response(id, thinking.turn, "old")).state)
    }
    @Test fun endBackgroundPermissionAndEmptyResponseFailClosed() {
        assertEquals(LiveError.PERMISSION, machine.reduce(LiveState(), LiveEvent.Start(false, true)).state.error)
        assertEquals(LivePhase.RECONNECTING, machine.reduce(LiveState(), LiveEvent.Start(true, false)).state.phase)
        val thinking = submit(start()).state; val id = thinking.sessionId!!
        assertEquals(LiveError.EMPTY_RESPONSE, machine.reduce(thinking, LiveEvent.Response(id, thinking.turn, " ")).state.error)
        val ended = machine.reduce(thinking, LiveEvent.Backgrounded(id))
        assertEquals(LivePhase.ENDED, ended.state.phase)
        assertEquals(3, ended.effects.size)
        assertEquals(ended.state, machine.reduce(ended.state, LiveEvent.Response(id, thinking.turn, "old")).state)
        val restarted = machine.reduce(ended.state, LiveEvent.Start(true, true)).state
        assertNotEquals(id, restarted.sessionId)
    }
    @Test fun privateTextIsRedactedFromEventsAndEffects() {
        val id = UUID.randomUUID(); val secret = "private transcript"
        assertFalse(LiveEvent.Transcript(id, 0, secret).toString().contains(secret))
        assertFalse(LiveEvent.Response(id, 0, secret).toString().contains(secret))
        assertFalse(LiveEffect.Submit(id, 0, secret).toString().contains(secret))
        assertFalse(LiveEffect.Speak(id, 0, secret).toString().contains(secret))
    }
}
