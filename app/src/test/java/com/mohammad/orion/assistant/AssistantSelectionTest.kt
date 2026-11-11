package com.mohammad.orion.assistant

import org.junit.Assert.assertEquals
import org.junit.Test

class AssistantSelectionTest {
    @Test fun unavailableRoleAlwaysWins() {
        assertEquals(AssistantSelection.UNAVAILABLE, assistantSelection(false, false))
        assertEquals(AssistantSelection.UNAVAILABLE, assistantSelection(false, true))
    }
    @Test fun availableRoleReportsSelection() {
        assertEquals(AssistantSelection.SELECTED, assistantSelection(true, true))
        assertEquals(AssistantSelection.NOT_SELECTED, assistantSelection(true, false))
    }
}
