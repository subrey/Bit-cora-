package com.example.jarvis.assistant

/**
 * Reglas puras del diálogo hablado, separadas de Android para poder probarlas
 * sin emulador ni micrófono.
 */
object VoicePolicy {

    /**
     * ¿Debe abrirse el micrófono otra vez al terminar una respuesta?
     *
     * Solo se encadena la conversación si el turno nació de la voz, si el
     * usuario activó la conversación continua, si la app está en pantalla y si
     * no fue él quien interrumpió a JARVIS.
     */
    fun shouldAutoListen(
        continuous: Boolean,
        fromVoice: Boolean,
        appVisible: Boolean,
        interrupted: Boolean,
    ): Boolean = continuous && fromVoice && appVisible && !interrupted

    /**
     * Errores que merecen un reintento silencioso: el micrófono estaba ocupado
     * o el motor se reinició por su cuenta.
     */
    fun isTransientError(error: Int): Boolean =
        error == ERROR_CLIENT || error == ERROR_RECOGNIZER_BUSY

    /**
     * Errores normales cuando el usuario no dice nada: no hay que alarmarle,
     * basta con volver al estado de espera.
     */
    fun isSilenceError(error: Int): Boolean =
        error == ERROR_SPEECH_TIMEOUT || error == ERROR_NO_MATCH

    // Copias de las constantes de SpeechRecognizer para no depender del SDK
    // de Android en las pruebas unitarias.
    private const val ERROR_CLIENT = 5
    private const val ERROR_SPEECH_TIMEOUT = 6
    private const val ERROR_NO_MATCH = 7
    private const val ERROR_RECOGNIZER_BUSY = 8
}
