package com.mohammad.orion.domain.notifications

import com.mohammad.orion.domain.memory.MemorySecretScanner

enum class NotificationExposure { PUBLIC, PRIVATE, SECRET, UNKNOWN }
enum class NotificationSuppression { DISABLED, ACCESS_MISSING, PROFILE_LOCKED, NOT_ALLOWLISTED, SENSITIVE, TOO_LARGE, EMPTY }

/** Untrusted notification content is never included in diagnostic strings. */
class NotificationCandidate(
    val packageName: String,
    val exposure: NotificationExposure,
    val sensitive: Boolean,
    val title: String?,
    val body: String?,
) {
    override fun toString() = "NotificationCandidate([redacted])"
}
sealed interface NotificationDecision {
    data class Suppressed(val reason: NotificationSuppression) : NotificationDecision
    class Allowed(val title: String, val body: String) : NotificationDecision {
        override fun toString() = "Allowed([redacted])"
    }
}

/**
 * Conservative filtering only, not a substitute for user review or complete PII detection.
 * The allowlist and capability state must be supplied by trusted app-owned code.
 */
class NotificationPrivacyPolicy(private val scanner: MemorySecretScanner = MemorySecretScanner()) {
    private val possibleCode = Regex("(?iu)(?:otp|verification|security code|one[ -]?time|passcode|password|رمز|کد).{0,32}[0-9۰-۹٠-٩]{4,8}", RegexOption.DOT_MATCHES_ALL)

    fun evaluate(candidate: NotificationCandidate, enabled: Boolean, accessGranted: Boolean, profileUnlocked: Boolean, allowedPackages: Set<String>): NotificationDecision {
        fun suppress(reason: NotificationSuppression) = NotificationDecision.Suppressed(reason)
        if (!enabled) return suppress(NotificationSuppression.DISABLED)
        if (!accessGranted) return suppress(NotificationSuppression.ACCESS_MISSING)
        if (!profileUnlocked) return suppress(NotificationSuppression.PROFILE_LOCKED)
        if (candidate.packageName.isBlank() || candidate.packageName !in allowedPackages) return suppress(NotificationSuppression.NOT_ALLOWLISTED)
        if (candidate.sensitive || candidate.exposure != NotificationExposure.PUBLIC) return suppress(NotificationSuppression.SENSITIVE)
        val title = candidate.title.orEmpty()
        val body = candidate.body.orEmpty()
        if (title.length > MAX_TITLE || body.length > MAX_BODY) return suppress(NotificationSuppression.TOO_LARGE)
        if (title.isBlank() && body.isBlank()) return suppress(NotificationSuppression.EMPTY)
        // Sensitive labels and values may be split across title and body or line breaks.
        val combined = "$title\n$body"
        if (scanner.inspect(listOf(title, body, combined)).isNotEmpty() || possibleCode.containsMatchIn(combined)) return suppress(NotificationSuppression.SENSITIVE)
        return NotificationDecision.Allowed(title.trim(), body.trim())
    }

    companion object { private const val MAX_TITLE = 500; private const val MAX_BODY = 4_000 }
}
