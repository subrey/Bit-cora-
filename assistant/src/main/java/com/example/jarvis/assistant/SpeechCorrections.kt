package com.example.jarvis.assistant

/**
 * Correcciones de palabras que el dictado confunde a menudo.
 *
 * Se aplican sobre el texto ya normalizado (minúsculas, sin acentos), así que
 * las claves van sin tildes. Solo se tocan palabras de las órdenes: nunca se
 * reescribe el mensaje de un WhatsApp ni el nombre de un contacto.
 */
object SpeechCorrections {

    private val pairs = listOf(
        "muscia" to "musica",
        "msuica" to "musica",
        "musia" to "musica",
        "mussica" to "musica",
        "volumn" to "volumen",
        "bolumen" to "volumen",
        "volmen" to "volumen",
        "temporisador" to "temporizador",
        "temporizdor" to "temporizador",
        "alrma" to "alarma",
        "literna" to "linterna",
        "wasap" to "whatsapp",
        "whatsap" to "whatsapp",
        "wassap" to "whatsapp",
        "guasap" to "whatsapp",
        "reproduse" to "reproduce",
        "siguente" to "siguiente",
        "sigiente" to "siguiente",
        "antrior" to "anterior",
        "deterner" to "detener",
        "paussa" to "pausa",
        "baterya" to "bateria",
    )

    /** Patrones ya compilados: el parser se ejecuta en cada orden de voz. */
    private val replacements = pairs.map { (wrong, right) ->
        Regex("\\b${Regex.escape(wrong)}\\b") to right
    }

    fun apply(normalized: String): String {
        var result = normalized
        for ((pattern, right) in replacements) {
            result = result.replace(pattern, right)
        }
        return result
    }
}
