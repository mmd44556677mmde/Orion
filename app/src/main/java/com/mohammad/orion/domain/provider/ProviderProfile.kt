package com.mohammad.orion.domain.provider

import java.net.URI

/** Credential material is deliberately absent; the future credential store owns it separately. */
enum class ProviderProtocol { OPENAI_COMPATIBLE, GEMINI_NATIVE, ANTHROPIC_NATIVE }
enum class ProviderRole { VOICE, BRAIN }
class ProviderProfile(
    val id: String,
    val displayName: String,
    val protocol: ProviderProtocol,
    val baseUrl: String,
    val model: String,
    val temperature: Double,
    val maxTokens: Int,
    val timeoutSeconds: Int,
    val retryCount: Int,
    val fallbackId: String? = null,
) {
    override fun toString() = "ProviderProfile([redacted])"
}

enum class ProfileProblem { ID, NAME, URL, MODEL, TEMPERATURE, MAX_TOKENS, TIMEOUT, RETRIES, FALLBACK }

/** Validates locally; never includes user-entered data in errors. */
object ProviderProfileValidator {
    fun validate(profile: ProviderProfile): Set<ProfileProblem> = buildSet {
        if (profile.id.isBlank()) add(ProfileProblem.ID)
        if (profile.displayName.isBlank()) add(ProfileProblem.NAME)
        if (!isSafeBaseUrl(profile.baseUrl)) add(ProfileProblem.URL)
        if (profile.model.isBlank()) add(ProfileProblem.MODEL)
        if (!profile.temperature.isFinite() || profile.temperature !in 0.0..2.0) add(ProfileProblem.TEMPERATURE)
        if (profile.maxTokens !in 1..1_000_000) add(ProfileProblem.MAX_TOKENS)
        if (profile.timeoutSeconds !in 1..600) add(ProfileProblem.TIMEOUT)
        if (profile.retryCount !in 0..5) add(ProfileProblem.RETRIES)
        if (profile.fallbackId != null && (profile.fallbackId.isBlank() || profile.fallbackId == profile.id)) add(ProfileProblem.FALLBACK)
    }

    private val ENCODED_CONTROL = Regex("%(?:0[0-9a-f]|1[0-9a-f]|7f)", RegexOption.IGNORE_CASE)
    private fun isSafeBaseUrl(url: String): Boolean {
        if (url.length > 2_048) return false
        return try {
            val uri = URI(url)
            uri.scheme.equals("https", ignoreCase = true) && uri.host != null &&
                uri.userInfo == null && uri.rawQuery == null && uri.rawFragment == null &&
                !ENCODED_CONTROL.containsMatchIn(uri.rawPath.orEmpty()) &&
                (uri.port == -1 || uri.port in 1..65535) && !uri.isOpaque
        } catch (_: IllegalArgumentException) { false
        } catch (_: java.net.URISyntaxException) { false }
    }
}

/** Route selection is independent for each role and refuses ambiguous IDs and fallback cycles. */
object ProviderRoutes {
    fun select(role: ProviderRole, assignments: Map<ProviderRole, String>, profiles: List<ProviderProfile>): ProviderProfile? {
        val id = assignments[role] ?: return null
        return profiles.singleOrNull { it.id == id }?.takeIf { ProviderProfileValidator.validate(it).isEmpty() }
    }

    fun fallback(primary: ProviderProfile, profiles: List<ProviderProfile>): ProviderProfile? {
        if (ProviderProfileValidator.validate(primary).isNotEmpty() || profiles.count { it.id == primary.id } != 1) return null
        val seen = mutableSetOf(primary.id)
        var current = primary
        var first: ProviderProfile? = null
        while (current.fallbackId != null) {
            val next = profiles.singleOrNull { it.id == current.fallbackId } ?: return null
            if (!seen.add(next.id) || ProviderProfileValidator.validate(next).isNotEmpty()) return null
            if (first == null) first = next
            current = next
        }
        return first
    }
}
