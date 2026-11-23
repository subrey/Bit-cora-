package com.example.jarvis.assistant

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import java.net.URLEncoder

/**
 * Prepara mensajes de WhatsApp. Por seguridad no envía nada por segundo:
 * abre el chat con el texto ya escrito y el usuario pulsa «enviar».
 */
class WhatsAppSender(private val context: Context) {

    fun hasContactsPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CONTACTS,
        ) == PackageManager.PERMISSION_GRANTED

    fun isInstalled(): Boolean = runCatching {
        context.packageManager.getPackageInfo("com.whatsapp", 0)
        true
    }.getOrDefault(false)

    /** Normaliza un número o busca un contacto por nombre (coincidencia parcial). */
    fun resolveNumber(recipient: String): String? {
        val clean = recipient.trim()
        if (clean.isBlank()) return null
        val digits = clean.filter { it.isDigit() || it == '+' }
        if (digits.count { it.isDigit() } >= 7) {
            return digits.replace(Regex("[^0-9+]"), "")
        }
        if (!hasContactsPermission()) return null

        val projection = arrayOf(
            android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER,
        )
        val selection = "${android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val args = arrayOf("%$clean%")
        val uri = android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val cursor = runCatching {
            context.contentResolver.query(uri, projection, selection, args, null)
        }.getOrNull() ?: return null
        cursor.use {
            if (it.moveToFirst()) {
                val number = it.getString(0)?.trim().orEmpty()
                if (number.isNotBlank()) return number.replace(Regex("[^0-9+]"), "")
            }
        }
        return null
    }

    /**
     * Abre el chat con el número y el mensaje preparados. Devuelve false si
     * no hay ninguna aplicación capaz de abrir el enlace.
     */
    fun openChat(number: String, text: String): Boolean {
        val url = "https://api.whatsapp.com/send?phone=$number&text=" +
            URLEncoder.encode(text, "UTF-8")
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (isInstalled()) setPackage("com.whatsapp")
        }
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }
}
