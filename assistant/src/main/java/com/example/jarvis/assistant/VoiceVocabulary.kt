package com.example.jarvis.assistant

/**
 * Palabras y frases que se le indican al motor de voz como pistas.
 *
 * Android 13+ admite `EXTRA_BIASING_STRINGS`: con estas pistas el reconocedor
 * prefiere las palabras propias de JARVIS («música», «temporizador», «WhatsApp»)
 * frente a otras que suenan parecido. En versiones anteriores se ignora.
 */
object VoiceVocabulary {

    val commands: List<String> = listOf(
        "Jarvis",
        "pon música",
        "pausa la música",
        "quita la música",
        "sube el volumen",
        "baja el volumen",
        "siguiente canción",
        "canción anterior",
        "pon un temporizador",
        "recuérdame",
        "pon una alarma",
        "despiértame",
        "manda un WhatsApp",
        "abre WhatsApp",
        "qué hora es",
        "qué día es hoy",
        "qué tiempo hace",
        "enciende la linterna",
        "apaga la linterna",
        "cuánta batería queda",
        "añade tarea",
        "memoriza",
        "cuéntame un chiste",
    )
}
