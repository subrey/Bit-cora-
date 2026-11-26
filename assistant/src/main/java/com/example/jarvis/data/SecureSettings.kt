package com.example.jarvis.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Almacenamiento cifrado de la clave de la IA.
 *
 * Usa el Keystore de Android a través de [EncryptedSharedPreferences]: la clave
 * queda cifrada en el teléfono y no se guarda nunca en texto plano ni en el
 * historial de git. Si el Keystore no está disponible, la app lo avisa y no
 * guarda nada (nunca cae a preferencias sin cifrar).
 */
class SecureSettings(context: Context) {

    /**
     * Se abre en el primer uso, no al construir la clase: crear la clave del
     * Keystore puede tardar y no debe hacerlo el hilo principal al arrancar.
     */
    private val prefs: SharedPreferences? by lazy {
        runCatching {
            val masterKey = MasterKey.Builder(context.applicationContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context.applicationContext,
                FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }.getOrNull()
    }

    /** false si el Keystore del teléfono no ha podido abrirse. */
    val available: Boolean
        get() = prefs != null

    fun apiKey(): String = prefs?.getString(KEY_API, "").orEmpty()

    fun hasKey(): Boolean = apiKey().isNotBlank()

    fun setApiKey(value: String) {
        prefs?.edit()?.putString(KEY_API, value.trim())?.apply()
    }

    /** Segunda clave que se usa cuando la principal se queda sin cuota. */
    fun backupApiKey(): String = prefs?.getString(KEY_API_BACKUP, "").orEmpty()

    fun hasBackupKey(): Boolean = backupApiKey().isNotBlank()

    fun setBackupApiKey(value: String) {
        prefs?.edit()?.putString(KEY_API_BACKUP, value.trim())?.apply()
    }

    fun hasAnyKey(): Boolean = hasKey() || hasBackupKey() || hasGroqKey()

    /** Clave de Groq: segundo proveedor gratuito con búsqueda en navegador. */
    fun groqApiKey(): String = prefs?.getString(KEY_GROQ, "").orEmpty()

    fun hasGroqKey(): Boolean = groqApiKey().isNotBlank()

    fun setGroqApiKey(value: String) {
        prefs?.edit()?.putString(KEY_GROQ, value.trim())?.apply()
    }

    fun model(): String = prefs?.getString(KEY_MODEL, "").orEmpty()

    fun setModel(value: String) {
        prefs?.edit()?.putString(KEY_MODEL, value.trim())?.apply()
    }

    private companion object {
        const val FILE = "jarvis_secure"
        const val KEY_API = "gemini_api_key"
        const val KEY_API_BACKUP = "gemini_api_key_backup"
        const val KEY_GROQ = "groq_api_key"
        const val KEY_MODEL = "gemini_model"
    }
}
