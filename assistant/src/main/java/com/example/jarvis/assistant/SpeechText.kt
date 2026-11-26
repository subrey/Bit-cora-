package com.example.jarvis.assistant

import com.example.jarvis.ai.AiText

/**
 * Prepara el texto antes de llevárselo al motor de voz.
 *
 * Los motores TTS de algunos móviles leen las cifras como una sucesión de
 * letras o las pronuncian mal («balbuceos»): por ejemplo `07:30`, `12.5 GB` o
 * `-3`. Aquí se convierten a palabras en español. El texto del chat NO se
 * toca: esta transformación se aplica solo a lo que se dice en voz alta.
 */
object SpeechText {

    private val units = arrayOf(
        "", "uno", "dos", "tres", "cuatro", "cinco", "seis", "siete", "ocho", "nueve",
    )
    private val teens = arrayOf(
        "diez", "once", "doce", "trece", "catorce", "quince",
        "dieciseis", "diecisiete", "dieciocho", "diecinueve",
    )
    private val twenties = arrayOf(
        "", "veintiuno", "veintidos", "veintitres", "veinticuatro", "veinticinco",
        "veintiseis", "veintisiete", "veintiocho", "veintinueve",
    )
    private val tens = arrayOf(
        "", "diez", "veinte", "treinta", "cuarenta", "cincuenta",
        "sesenta", "setenta", "ochenta", "noventa",
    )
    private val hundreds = arrayOf(
        "", "ciento", "doscientos", "trescientos", "cuatrocientos", "quinientos",
        "seiscientos", "setecientos", "ochocientos", "novecientos",
    )

    private val clock = Regex("\\b(\\d{1,2}):(\\d{2})\\b")
    private val decimal = Regex("(\\d+)[.,](\\d+)")
    private val signedDecimal = Regex("(^|[\\s:()-])-(\\d+)[.,](\\d+)")
    private val signed = Regex("(^|[\\s:()-])-(\\d+)")
    private val digits = Regex("\\d+")
    private val spaces = Regex("\\s+")
    private val sourcesBlock = Regex("(?s)\\n\\s*Fuentes\\s*:.*")
    private val urls = Regex("https?://\\S+")

    /** Texto listo para pronunciar. */
    fun forSpeech(text: String): String {
        var result = AiText.clean(text.trim())
        if (result.isEmpty()) return result

        // Las fuentes y las marcas de formato se ven en pantalla, pero no se
        // leen: dichas en voz alta suenan a números y letras sueltas.
        result = sourcesBlock.replace(result, "")
        result = urls.replace(result, "")
        result = result.replace("**", "").replace("•", "").replace("#", "")

        // Unidades que el TTS suele deletrear letra a letra.
        result = result.replace(Regex("\\bGB\\b", RegexOption.IGNORE_CASE), "gigabytes")
        result = result.replace(Regex("\\bMB\\b", RegexOption.IGNORE_CASE), "megabytes")
        result = result.replace(Regex("\\bkm\\b", RegexOption.IGNORE_CASE), "kilometros")
        result = result.replace(Regex("\\bkg\\b", RegexOption.IGNORE_CASE), "kilos")
        result = result.replace("%", " por ciento")
        result = result.replace("°C", " grados").replace("ºC", " grados")
        result = result.replace("°", " grados")
        result = result.replace("/", " por ")
        // Quita el hueco que queda delante de la puntuación al sustituir.
        result = result.replace(Regex("\\s+([.,;:!?])"), "$1")

        // Horas: «Son las 07:30» → «Son las siete y media».
        result = clock.replace(result) { match ->
            val hour = say(match.groupValues[1].toLongOrNull() ?: 0L)
            val minute = when (match.groupValues[2].toIntOrNull()) {
                0 -> "en punto"
                15 -> "y cuarto"
                30 -> "y media"
                45 -> "menos cuarto"
                else -> "y ${say(match.groupValues[2].toLongOrNull() ?: 0L)}"
            }
            "$hour $minute"
        }

        // Decimal negativo antes que el entero: «-3.5» → «menos tres coma cinco».
        result = signedDecimal.replace(result) { match ->
            val whole = match.groupValues[2].toLongOrNull()
            val fraction = match.groupValues[3].toLongOrNull()
            if (whole == null || fraction == null) {
                match.value
            } else {
                "${match.groupValues[1]}menos ${say(whole)} coma ${say(fraction)}"
            }
        }

        // Números negativos enteros: «-3» → «menos tres».
        result = signed.replace(result) { match ->
            val number = match.groupValues[2].toLongOrNull()
            "${match.groupValues[1]}menos ${if (number == null) match.groupValues[2] else say(number)}"
        }

        // Decimales: en español se leen con coma, no con punto.
        result = decimal.replace(result) { match ->
            val whole = match.groupValues[1].toLongOrNull()
            val fraction = match.groupValues[2].toLongOrNull()
            if (whole == null || fraction == null) {
                match.value
            } else {
                "${say(whole)} coma ${say(fraction)}"
            }
        }

        // Resto de cifras sueltas.
        result = digits.replace(result) { match ->
            val value = match.value.toLongOrNull()
            if (value == null || match.value.length > 12) {
                match.value.map { units[it - '0'] }.joinToString(" ")
            } else {
                say(value)
            }
        }

        return result.replace(spaces, " ").trim()
    }

    /** Convierte un entero a palabras en español (hasta mil millones). */
    internal fun say(value: Long): String {
        if (value == 0L) return "cero"
        if (value < 0) return "menos ${say(-value)}"
        val number = value

        val million = number / 1_000_000
        val thousand = (number / 1_000) % 1_000
        val rest = number % 1_000
        val parts = mutableListOf<String>()

        if (million > 0) {
            parts += if (million == 1L) "un millon" else "${say(million)} millones"
        }
        if (thousand > 0) {
            parts += if (thousand == 1L) "mil" else "${underThousand(thousand)} mil"
        }
        if (rest > 0) {
            parts += underThousand(rest)
        }
        return parts.joinToString(" ")
    }

    private fun underThousand(value: Long): String {
        val hundred = (value / 100).toInt()
        val rest = (value % 100).toInt()
        val parts = mutableListOf<String>()
        if (hundred > 0) {
            parts += if (hundred == 1 && rest == 0) "cien" else hundreds[hundred]
        }
        if (rest > 0) parts += underHundred(rest)
        return parts.joinToString(" ")
    }

    private fun underHundred(value: Int): String = when {
        value < 10 -> units[value]
        value < 20 -> teens[value - 10]
        value == 20 -> tens[2]
        value < 30 -> twenties[value - 20]
        else -> {
            val ten = value / 10
            val unit = value % 10
            if (unit == 0) tens[ten] else "${tens[ten]} y ${units[unit]}"
        }
    }
}
