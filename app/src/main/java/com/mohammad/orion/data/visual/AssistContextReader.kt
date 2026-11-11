package com.mohammad.orion.data.visual

import android.app.assist.AssistStructure
import android.view.View
import com.mohammad.orion.domain.visual.ContextNode
import com.mohammad.orion.domain.visual.ContextStatus
import com.mohammad.orion.domain.visual.VisibleContext
import com.mohammad.orion.domain.visual.VisibleContextReader
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Reads a structure only when explicitly called by app-owned code. Never logs or uploads it. */
class AssistContextReader(
    private val reader: VisibleContextReader = VisibleContextReader(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    suspend fun read(structure: AssistStructure?): VisibleContext = withContext(dispatcher) {
        if (structure == null) return@withContext VisibleContext(ContextStatus.UNAVAILABLE, "")
        try {
            val count = structure.windowNodeCount.coerceAtMost(MAX_WINDOWS)
            val roots = (0 until count).mapNotNull { structure.getWindowNodeAt(it)?.rootViewNode?.let(::AndroidContextNode) }
            val result = reader.read(roots)
            if (structure.windowNodeCount > MAX_WINDOWS && result.status != ContextStatus.LIMIT_REACHED) {
                VisibleContext(ContextStatus.LIMIT_REACHED, result.text)
            } else result
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: RuntimeException) {
            // No exception or partial content is exposed when a structure cannot be read.
            VisibleContext(ContextStatus.UNAVAILABLE, "")
        }
    }

    private class AndroidContextNode(private val node: AssistStructure.ViewNode) : ContextNode {
        override val visible: Boolean get() = node.visibility == View.VISIBLE && node.alpha > 0f
        override val blocked: Boolean get() = node.isAssistBlocked
        override val sensitive: Boolean get() {
            val name = node.className.orEmpty().lowercase(Locale.ROOT)
            // Exclude all editable inputs, not just password variations; assist hints may be absent.
            return node.inputType != 0 || "edittext" in name || "password" in name
        }
        override val text: CharSequence? get() = node.text
        override val childCount: Int get() = node.childCount
        override fun childAt(index: Int): ContextNode = AndroidContextNode(node.getChildAt(index))
    }
    companion object { private const val MAX_WINDOWS = 16 }
}
