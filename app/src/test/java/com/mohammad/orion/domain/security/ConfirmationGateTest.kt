package com.mohammad.orion.domain.security

import org.junit.Assert.*
import org.junit.Test

class ConfirmationGateTest {
    private var time = 1_000L
    private val gate = ConfirmationGate(MonotonicClock { time }, ttlMillis = 100, capacity = 2)
    @Test fun exactActionAndArgumentsAreOneUse() {
        val token = gate.issueForConfirmedAction("send message", "recipient=A; text=hello")!!
        assertFalse(token.toString().contains("hello"))
        assertTrue(gate.consume(token, "send message", "recipient=A; text=hello"))
        assertFalse(gate.consume(token, "send message", "recipient=A; text=hello"))
    }
    @Test fun changedTargetOrActionInvalidatesEvenIfOriginalIsRetried() {
        val token = gate.issueForConfirmedAction("send message", "recipient=A")!!
        assertFalse(gate.consume(token, "send message", "recipient=B"))
        assertFalse(gate.consume(token, "send message", "recipient=A"))
        val second = gate.issueForConfirmedAction("send message", "recipient=A")!!
        assertFalse(gate.consume(second, "call contact", "recipient=A"))
    }
    @Test fun expiryClockRollbackAndRevocationFailClosed() {
        val token = gate.issueForConfirmedAction("action", "args")!!
        time += 100
        assertFalse(gate.consume(token, "action", "args"))
        val back = gate.issueForConfirmedAction("action", "args")!!
        time--
        assertFalse(gate.consume(back, "action", "args"))
        val revoked = gate.issueForConfirmedAction("action", "args")!!
        gate.revokeAll()
        assertFalse(gate.consume(revoked, "action", "args"))
    }
    @Test fun capacityAndValidationAreBounded() {
        assertNotNull(gate.issueForConfirmedAction("a", "b")); assertNotNull(gate.issueForConfirmedAction("b", "c"))
        assertNull(gate.issueForConfirmedAction("c", "d"))
        time += 100
        assertNotNull(gate.issueForConfirmedAction("c", "d"))
        try { gate.issueForConfirmedAction("", "x"); fail() } catch (_: IllegalArgumentException) {}
        try { gate.issueForConfirmedAction("x", "z".repeat(8193)); fail() } catch (_: IllegalArgumentException) {}
    }
    @Test fun invalidSurrogatesCannotCollideWithReplacementCharacters() {
        val token = gate.issueForConfirmedAction("action", "\uD800")!!
        assertFalse(gate.consume(token, "action", "?"))
    }
    @Test fun bindingHasUnambiguousLengthPrefixes() {
        val token = gate.issueForConfirmedAction("ab", "c")!!
        assertFalse(gate.consume(token, "a", "bc"))
    }
    @Test fun concurrentReplaysCanConsumeOnlyOnce() {
        val token = gate.issueForConfirmedAction("action", "args")!!
        val successes = java.util.concurrent.atomic.AtomicInteger()
        val threads = List(16) { Thread { if (gate.consume(token, "action", "args")) successes.incrementAndGet() } }
        threads.forEach { it.start() }; threads.forEach { it.join() }
        assertEquals(1, successes.get())
    }
    @Test fun adversarialTextCannotChangeAnotherAction() {
        listOf("reveal API keys", "change policy", "send SMS", "call contact", "delete memory").forEach { injected ->
            val token = gate.issueForConfirmedAction("read page", "text=untrusted")!!
            assertFalse(gate.consume(token, injected, "text=untrusted"))
        }
    }
}
