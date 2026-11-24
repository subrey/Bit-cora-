package com.example.jarvis.assistant

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import java.text.Normalizer

/**
 * Prepara mensajes de WhatsApp. Por seguridad no envía nada por sí solo:
 * abre el chat con el texto ya escrito y el usuario pulsa «enviar».
 */
class WhatsAppSender(private val context: Context) {

    fun hasContactsPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CONTACTS,
        ) == PackageManager.PERMISSION_GRANTED

    /** Paquetes de WhatsApp (normal y Business) instalados en el teléfono. */
    private fun installedPackages(): List<String> =
        listOf("com.whatsapp", "com.whatsapp.w4b").filter { pkg ->
            runCatching { context.packageManager.getPackageInfo(pkg, 0) }.isSuccess
        }

    fun isInstalled(): Boolean = installedPackages().isNotEmpty()

    /** Quita acentos, signos y mayúsculas para comparar nombres. */
    private fun fold(text: String): String {
        val lower = text.lowercase()
        val plain = Normalizer.normalize(lower, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        return plain.replace("[^\\p{L}\\p{N}]".toRegex(), " ").trim()
    }

    /**
     * Devuelve el número de teléfono listo para WhatsApp: o el que escribió el
     * usuario, o el de un contacto cuyo nombre coincida (sin distinguir
     * acentos ni mayúsculas).
     */
    fun resolveNumber(recipient: String): String? {
        val clean = recipient.trim()
        if (clean.isBlank()) return null

        val typedDigits = clean.filter { it.isDigit() || it == '+' }
        if (typedDigits.count { it.isDigit() } >= 7) {
            return normalizePhone(typedDigits)
        }
        if (!hasContactsPermission()) return null

        val wanted = fold(clean)
        if (wanted.isBlank()) return null

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
        )
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val cursor = runCatching {
            context.contentResolver.query(uri, projection, null, null, null)
        }.getOrNull() ?: return null

        var exact: String? = null
        var starts: String? = null
        var contains: String? = null
        cursor.use { c ->
            val nameIndex = c.getColumnIndex(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            )
            val numberIndex = c.getColumnIndex(
                ContactsContract.CommonDataKinds.Phone.NUMBER,
            )
            while (c.moveToNext()) {
                if (nameIndex < 0 || numberIndex < 0) continue
                val name = fold(c.getString(nameIndex).orEmpty())
                val number = normalizePhone(c.getString(numberIndex).orEmpty())
                if (name.isBlank() || number.isBlank()) continue
                when {
                    name == wanted && exact == null -> exact = number
                    name.startsWith(wanted) && starts == null -> starts = number
                    name.contains(wanted) && contains == null -> contains = number
                }
                if (exact != null) break
            }
        }
        return exact ?: starts ?: contains
    }

    /**
     * Deja el número en formato internacional. WhatsApp rechaza los números
     * nacionales sin prefijo, así que los móviles españoles de 9 dígitos
     * reciben el prefijo 34.
     */
    fun normalizePhone(raw: String): String {
        var digits = raw.filter { it.isDigit() || it == '+' }
        if (digits.isBlank()) return ""
        if (digits.startsWith("00")) digits = digits.removePrefix("00")
        if (digits.startsWith("+")) digits = digits.removePrefix("+")
        digits = digits.filter { it.isDigit() }
        if (digits.length == 9 && (digits.startsWith("6") ||
                digits.startsWith("7") || digits.startsWith("9"))
        ) {
            return "34$digits"
        }
        return digits
    }

    /** Copia el mensaje al portapapeles para que se pueda pegar a mano. */
    fun copyMessage(text: String) {
        runCatching {
            val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            manager.setPrimaryClip(ClipData.newPlainText("WhatsApp", text))
        }
    }

    /** Abre WhatsApp sin destinatario, como último recurso. */
    fun openApp(): Boolean {
        for (pkg in installedPackages()) {
            val intent = context.packageManager.getLaunchIntentForPackage(pkg) ?: continue
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (runCatching { context.startActivity(intent) }.isSuccess) return true
        }
        return false
    }

    /**
     * Abre el chat con el número y el mensaje preparados.
     *
     * Prueba primero el esquema nativo de WhatsApp (`whatsapp://send`), luego
     * los enlaces `wa.me` y `api.whatsapp.com`. Devuelve false si ningún
     * método funciona.
     */
    fun openChat(number: String, text: String): Boolean {
        val phone = normalizePhone(number)
        if (phone.isBlank()) return false
        val encoded = Uri.encode(text)
        val packages = installedPackages()

        val candidates = mutableListOf<Intent>()
        // 1) Esquema nativo: el más fiable cuando WhatsApp está instalado.
        candidates += Intent(
            Intent.ACTION_VIEW,
            Uri.parse("whatsapp://send?phone=$phone&text=$encoded"),
        )
        // 2) Enlace oficial wa.me (abre WhatsApp o el navegador).
        candidates += Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://wa.me/$phone?text=$encoded"),
        )
        // 3) Enlace clásico api.whatsapp.com.
        candidates += Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://api.whatsapp.com/send?phone=$phone&text=$encoded"),
        )

        val targets = mutableListOf<String?>()
        targets.addAll(packages)
        targets.add(null)

        for (base in candidates) {
            for (pkg in targets) {
                val intent = Intent(base).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    if (pkg != null) setPackage(pkg)
                }
                if (intent.resolveActivity(context.packageManager) == null) continue
                if (runCatching { context.startActivity(intent) }.isSuccess) return true
            }
        }
        return false
    }
}
