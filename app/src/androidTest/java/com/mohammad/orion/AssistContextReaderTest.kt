package com.mohammad.orion

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mohammad.orion.data.visual.AssistContextReader
import com.mohammad.orion.domain.visual.ContextStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AssistContextReaderTest {
    @Test fun unavailableStructureReturnsNoContent() = runBlocking {
        val result = AssistContextReader().read(null)
        assertEquals(ContextStatus.UNAVAILABLE, result.status)
        assertEquals("", result.text)
    }
}
