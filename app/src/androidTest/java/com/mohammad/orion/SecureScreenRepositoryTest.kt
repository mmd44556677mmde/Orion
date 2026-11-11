package com.mohammad.orion

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mohammad.orion.data.settings.SecureScreenRepository
import com.mohammad.orion.data.settings.SecureScreenState
import com.mohammad.orion.data.settings.secureScreenRequired
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SecureScreenRepositoryTest {
    @Test fun defaultIsProtectedAndExplicitChoicePersists() = runBlocking {
        val file = File.createTempFile("orion-security-", ".preferences_pb", InstrumentationRegistry.getInstrumentation().targetContext.cacheDir).apply { delete() }
        val job = SupervisorJob()
        try {
            val repo = SecureScreenRepository(PreferenceDataStoreFactory.create(scope = CoroutineScope(job + Dispatchers.IO)) { file })
            assertEquals(SecureScreenState.Ready(true), repo.state.first())
            repo.setEnabled(false)
            assertEquals(SecureScreenState.Ready(false), repo.state.first())
        } finally { job.cancelAndJoin(); file.delete() }
    }
    @Test fun failedOrLoadingSettingsFailClosed() = runBlocking {
        val store = object : DataStore<Preferences> {
            override val data: Flow<Preferences> = flow { throw IOException() }
            override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences = throw IOException()
        }
        assertEquals(SecureScreenState.Unavailable, SecureScreenRepository(store).state.first())
        val corruptType = object : DataStore<Preferences> {
            override val data: Flow<Preferences> = flow { throw IllegalStateException() }
            override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences = throw IllegalStateException()
        }
        assertEquals(SecureScreenState.Unavailable, SecureScreenRepository(corruptType).state.first())
        assertTrue(secureScreenRequired(SecureScreenState.Loading))
        assertTrue(secureScreenRequired(SecureScreenState.Unavailable))
        assertFalse(secureScreenRequired(SecureScreenState.Ready(false)))
    }
}
