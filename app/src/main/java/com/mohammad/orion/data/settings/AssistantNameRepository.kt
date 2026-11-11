package com.mohammad.orion.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mohammad.orion.domain.settings.AssistantNameState
import com.mohammad.orion.domain.settings.normalizeAssistantName
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/** Non-secret display preference. All writers must validate before persistence. */
class AssistantNameRepository(private val store: DataStore<Preferences>) {
    val state: Flow<AssistantNameState> = store.data.map<Preferences, AssistantNameState> { prefs ->
        AssistantNameState.Ready(prefs[NAME]?.let(::normalizeAssistantName))
    }.catch { error ->
        if (error is IOException) emit(AssistantNameState.Unavailable) else throw error
    }

    suspend fun setName(name: String) {
        val normalized = requireNotNull(normalizeAssistantName(name)) { "Invalid assistant name" }
        store.edit { it[NAME] = normalized }
    }
    suspend fun reset() { store.edit { it.remove(NAME) } }

    private companion object { val NAME = stringPreferencesKey("assistant_display_name") }
}
