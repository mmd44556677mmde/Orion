package com.mohammad.orion.domain.memory

/** Categories only: neither findings nor logs retain the matched secret. */
enum class SecretKind { PRIVATE_KEY, CREDENTIAL, PAYMENT_CARD }

/**
 * Conservative pre-storage screening, not a guarantee that arbitrary text is secret-free.
 * Callers must inspect all user-controlled fields, including metadata and tags.
 */
class MemorySecretScanner {
    private val privateKey = Regex("-----BEGIN (?:[A-Z0-9 ]+ )?PRIVATE KEY-----", RegexOption.IGNORE_CASE)
    private val credential = Regex(
        "(?i)\\b(?:api[ _-]?key|password|passwd|passphrase|otp|one[ _-]?time[ _-]?(?:password|code)|access[ _-]?token|refresh[ _-]?token|auth(?:orization)?[ _-]?token|secret)\\b\\s*(?:[:=]|\\bis\\b)\\s*[^\\s,;]{3,}"
    )
    private val bearer = Regex("(?i)\\bbearer\\s+[a-z0-9._~+/-]{12,}={0,2}")
    private val cardCandidate = Regex("(?<!\\d)(?:\\d[ -]?){12,18}\\d(?!\\d)")

    fun inspect(fields: Iterable<String>): Set<SecretKind> = buildSet {
        fields.forEach { field ->
            if (privateKey.containsMatchIn(field)) add(SecretKind.PRIVATE_KEY)
            if (credential.containsMatchIn(field) || bearer.containsMatchIn(field)) add(SecretKind.CREDENTIAL)
            if (cardCandidate.findAll(field).any { luhn(it.value.filter(Char::isDigit)) }) add(SecretKind.PAYMENT_CARD)
        }
    }

    private fun luhn(number: String): Boolean {
        if (number.length !in 13..19 || number.all { it == number[0] }) return false
        var sum = 0
        number.reversed().forEachIndexed { index, char ->
            var digit = char.digitToInt()
            if (index % 2 == 1) { digit *= 2; if (digit > 9) digit -= 9 }
            sum += digit
        }
        return sum % 10 == 0
    }
}

/** Agent operations are denied unless explicitly enabled by app-owned configuration. */
data class MemoryPolicy(
    val agentReadAllowed: Boolean = false,
    val agentAddAllowed: Boolean = false,
    val agentEditAllowed: Boolean = false,
    val agentDeleteAllowed: Boolean = false,
)

enum class MemoryOperation { SEARCH, READ, ADD, UPDATE, ARCHIVE, DELETE, FORGET_TOPIC }
enum class MemorySensitivity { NORMAL, SENSITIVE }
enum class MemoryDecision { DENY, ALLOW, REQUIRE_CONFIRMATION }

/** This decision is not a confirmation mechanism; app-owned orchestration must enforce it. */
class MemoryPolicyEvaluator {
    fun evaluate(policy: MemoryPolicy, operation: MemoryOperation, sensitivity: MemorySensitivity): MemoryDecision {
        val enabled = when (operation) {
            MemoryOperation.SEARCH, MemoryOperation.READ -> policy.agentReadAllowed
            MemoryOperation.ADD -> policy.agentAddAllowed
            MemoryOperation.UPDATE, MemoryOperation.ARCHIVE -> policy.agentEditAllowed
            MemoryOperation.DELETE, MemoryOperation.FORGET_TOPIC -> policy.agentDeleteAllowed
        }
        if (!enabled) return MemoryDecision.DENY
        return when {
            sensitivity == MemorySensitivity.SENSITIVE -> MemoryDecision.REQUIRE_CONFIRMATION
            operation in setOf(MemoryOperation.UPDATE, MemoryOperation.ARCHIVE, MemoryOperation.DELETE, MemoryOperation.FORGET_TOPIC) -> MemoryDecision.REQUIRE_CONFIRMATION
            else -> MemoryDecision.ALLOW
        }
    }
}
