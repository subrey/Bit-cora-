package com.example.jarvis.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.jarvisDataStore by preferencesDataStore(name = "jarvis_settings")

/** Preferencias del asistente (voz, escucha siempre activa, palabra clave…). */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val tts = booleanPreferencesKey("tts_enabled")
        val alwaysListening = booleanPreferencesKey("always_listening")
        val wakeWord = stringPreferencesKey("wake_word")
        val accent = stringPreferencesKey("accent")
        val onDeviceVoice = booleanPreferencesKey("on_device_voice")
    }

    val ttsEnabled: Flow<Boolean> = context.jarvisDataStore.data.map { it[Keys.tts] ?: true }

    val alwaysListening: Flow<Boolean> =
        context.jarvisDataStore.data.map { it[Keys.alwaysListening] ?: false }

    val wakeWord: Flow<String> = context.jarvisDataStore.data.map { it[Keys.wakeWord] ?: "jarvis" }

    val accent: Flow<String> = context.jarvisDataStore.data.map { it[Keys.accent] ?: "green" }

    /**
     * Voz en el dispositivo: no pita ni usa red, pero necesita el modelo de voz
     * descargado en el sistema; si no está, no reconoce nada.
     */
    val onDeviceVoice: Flow<Boolean> = context.jarvisDataStore.data.map { it[Keys.onDeviceVoice] ?: false }

    suspend fun setOnDeviceVoice(value: Boolean) {
        context.jarvisDataStore.edit { it[Keys.onDeviceVoice] = value }
    }

    suspend fun setTtsEnabled(value: Boolean) {
        context.jarvisDataStore.edit { it[Keys.tts] = value }
    }

    suspend fun setAlwaysListening(value: Boolean) {
        context.jarvisDataStore.edit { it[Keys.alwaysListening] = value }
    }

    suspend fun setWakeWord(value: String) {
        context.jarvisDataStore.edit { it[Keys.wakeWord] = value }
    }

    suspend fun setAccent(value: String) {
        context.jarvisDataStore.edit { it[Keys.accent] = value }
    }
}
