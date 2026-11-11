package com.mohammad.orion.domain.settings

import org.junit.Assert.*
import org.junit.Test

class AssistantNameTest {
    @Test fun acceptsTrimmedPersianAndEnglishNames() {
        assertEquals("Nova", normalizeAssistantName("  Nova  "))
        assertEquals("دستیار من", normalizeAssistantName("دستیار من"))
        assertEquals("می\u200cنا", normalizeAssistantName("می\u200cنا"))
        assertEquals("x".repeat(40), normalizeAssistantName("x".repeat(40)))
    }
    @Test fun rejectsEmptyLongMultilineAndInvisibleControls() {
        listOf("", "  ", "x".repeat(41), "line\nbreak", "A\u202eB", "A\u0000B").forEach { assertNull(normalizeAssistantName(it)) }
    }
}
