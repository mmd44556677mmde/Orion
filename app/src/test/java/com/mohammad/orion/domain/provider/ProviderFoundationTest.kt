package com.mohammad.orion.domain.provider

import org.junit.Assert.*
import org.junit.Test

class ProviderFoundationTest {
    private fun profile(id: String, fallback: String? = null, url: String = "https://example.org/api") =
        ProviderProfile(id, id, ProviderProtocol.OPENAI_COMPATIBLE, url, "chosen-model", 0.5, 500, 30, 0, fallback)

    @Test fun httpsUrlValidationRejectsEmbeddedCredentialsAndQuery() {
        listOf("http://example.org", "https://user:pass@example.org", "https://example.org/?key=secret", "https://example.org/#fragment", "https://example.org/%0d%0aHost:evil", "https://example.org/%00", "https://example.org/" + "x".repeat(2049), "not a url", "https://").forEach {
            assertTrue(it, ProfileProblem.URL in ProviderProfileValidator.validate(profile("id", url = it)))
        }
        assertTrue(ProviderProfileValidator.validate(profile("id", url = "https://example.org:8443/custom/v1")).isEmpty())
    }
    @Test fun invalidNumericsAndFallbackAreReportedWithoutSensitiveValues() {
        val bad = ProviderProfile("id", "name", ProviderProtocol.GEMINI_NATIVE, "https://example.org", "model", Double.NaN, 0, 0, -1, "id")
        assertEquals(setOf(ProfileProblem.TEMPERATURE, ProfileProblem.MAX_TOKENS, ProfileProblem.TIMEOUT, ProfileProblem.RETRIES, ProfileProblem.FALLBACK), ProviderProfileValidator.validate(bad))
    }
    @Test fun roleSelectionIsIndependentAndAmbiguousIdsFailClosed() {
        val voice = profile("voice"); val brain = profile("brain")
        val roles = mapOf(ProviderRole.VOICE to voice.id, ProviderRole.BRAIN to brain.id)
        assertSame(voice, ProviderRoutes.select(ProviderRole.VOICE, roles, listOf(voice, brain)))
        assertSame(brain, ProviderRoutes.select(ProviderRole.BRAIN, roles, listOf(voice, brain)))
        assertNull(ProviderRoutes.select(ProviderRole.VOICE, roles, listOf(voice, voice)))
        assertNull(ProviderRoutes.select(ProviderRole.BRAIN, emptyMap(), listOf(brain)))
    }
    @Test fun fallbackRejectsCyclesMissingAndDuplicateIds() {
        val primary = profile("a", "b"); val b = profile("b")
        assertSame(b, ProviderRoutes.fallback(primary, listOf(primary, b)))
        assertNull(ProviderRoutes.fallback(primary, listOf(primary)))
        assertNull(ProviderRoutes.fallback(primary, listOf(primary, b, b)))
        assertNull(ProviderRoutes.fallback(primary, listOf(primary, primary, b)))
        assertNull(ProviderRoutes.fallback(primary, listOf(primary, profile("b", "a"))))
    }
    @Test fun messageAndProfileDebugStringsNeverIncludePrivateData() {
        val privateText = "private-input-and-api-key"
        assertFalse(LlmMessage(MessageRole.USER, privateText).toString().contains(privateText))
        assertFalse(GenerationResult(privateText, emptyList(), null).toString().contains(privateText))
        assertFalse(profile(privateText, url = "https://example.org/$privateText").toString().contains(privateText))
        assertFalse(ToolCall(privateText, privateText, privateText).toString().contains(privateText))
    }
}
