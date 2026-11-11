package com.mohammad.orion.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mohammad.orion.domain.settings.AppearanceLoad
import com.mohammad.orion.domain.settings.AppearanceSettings
import com.mohammad.orion.domain.settings.ThemeMode
import com.mohammad.orion.domain.settings.parseThemeMode
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/** Contains non-secret presentation preferences only. */
class AppearanceRepository(private val store: DataStore<Preferences>) {
    val state: Flow<AppearanceLoad> = store.data.map<Preferences, AppearanceLoad> { values ->
        AppearanceLoad.Ready(AppearanceSettings(parseThemeMode(values[THEME]), values[DYNAMIC] ?: false))
    }.catch { error ->
        if (error is IOException) emit(AppearanceLoad.Unavailable) else throw error
    }

    suspend fun setTheme(value: ThemeMode) { store.edit { it[THEME] = value.name } }
    suspend fun setDynamicColor(enabled: Boolean) { store.edit { it[DYNAMIC] = enabled } }
    suspend fun reset() { store.edit { it.remove(THEME); it.remove(DYNAMIC) } }

    companion object {
        private val THEME = stringPreferencesKey("appearance_theme")
        private val DYNAMIC = booleanPreferencesKey("appearance_dynamic_color")
    }
}
