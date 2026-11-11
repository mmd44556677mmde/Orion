package com.mohammad.orion.domain.voice

/**
 * Converts a completed Markdown-like response into speech-safe sentences. Code and URL targets
 * are omitted; link labels remain. This batch utility does not process partial stream chunks.
 * It holds no state and does not retain the supplied text.
 */
class SpokenTextSegmenter {
    fun segment(text: String): List<String> {
        val spoken = readableText(text, 0).replace(Regex("\\s+"), " ").trim()
        if (spoken.isEmpty()) return emptyList()
        val result = mutableListOf<String>()
        var start = 0
        var i = 0
        while (i < spoken.length) {
            val ch = spoken[i]
            val decimal = ch == '.' && i > 0 && i + 1 < spoken.length && spoken[i - 1].isDigit() && spoken[i + 1].isDigit()
            if (ch in TERMINATORS && !decimal) {
                var end = i + 1
                while (end < spoken.length && spoken[end] in TERMINATORS) end++
                while (end < spoken.length && spoken[end] in CLOSING_PUNCTUATION) end++
                if (end == spoken.length || spoken[end].isWhitespace()) {
                    spoken.substring(start, end).trim().takeIf { it.any(Char::isLetterOrDigit) }?.let(result::add)
                    start = end
                    i = end
                    continue
                }
            }
            i++
        }
        spoken.substring(start).trim().takeIf { it.any(Char::isLetterOrDigit) }?.let(result::add)
        return result
    }

    private fun readableText(text: String, depth: Int): String {
        if (depth > 4) return ""
        val out = StringBuilder()
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (c == '`' || (c == '~' && text.startsWith("~~~", i))) {
                val marker = c.toString().repeat(if (text.startsWith("$c$c$c", i)) 3 else 1)
                val close = text.indexOf(marker, i + marker.length)
                out.append(' ')
                i = if (close < 0) text.length else close + marker.length
                continue
            }
            if (startsUrl(text, i)) {
                var end = i
                while (end < text.length && !text[end].isWhitespace() && text[end] != '<' && text[end] != '>') end++
                var trailing = end
                while (trailing > i && text[trailing - 1] in URL_TRAILING_PUNCTUATION) trailing--
                out.append(' ')
                out.append(text, trailing, end)
                i = end
                continue
            }
            if (c == '<') {
                val end = text.indexOf('>', i + 1)
                out.append(' ')
                i = if (end < 0) text.length else end + 1
                continue
            }
            if (c == '[') {
                val labelEnd = text.indexOf("](", i + 1)
                if (labelEnd >= 0) {
                    val targetEnd = closingParenthesis(text, labelEnd + 1)
                    if (targetEnd < 0) break
                    out.append(readableText(text.substring(i + 1, labelEnd), depth + 1))
                    i = targetEnd + 1
                    continue
                }
            }
            if (c !in MARKUP_CHARACTERS) out.append(c) else out.append(' ')
            i++
        }
        return out.toString()
    }

    private fun startsUrl(text: String, index: Int): Boolean =
        text.regionMatches(index, "https://", 0, 8, ignoreCase = true) ||
            text.regionMatches(index, "http://", 0, 7, ignoreCase = true) ||
            text.regionMatches(index, "www.", 0, 4, ignoreCase = true)

    private fun closingParenthesis(text: String, opening: Int): Int {
        var nesting = 0
        for (index in opening until text.length) {
            when (text[index]) {
                '(' -> nesting++
                ')' -> if (--nesting == 0) return index
            }
        }
        return -1
    }

    companion object {
        private val TERMINATORS = setOf('.', '!', '?', '؟', '۔', '…')
        private val CLOSING_PUNCTUATION = setOf('"', '\'', '”', '’', ')', ']', '»')
        private val URL_TRAILING_PUNCTUATION = setOf('.', '!', '?', '؟', '…', ',', '،', ';', '؛')
        private val MARKUP_CHARACTERS = setOf('#', '*', '_', '[', ']')
    }
}
