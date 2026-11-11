package com.mohammad.orion

import com.mohammad.orion.core.AppDispatchers
import com.mohammad.orion.core.OrionError
import com.mohammad.orion.core.SafeEvent
import com.mohammad.orion.core.SafeLogger
import com.mohammad.orion.ui.design.OrionStatus
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.*
import org.junit.Test

class FoundationTest {
    @Test fun dispatchersCanBeInjectedIndependently() {
        val dispatchers = AppDispatchers(Dispatchers.Unconfined, Dispatchers.Unconfined, Dispatchers.Unconfined)
        assertSame(Dispatchers.Unconfined, dispatchers.io)
        assertSame(Dispatchers.Unconfined, dispatchers.main)
        assertSame(Dispatchers.Unconfined, dispatchers.default)
    }
    @Test fun statusLabelsAreDistinctAndCoverEveryState() {
        assertEquals(5, OrionStatus.entries.size)
        assertEquals(5, OrionStatus.entries.map { it.label }.toSet().size)
    }
    @Test fun errorsAndLogsUseClosedTypesRatherThanPrivateText() {
        val events = mutableListOf<SafeEvent>()
        val logger = object : SafeLogger { override fun event(event: SafeEvent) { events.add(event) } }
        logger.event(SafeEvent.APPLICATION_STARTED)
        assertEquals(listOf(SafeEvent.APPLICATION_STARTED), events)
        assertNotEquals(OrionError.InvalidInput, OrionError.Unexpected)
        assertNotEquals(OrionError.Unauthorized, OrionError.Unavailable)
    }
}
