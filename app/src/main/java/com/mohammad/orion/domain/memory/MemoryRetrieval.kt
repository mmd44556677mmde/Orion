package com.mohammad.orion.domain.memory

import java.text.Normalizer
import java.time.Instant
import java.util.Locale

/** A bounded, already-authorized repository candidate. Memory content is always untrusted data. */
class MemoryCandidate(
    val id: String,
    val title: String,
    val content: String,
    val source: String,
    val tags: List<String> = emptyList(),
    val importance: Int = 0,
    val confidence: Double = 1.0,
    val pinned: Boolean = false,
    val archived: Boolean = false,
    val deletedAt: Instant? = null,
    val expiresAt: Instant? = null,
    val reviewAt: Instant? = null,
    val sensitive: Boolean = false,
) {
    override fun toString() = "MemoryCandidate([redacted])"
}

enum class MemoryDestination { LOCAL, REMOTE }
enum class RetrievalStatus { OK, BLOCKED, INVALID_QUERY, TOO_MANY_CANDIDATES }

/** Values must come from trusted app-owned user settings, never from a model or retrieved text. */
data class RetrievalPrivacy(
    val privateMode: Boolean = true,
    val remoteSharingAllowed: Boolean = false,
    val privateRemoteException: Boolean = false,
    val sensitiveAllowed: Boolean = false,
)
class MemoryRetrievalResult(val status: RetrievalStatus, val memories: List<MemoryCandidate>) {
    override fun toString() = "MemoryRetrievalResult(status=$status, memories=[redacted])"
}

/**
 * Local ranking over a bounded repository-provided candidate page. This is not a repository,
 * tokenizer, authorization gate, or prompt builder. Its character budget is not a token budget.
 */
class MemoryRetrieval {
    fun select(
        query: String,
        candidates: List<MemoryCandidate>,
        now: Instant,
        destination: MemoryDestination,
        privacy: RetrievalPrivacy,
        maxItems: Int = 5,
        maxCharacters: Int = 4_000,
    ): MemoryRetrievalResult {
        require(maxItems in 1..20 && maxCharacters in 1..20_000)
        if (destination == MemoryDestination.REMOTE && (!privacy.remoteSharingAllowed || (privacy.privateMode && !privacy.privateRemoteException))) {
            return MemoryRetrievalResult(RetrievalStatus.BLOCKED, emptyList())
        }
        if (query.length > 512) return MemoryRetrievalResult(RetrievalStatus.INVALID_QUERY, emptyList())
        val terms = tokens(query)
        if (terms.isEmpty()) return MemoryRetrievalResult(RetrievalStatus.INVALID_QUERY, emptyList())
        if (candidates.size > MAX_CANDIDATES) return MemoryRetrievalResult(RetrievalStatus.TOO_MANY_CANDIDATES, emptyList())
        val duplicates = candidates.groupingBy { it.id }.eachCount().filterValues { it > 1 }.keys
        val matches = candidates.asSequence().filter { candidate ->
            candidate.id.isNotBlank() && candidate.id.length <= 128 && candidate.id !in duplicates &&
                candidate.title.length <= 256 && candidate.content.length <= 4_096 && candidate.source.length <= 512 &&
                candidate.tags.size <= 30 && candidate.tags.all { it.length <= 64 } &&
                candidate.importance in 0..100 && candidate.confidence.isFinite() && candidate.confidence in 0.5..1.0 &&
                !candidate.archived && candidate.deletedAt == null &&
                (candidate.expiresAt == null || candidate.expiresAt > now) &&
                (candidate.reviewAt == null || candidate.reviewAt > now) &&
                (!candidate.sensitive || privacy.sensitiveAllowed)
        }.mapNotNull { candidate ->
            val candidateTerms = tokens(candidate.title + " " + candidate.content + " " + candidate.tags.joinToString(" "))
            val overlap = terms.count { it in candidateTerms }
            if (overlap == 0) null else candidate to overlap
        }.sortedWith(compareByDescending<Pair<MemoryCandidate, Int>> { it.first.pinned }
            .thenByDescending { it.second }.thenByDescending { it.first.importance }.thenBy { it.first.id }).toList()
        val selected = mutableListOf<MemoryCandidate>()
        var remaining = maxCharacters
        for ((candidate) in matches) {
            val size = candidate.id.length + candidate.title.length + candidate.content.length + candidate.source.length + candidate.tags.sumOf { it.length } + 16
            if (size <= remaining) {
                selected += candidate
                remaining -= size
                if (selected.size == maxItems) break
            }
        }
        return MemoryRetrievalResult(RetrievalStatus.OK, selected)
    }

    private fun tokens(value: String): Set<String> {
        val normalized = Normalizer.normalize(value, Normalizer.Form.NFKC).lowercase(Locale.ROOT)
            .replace('ي', 'ی').replace('ك', 'ک').replace('\u200c', ' ')
            .replace(Regex("\\p{M}+"), "")
        return WORD.findAll(normalized).map { it.value }.filter { it.length >= 2 && it !in STOP_WORDS }.toSet()
    }
    companion object {
        private const val MAX_CANDIDATES = 200
        private val WORD = Regex("[\\p{L}\\p{N}]+")
        private val STOP_WORDS = setOf("the", "and", "for", "with", "from", "این", "برای", "است", "های")
    }
}
