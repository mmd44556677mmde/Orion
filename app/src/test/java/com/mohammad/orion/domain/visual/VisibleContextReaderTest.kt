package com.mohammad.orion.domain.visual

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class VisibleContextReaderTest {
    private class Node(
        override val visible: Boolean = true,
        override val blocked: Boolean = false,
        override val sensitive: Boolean = false,
        private val value: String? = null,
        private val children: List<Node> = emptyList(),
        private val denyTextAccess: Boolean = false,
    ) : ContextNode {
        override val text: CharSequence? get() { check(!denyTextAccess); return value }
        override val childCount: Int get() = children.size
        override fun childAt(index: Int): ContextNode = children[index]
    }
    private val reader = VisibleContextReader()
    @Test fun readsVisibleTextInTreeOrder() = runBlocking {
        val result = reader.read(listOf(Node(value = "Title", children = listOf(Node(value = "First"), Node(value = "Second")))))
        assertEquals(ContextStatus.AVAILABLE, result.status)
        assertEquals("Title\nFirst\nSecond", result.text)
        assertFalse(result.toString().contains("Title"))
    }
    @Test fun blockedInvisibleAndInputSubtreesAreNeverRead() = runBlocking {
        val root = Node(children = listOf(Node(blocked = true, denyTextAccess = true), Node(visible = false, denyTextAccess = true), Node(sensitive = true, denyTextAccess = true), Node(value = "safe")))
        val result = reader.read(listOf(root))
        assertEquals("safe", result.text)
        assertEquals(ContextStatus.AVAILABLE, result.status)
        assertEquals(ContextStatus.BLOCKED, reader.read(listOf(Node(blocked = true, denyTextAccess = true))).status)
        assertEquals(ContextStatus.EMPTY, reader.read(emptyList()).status)
    }
    @Test fun omitsRecognizedSecretsAndOversizedNodes() = runBlocking {
        val result = reader.read(listOf(Node(value = "Password: sample123"), Node(value = "safe")), maxCharacters = 100)
        assertEquals("safe", result.text)
        val huge = reader.read(listOf(Node(value = "prefix Password: abc123")), maxCharacters = 8)
        assertEquals(ContextStatus.LIMIT_REACHED, huge.status)
        assertEquals("", huge.text)
    }
    @Test fun enforcesNodeAndCharacterLimitsWithoutSplittingSurrogates() = runBlocking {
        val nodes = reader.read(listOf(Node(children = listOf(Node(value = "a"), Node(value = "b")))), maxNodes = 2)
        assertEquals(ContextStatus.LIMIT_REACHED, nodes.status)
        assertEquals("a", nodes.text)
        val chars = reader.read(listOf(Node(value = "abc"), Node(value = "🙂x")), maxCharacters = 5)
        assertEquals(ContextStatus.LIMIT_REACHED, chars.status)
        assertFalse(chars.text.lastOrNull()?.isHighSurrogate() == true)
    }
    @Test fun invalidLimitsFailWithoutReadingData() = runBlocking {
        try { reader.read(listOf(Node(denyTextAccess = true)), maxNodes = 0); fail() } catch (_: IllegalArgumentException) {}
    }
}
