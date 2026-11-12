package com.mohammad.orion

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mohammad.orion.data.settings.AssistantNameRepository
import com.mohammad.orion.domain.settings.AssistantNameState
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AssistantNameRepositoryTest {
    @Test fun persistsValidNameAcrossStoreRecreationAndReset() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File.createTempFile("assistant-name-", ".preferences_pb", context.cacheDir).apply { delete() }
        val job = SupervisorJob()
        try {
            val repo = AssistantNameRepository(PreferenceDataStoreFactory.create(scope = CoroutineScope(job + Dispatchers.IO)) { file })
            assertEquals(AssistantNameState.Ready(null), repo.state.first())
            repo.setName("  دستیار  ")
            try { repo.setName("bad\nname"); fail("invalid name accepted") } catch (_: IllegalArgumentException) { }
            assertEquals(AssistantNameState.Ready("دستیار"), repo.state.first())
            job.cancelAndJoin()
            val nextJob = SupervisorJob()
            try {
                val restored = AssistantNameRepository(PreferenceDataStoreFactory.create(scope = CoroutineScope(nextJob + Dispatchers.IO)) { file })
                assertEquals(AssistantNameState.Ready("دستیار"), restored.state.first())
                restored.reset()
                assertEquals(AssistantNameState.Ready(null), restored.state.first())
            } finally { nextJob.cancelAndJoin() }
        } finally { job.cancelAndJoin(); file.delete() }
    }
}
