package com.mohammad.orion.integration

import com.mohammad.orion.domain.connectivity.*
import com.mohammad.orion.domain.live.*
import com.mohammad.orion.domain.memory.*
import com.mohammad.orion.domain.provider.ProviderError
import com.mohammad.orion.domain.security.ConfirmationGate
import com.mohammad.orion.domain.security.MonotonicClock
import com.mohammad.orion.domain.tools.OfflineCommandParser
import com.mohammad.orion.domain.tools.OfflineParseResult
import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

/** Real domain components with synthetic inputs; no Android services or persistence. */
class DomainBoundaryStabilityTest {
    @Test fun parsedMemoryCandidateRemainsScopedAndDeletedCandidateIsExcluded() {
        val parsed = ExplicitMemoryCommandParser().parse("remember that I prefer green tea") as ExplicitMemoryCommand.Remember
        val now = Instant.parse("2026-01-01T00:00:00Z")
        val ranker = MemoryRetrieval()
        val active = MemoryCandidate("1", "Tea", parsed.text, "user")
        assertEquals(listOf("1"), ranker.select("green tea", listOf(active), now, MemoryDestination.LOCAL, RetrievalPrivacy()).memories.map { it.id })
        assertEquals(RetrievalStatus.BLOCKED, ranker.select("green tea", listOf(active), now, MemoryDestination.REMOTE, RetrievalPrivacy()).status)
        val deleted = MemoryCandidate("1", "Tea", parsed.text, "user", deletedAt = now)
        assertTrue(ranker.select("green tea", listOf(deleted), now, MemoryDestination.LOCAL, RetrievalPrivacy()).memories.isEmpty())
        assertEquals(MemoryDecision.DENY, MemoryPolicyEvaluator().evaluate(MemoryPolicy(), MemoryOperation.DELETE, MemorySensitivity.NORMAL))
    }
    @Test fun parsedAppLabelDoesNotAuthorizeDifferentTargetsOrReplay() {
        val parsed = OfflineCommandParser().parse("open app Maps") as OfflineParseResult.OpenApp
        var now = 1_000L
        val gate = ConfirmationGate(MonotonicClock { now }, 100)
        val token = gate.issueForConfirmedAction("open app", parsed.label)!!
        assertFalse(gate.consume(token, "open app", "Other app"))
        assertFalse(gate.consume(token, "open app", parsed.label))
        val expired = gate.issueForConfirmedAction("open app", parsed.label)!!
        now += 100
        assertFalse(gate.consume(expired, "open app", parsed.label))
    }
    @Test fun repeatedNetworkChangesNeverImplyProviderSuccessOrUnsafeRetry() {
        val retry = RetryPolicy(1_000, 8_000, 3)
        repeat(2_000) { attempt ->
            assertEquals(RemoteAvailability.OFFLINE, remoteAvailability(NetworkStatus.DISCONNECTED, ProviderHealth.CHECKING))
            assertEquals(RemoteAvailability.OFFLINE, remoteAvailability(NetworkStatus.UNVALIDATED, ProviderHealth.HEALTHY))
            assertEquals(RemoteAvailability.CONNECTING, remoteAvailability(NetworkStatus.VALIDATED, ProviderHealth.CHECKING))
            assertNull(retry.delayMillis(ProviderError.AUTHENTICATION, 0, true, false, 0.5))
            assertNull(retry.delayMillis(ProviderError.NETWORK, 0, true, true, 0.5))
            assertTrue(retry.delayMillis(ProviderError.NETWORK, attempt % 3, true, false, 0.5)!! <= 8_000)
        }
    }
    @Test fun repeatedLiveBackgroundAndRestartCannotAcceptStaleEvents() {
        val machine = LiveStateMachine()
        var state = LiveState()
        repeat(1_000) {
            val started = machine.reduce(state, LiveEvent.Start(true, true))
            val id = started.state.sessionId!!
            val turn = started.state.turn
            val thinking = machine.reduce(started.state, LiveEvent.Transcript(id, turn, "hello")).state
            val ended = machine.reduce(thinking, LiveEvent.Backgrounded(id))
            assertEquals(LivePhase.ENDED, ended.state.phase)
            assertEquals(3, ended.effects.size)
            assertEquals(ended.state, machine.reduce(ended.state, LiveEvent.Response(id, thinking.turn, "late")).state)
            state = ended.state
        }
    }
}
