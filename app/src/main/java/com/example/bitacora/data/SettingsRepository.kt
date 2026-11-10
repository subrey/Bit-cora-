package com.example.bitacora.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Preferencias de la app (por ahora, el modo de tema).
 */
class SettingsRepository(private val context: Context) {

    private val themeKey = stringPreferencesKey("theme_mode")

    /** "system", "light" o "dark". */
    val themeMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[themeKey] ?: "system"
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { prefs -> prefs[themeKey] = mode }
    }
}
