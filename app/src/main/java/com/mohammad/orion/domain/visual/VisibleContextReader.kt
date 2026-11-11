package com.mohammad.orion.domain.visual

import com.mohammad.orion.domain.memory.MemorySecretScanner
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Access is app-owned; implementing this interface does not grant capture or upload permission. */
interface ContextNode {
    val visible: Boolean
    val blocked: Boolean
    val sensitive: Boolean
    val text: CharSequence?
    val childCount: Int
    fun childAt(index: Int): ContextNode
}

enum class ContextStatus { AVAILABLE, EMPTY, BLOCKED, LIMIT_REACHED, UNAVAILABLE }
class VisibleContext(val status: ContextStatus, val text: String) {
    override fun toString() = "VisibleContext(status=$status, text=[redacted])"
}

/**
 * Bounded on-device extraction. Sensitive, blocked and invisible subtrees are never read.
 * Content may still be sensitive: user review and per-request consent are required before upload.
 */
class VisibleContextReader(private val secretScanner: MemorySecretScanner = MemorySecretScanner()) {
    suspend fun read(roots: List<ContextNode>, maxNodes: Int = 1_000, maxCharacters: Int = 16_000): VisibleContext {
        require(maxNodes in 1..10_000 && maxCharacters in 1..100_000)
        val stack = ArrayDeque<ContextNode>()
        roots.take(maxNodes).asReversed().forEach(stack::addLast)
        var limited = roots.size > maxNodes
        var blocked = false
        var visited = 0
        val output = StringBuilder()
        while (stack.isNotEmpty() && visited < maxNodes) {
            currentCoroutineContext().ensureActive()
            val node = stack.removeLast()
            visited++
            if (node.blocked) { blocked = true; continue }
            if (!node.visible || node.sensitive) continue
            val raw = node.text
            if (raw != null && raw.isNotBlank()) {
                // Do not emit a partial node before inspecting all of its content.
                if (raw.length > maxCharacters) { limited = true; continue }
                val candidate = raw.toString().trim()
                if (secretScanner.inspect(listOf(candidate)).isEmpty()) {
                    val separator = if (output.isEmpty()) 0 else 1
                    val room = maxCharacters - output.length - separator
                    if (room <= 0) { limited = true; break }
                    if (separator != 0) output.append('\n')
                    var length = minOf(candidate.length, room)
                    if (length < candidate.length && length > 0 && Character.isHighSurrogate(candidate[length - 1])) length--
                    output.append(candidate, 0, length)
                    if (length < candidate.length) { limited = true; break }
                }
            }
            val count = node.childCount.coerceAtLeast(0)
            val capacity = (maxNodes - visited - stack.size).coerceAtLeast(0)
            val take = minOf(count, capacity)
            if (take < count) limited = true
            for (i in take - 1 downTo 0) stack.addLast(node.childAt(i))
        }
        if (stack.isNotEmpty()) limited = true
        val status = when {
            limited -> ContextStatus.LIMIT_REACHED
            output.isNotEmpty() -> ContextStatus.AVAILABLE
            blocked -> ContextStatus.BLOCKED
            else -> ContextStatus.EMPTY
        }
        return VisibleContext(status, output.toString())
    }
}
