package com.mohammad.orion.domain.security

import java.nio.ByteBuffer
import java.security.MessageDigest

fun interface MonotonicClock { fun nowMillis(): Long }

/** In-process, non-serializable capability; its identity and contents are never logged. */
class ConfirmationToken internal constructor() {
    override fun toString() = "ConfirmationToken([redacted])"
}

/**
 * Issue only after a real user confirms the exact displayed action and arguments. The executor must
 * consume the token against those same validated strings, recheck permissions, and execute precisely
 * that validated action. This gate does not itself display UI or authorize any Android operation.
 */
class ConfirmationGate(
    private val clock: MonotonicClock = MonotonicClock { System.nanoTime() / 1_000_000 },
    private val ttlMillis: Long = 60_000,
    private val capacity: Int = 64,
) {
    init { require(ttlMillis in 1..300_000 && capacity in 1..256) }
    private class Entry(val digest: ByteArray, val issued: Long)
    private val pending = mutableMapOf<ConfirmationToken, Entry>()

    @Synchronized fun issueForConfirmedAction(displayedAction: String, displayedArguments: String): ConfirmationToken? {
        require(valid(displayedAction, displayedArguments))
        val now = clock.nowMillis()
        pending.entries.removeAll { !fresh(it.value, now) }
        if (pending.size >= capacity) return null
        return ConfirmationToken().also { pending[it] = Entry(fingerprint(displayedAction, displayedArguments), now) }
    }

    /** Every consumption attempt invalidates the token, including mismatches or expiry. */
    @Synchronized fun consume(token: ConfirmationToken, action: String, arguments: String): Boolean {
        val entry = pending.remove(token) ?: return false
        if (!fresh(entry, clock.nowMillis()) || !valid(action, arguments)) return false
        return MessageDigest.isEqual(entry.digest, fingerprint(action, arguments))
    }

    @Synchronized fun revokeAll() { pending.clear() }

    private fun fresh(entry: Entry, now: Long): Boolean = try {
        val elapsed = Math.subtractExact(now, entry.issued)
        elapsed >= 0 && elapsed < ttlMillis
    } catch (_: ArithmeticException) { false }

    private fun valid(action: String, arguments: String) = action.isNotBlank() && action.length <= 1_024 && arguments.length <= 8_192
    private fun fingerprint(action: String, arguments: String): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        for (part in listOf(action, arguments)) {
            digest.update(ByteBuffer.allocate(4).putInt(part.length).array())
            // Hash UTF-16 code units exactly; malformed surrogate pairs must not collapse to '?'.
            part.forEach { char ->
                digest.update((char.code ushr 8).toByte())
                digest.update(char.code.toByte())
            }
        }
        return digest.digest()
    }
}
