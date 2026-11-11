package com.mohammad.orion.domain.notifications

import org.junit.Assert.*
import org.junit.Test

class NotificationPrivacyPolicyTest {
    private val policy = NotificationPrivacyPolicy()
    private fun evaluate(candidate: NotificationCandidate, enabled: Boolean = true, access: Boolean = true, unlocked: Boolean = true, allowed: Set<String> = setOf("example.app")) = policy.evaluate(candidate, enabled, access, unlocked, allowed)
    private fun candidate(exposure: NotificationExposure = NotificationExposure.PUBLIC, text: String = "Meeting at noon", sensitive: Boolean = false, app: String = "example.app") = NotificationCandidate(app, exposure, sensitive, "Title", text)
    @Test fun defaultGatesFailClosed() {
        assertEquals(NotificationDecision.Suppressed(NotificationSuppression.DISABLED), evaluate(candidate(), enabled = false))
        assertEquals(NotificationDecision.Suppressed(NotificationSuppression.ACCESS_MISSING), evaluate(candidate(), access = false))
        assertEquals(NotificationDecision.Suppressed(NotificationSuppression.PROFILE_LOCKED), evaluate(candidate(), unlocked = false))
        assertEquals(NotificationDecision.Suppressed(NotificationSuppression.NOT_ALLOWLISTED), evaluate(candidate(app = "other.app")))
        assertEquals(NotificationDecision.Suppressed(NotificationSuppression.NOT_ALLOWLISTED), evaluate(candidate(), allowed = emptySet()))
    }
    @Test fun rejectsPrivateUnknownAndSecretContent() {
        listOf(NotificationExposure.PRIVATE, NotificationExposure.SECRET, NotificationExposure.UNKNOWN).forEach {
            assertEquals(NotificationDecision.Suppressed(NotificationSuppression.SENSITIVE), evaluate(candidate(exposure = it)))
        }
        assertEquals(NotificationDecision.Suppressed(NotificationSuppression.SENSITIVE), evaluate(candidate(sensitive = true)))
        listOf("Password: abc123", "Verification code: 123456", "کد تایید: ۱۲۳۴۵۶", "4111 1111 1111 1111").forEach {
            assertEquals(NotificationDecision.Suppressed(NotificationSuppression.SENSITIVE), evaluate(candidate(text = it)))
        }
    }
    @Test fun secretsSplitAcrossNotificationFieldsOrLinesAreSuppressed() {
        val cases = listOf(
            NotificationCandidate("example.app", NotificationExposure.PUBLIC, false, "Verification code", "123456"),
            NotificationCandidate("example.app", NotificationExposure.PUBLIC, false, "رمز تایید", "۱۲۳۴۵۶"),
            NotificationCandidate("example.app", NotificationExposure.PUBLIC, false, "Password", "= hunter22"),
            candidate(text = "Verification\ncode: 123456"),
        )
        cases.forEach { assertEquals(NotificationDecision.Suppressed(NotificationSuppression.SENSITIVE), evaluate(it)) }
    }
    @Test fun allowsOnlyApprovedNonemptyBoundedPublicTextAndDoesNotLogIt() {
        val candidate = candidate(); val decision = evaluate(candidate) as NotificationDecision.Allowed
        assertEquals("Title", decision.title); assertEquals("Meeting at noon", decision.body)
        assertFalse(candidate.toString().contains("Meeting")); assertFalse(decision.toString().contains("Meeting"))
        assertEquals(NotificationDecision.Suppressed(NotificationSuppression.TOO_LARGE), evaluate(candidate(text = "x".repeat(4001))))
        assertEquals(NotificationDecision.Suppressed(NotificationSuppression.EMPTY), evaluate(NotificationCandidate("example.app", NotificationExposure.PUBLIC, false, null, "  ")))
    }
}
