package com.mohammad.orion

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mohammad.orion.data.settings.AppearanceRepository
import com.mohammad.orion.domain.settings.AppearanceLoad
import com.mohammad.orion.domain.settings.AppearanceSettings
import com.mohammad.orion.domain.settings.ThemeMode
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
class AppearanceRepositoryTest {
    @Test fun changesSurviveRecreationAndResetOnlyAppearance() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File.createTempFile("orion-appearance-", ".preferences_pb", context.cacheDir).apply { delete() }
        val job = SupervisorJob()
        val store = PreferenceDataStoreFactory.create(scope = CoroutineScope(job + Dispatchers.IO)) { file }
        val repo = AppearanceRepository(store)
        val unrelated = stringPreferencesKey("unrelated")
        try {
            assertEquals(AppearanceSettings(), (repo.state.first() as AppearanceLoad.Ready).settings)
            repo.setTheme(ThemeMode.LIGHT)
            repo.setDynamicColor(true)
            store.edit { it[unrelated] = "kept" }
            assertEquals(AppearanceSettings(ThemeMode.LIGHT, true), (repo.state.first() as AppearanceLoad.Ready).settings)
            job.cancelAndJoin()
            val nextJob = SupervisorJob()
            try {
                val nextStore = PreferenceDataStoreFactory.create(scope = CoroutineScope(nextJob + Dispatchers.IO)) { file }
                val restored = AppearanceRepository(nextStore)
                assertEquals(AppearanceSettings(ThemeMode.LIGHT, true), (restored.state.first() as AppearanceLoad.Ready).settings)
                restored.reset()
                assertEquals(AppearanceSettings(), (restored.state.first() as AppearanceLoad.Ready).settings)
                assertEquals("kept", nextStore.data.first()[unrelated])
            } finally { nextJob.cancelAndJoin() }
        } finally { job.cancelAndJoin(); file.delete() }
    }
    @Test fun readAndWriteFailuresAreNotSilentlyReportedAsSuccess() = runBlocking {
        val failing = object : DataStore<Preferences> {
            override val data: Flow<Preferences> = flow { throw IOException() }
            override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences = throw IOException()
        }
        val repo = AppearanceRepository(failing)
        assertEquals(AppearanceLoad.Unavailable, repo.state.first())
        try { repo.setTheme(ThemeMode.SYSTEM); fail("Expected an I/O failure") } catch (_: IOException) {}
    }
}
