package com.mohammad.orion.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/** Non-secret preference. Missing or unreadable settings must fail closed. */
class SecureScreenRepository(private val store: DataStore<Preferences>) {
    val state: Flow<SecureScreenState> = store.data.map<Preferences, SecureScreenState> { SecureScreenState.Ready(it[KEY] ?: true) }
        .catch { error ->
            if (error is CancellationException) throw error
            if (error is Exception) emit(SecureScreenState.Unavailable) else throw error
        }
    suspend fun setEnabled(enabled: Boolean) { store.edit { it[KEY] = enabled } }
    companion object { private val KEY = booleanPreferencesKey("secure_screen_enabled") }
}
