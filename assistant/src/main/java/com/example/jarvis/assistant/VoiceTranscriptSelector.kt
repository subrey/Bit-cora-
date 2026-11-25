package com.example.jarvis.assistant

/**
 * Elige la mejor transcripción entre las alternativas que devuelve el motor.
 *
 * El motor suele ordenarlas por confianza, así que se respeta la primera. Solo
 * se cambia si la primera no se puede interpretar (UNKNOWN) y otra alternativa
 * sí: eso ocurre a menudo cuando la primera contiene ruido o una palabra mal
 * oída y la segunda es la orden real.
 */
object VoiceTranscriptSelector {

    /** Para el micrófono de la app: no hace falta palabra clave. */
    fun best(candidates: List<String>): String =
        choose(candidates, wakeWord = null, requireWake = false)

    /** Para la escucha permanente: exige la palabra clave si [requireWake]. */
    fun best(candidates: List<String>, wakeWord: String, requireWake: Boolean): String =
        choose(candidates, wakeWord, requireWake)

    private fun choose(
        candidates: List<String>,
        wakeWord: String?,
        requireWake: Boolean,
    ): String {
        val clean = candidates.map { it.trim() }.filter { it.isNotBlank() }
        if (clean.isEmpty()) return ""

        val pool = if (requireWake && wakeWord != null) {
            clean.filter { WakeWordDetector.containsWakeWord(it, wakeWord) }
        } else {
            clean
        }
        // Ninguna alternativa contiene la palabra clave: se devuelve la primera
        // para que el servicio pueda avisar de lo que ha oído.
        if (pool.isEmpty()) return clean.first()

        val first = pool.first()
        if (kindOf(first, wakeWord, requireWake) != IntentKind.UNKNOWN) return first
        return pool.firstOrNull { kindOf(it, wakeWord, requireWake) != IntentKind.UNKNOWN }
            ?: first
    }

    private fun kindOf(text: String, wakeWord: String?, requireWake: Boolean): IntentKind {
        val command = if (requireWake && wakeWord != null) {
            WakeWordDetector.extractCommand(text, wakeWord).orEmpty()
        } else {
            text
        }
        return if (command.isBlank()) IntentKind.UNKNOWN else IntentParser.parse(command).kind
    }
}
