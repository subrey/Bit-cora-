package com.example.jarvis.assistant

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.jarvis.domain.model.ChatMessage
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Exporta la conversación a un archivo de texto y lo ofrece para compartir.
 *
 * [format] es una función pura (sin Android) para poder probarla en tests.
 */
object ChatExporter {

    private val spanish = Locale.forLanguageTag("es")
    private val stamp = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy 'a las' HH:mm", spanish)

    /** Texto plano con la conversación completa, lista para guardar o compartir. */
    fun format(messages: List<ChatMessage>): String = buildString {
        appendLine("HISTORIAL DE JARVIS")
        appendLine("==============================")
        appendLine()
        if (messages.isEmpty()) {
            appendLine("(no hay conversación guardada)")
            return@buildString
        }
        messages.forEach { message ->
            val hora = Instant.ofEpochMilli(message.createdAt)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
                .format(stamp)
            val quien = if (message.fromUser) "TÚ" else "JARVIS"
            appendLine("[$hora] $quien: ${message.text}")
            appendLine()
        }
    }

    /**
     * Guarda el texto en la caché de la app y abre el menú de compartir.
     * Devuelve false si no se ha podido guardar o abrir.
     */
    fun saveAndShare(context: Context, text: String): Boolean {
        val file = writeFile(context, text) ?: return false
        val uri = runCatching {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }.getOrNull() ?: return false

        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Historial de JARVIS")
            putExtra(Intent.EXTRA_TEXT, "Historial de conversación con JARVIS.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(send, "Compartir historial")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { context.startActivity(chooser) }.isSuccess
    }

    /** Escribe el archivo en la caché y devuelve su ruta. */
    fun writeFile(context: Context, text: String): File? = runCatching {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val name = "jarvis-historial-" + System.currentTimeMillis() + ".txt"
        val file = File(dir, name)
        file.writeText(text)
        file
    }.getOrNull()
}
