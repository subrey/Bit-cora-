package com.example.bitacora.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Guarda y sirve los ficheros multimedia adjuntos a las notas en almacenamiento interno.
 */
class MediaStore(private val context: Context) {

    private val dir: File
        get() = File(context.filesDir, "attachments").also { it.mkdirs() }

    /** Copia el contenido de [uri] al almacenamiento interno y devuelve el nombre del fichero. */
    fun importFromUri(uri: Uri, fallbackExtension: String = "bin"): String {
        val ext = extensionFor(uri) ?: fallbackExtension
        val name = "${UUID.randomUUID()}.$ext"
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(File(dir, name)).use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalArgumentException("No se pudo leer el archivo")
        return name
    }

    /** Guarda un bitmap (dibujo a mano) como PNG y devuelve el nombre del fichero. */
    fun saveBitmap(bitmap: Bitmap): String {
        val name = "${UUID.randomUUID()}.png"
        FileOutputStream(File(dir, name)).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return name
    }

    fun file(name: String): File = File(dir, name)

    fun delete(name: String) {
        runCatching { file(name).delete() }
    }

    fun contentUri(name: String): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file(name))

    private fun extensionFor(uri: Uri): String? {
        val mime = context.contentResolver.getType(uri)
        if (mime != null) {
            MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)?.let { return it }
        }
        val segment = uri.lastPathSegment ?: return null
        val dot = segment.lastIndexOf('.')
        return if (dot >= 0 && dot < segment.length - 1) segment.substring(dot + 1) else null
    }
}
