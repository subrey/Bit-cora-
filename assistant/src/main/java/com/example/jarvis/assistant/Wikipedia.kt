package com.example.jarvis.assistant

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Consulta breve en Wikipedia en español. No necesita clave ni cuenta:
 * usa la API pública de resumen y búsqueda.
 */
object Wikipedia {

    private const val SEARCH_URL =
        "https://es.wikipedia.org/w/api.php?action=query&list=search&format=json&srlimit=1&srsearch="
    private const val SUMMARY_URL = "https://es.wikipedia.org/api/rest_v1/page/summary/"

    /** Devuelve un resumen corto del asunto, o null si no hay nada. */
    suspend fun summary(subject: String): String? = withContext(Dispatchers.IO) {
        val query = subject.trim()
        if (query.length < 3) return@withContext null
        runCatching {
            val title = searchTitle(query) ?: return@runCatching null
            fetchSummary(title)
        }.getOrNull()
    }

    private fun searchTitle(query: String): String? {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val body = get(SEARCH_URL + encoded) ?: return null
        val json = JSONObject(body)
        val results = json.optJSONObject("query")?.optJSONArray("search")
        if (results == null || results.length() == 0) return null
        return results.optJSONObject(0)?.optString("title")
    }

    private fun fetchSummary(title: String): String? {
        val encoded = URLEncoder.encode(title.replace(" ", "_"), "UTF-8")
        val body = get(SUMMARY_URL + encoded) ?: return null
        val json = JSONObject(body)
        val extract = json.optString("extract").trim()
        if (extract.isBlank()) return null
        return extract
    }

    private fun get(url: String): String? = runCatching {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.setRequestProperty("User-Agent", "Jarvis-Android/1.0 (asistente local)")
        connection.connectTimeout = 8000
        connection.readTimeout = 8000
        if (connection.responseCode !in 200..299) return@runCatching null
        connection.inputStream.bufferedReader().use { it.readText() }
    }.getOrNull()

    /** Recorta el texto a las primeras frases para no leer artículos enteros. */
    fun shorten(text: String, maxChars: Int = 420): String {
        if (text.length <= maxChars) return text
        val cut = text.take(maxChars)
        val lastStop = cut.lastIndexOfAny(charArrayOf('.', '!', '?'))
        return if (lastStop > 60) cut.take(lastStop + 1) else "$cut…"
    }
}
