package com.mohammad.orion.domain.memory

import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class MemoryRetrievalTest {
    private val retrieval = MemoryRetrieval()
    private val now = Instant.parse("2026-01-01T00:00:00Z")
    private val local = RetrievalPrivacy()
    private fun c(id: String, text: String = "Persian coffee", pinned: Boolean = false, archived: Boolean = false, deleted: Instant? = null, expires: Instant? = null, review: Instant? = null, sensitive: Boolean = false, confidence: Double = 1.0) = MemoryCandidate(id, "Coffee", text, "user", importance = 10, confidence = confidence, pinned = pinned, archived = archived, deletedAt = deleted, expiresAt = expires, reviewAt = review, sensitive = sensitive)
    private fun select(list: List<MemoryCandidate>, privacy: RetrievalPrivacy = local, destination: MemoryDestination = MemoryDestination.LOCAL, budget: Int = 4000) = retrieval.select("coffee", list, now, destination, privacy, maxCharacters = budget)
    @Test fun pinnedMustBeRelevantAndActive() {
        val candidates = listOf(c("a"), c("b", pinned = true), MemoryCandidate("irrelevant", "Tea", "water", "user", pinned = true), c("archive", pinned = true, archived = true), c("deleted", pinned = true, deleted = now), c("expired", pinned = true, expires = now), c("review", pinned = true, review = now))
        assertEquals(listOf("b", "a"), select(candidates).memories.map { it.id })
    }
    @Test fun privateModeBlocksRemoteUnlessUserSettingsExplicitlyAllowIt() {
        assertEquals(RetrievalStatus.BLOCKED, select(listOf(c("a")), destination = MemoryDestination.REMOTE).status)
        val allow = RetrievalPrivacy(privateMode = true, remoteSharingAllowed = true, privateRemoteException = true)
        assertEquals(listOf("a"), select(listOf(c("a")), allow, MemoryDestination.REMOTE).memories.map { it.id })
        assertTrue(select(listOf(c("s", sensitive = true)), allow, MemoryDestination.REMOTE).memories.isEmpty())
    }
    @Test fun budgetAndLowConfidenceExcludeCandidates() {
        assertTrue(select(listOf(c("large")), budget = 3).memories.isEmpty())
        assertTrue(select(listOf(c("low", confidence = 0.3), c("bad", confidence = Double.NaN))).memories.isEmpty())
        assertTrue(select(listOf(c("a"), c("a"))).memories.isEmpty())
    }
    @Test fun normalizationMatchesPersianAndRejectedEmptyOrBroadInput() {
        val record = MemoryCandidate("p", "کتاب", "علاقه به كتاب", "user")
        assertEquals(listOf("p"), retrieval.select("كتاب", listOf(record), now, MemoryDestination.LOCAL, local).memories.map { it.id })
        assertEquals(RetrievalStatus.INVALID_QUERY, retrieval.select("the and", listOf(record), now, MemoryDestination.LOCAL, local).status)
        assertEquals(RetrievalStatus.TOO_MANY_CANDIDATES, select((1..201).map { c(it.toString()) }).status)
    }
    @Test fun contentAndResultsDoNotExposeMemoryInToString() {
        val record = c("a", text = "coffee private secret")
        val result = select(listOf(record))
        assertFalse(record.toString().contains("private"))
        assertFalse(result.toString().contains("private"))
    }
}
