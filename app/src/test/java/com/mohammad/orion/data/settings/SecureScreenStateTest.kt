package com.mohammad.orion.data.settings

import org.junit.Assert.*
import org.junit.Test

class SecureScreenStateTest {
    @Test fun defaultAndUnavailableFailClosed() {
        assertTrue(secureScreenRequired(SecureScreenState.Loading))
        assertTrue(secureScreenRequired(SecureScreenState.Unavailable))
        assertTrue(secureScreenRequired(SecureScreenState.Ready(true)))
        assertFalse(secureScreenRequired(SecureScreenState.Ready(false)))
    }
}
