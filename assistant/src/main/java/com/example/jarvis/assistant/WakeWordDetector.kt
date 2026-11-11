package com.example.jarvis.assistant

/**
 * Detección de la palabra clave. Es deliberadamente tolerante: con ruido o
 * música de fondo el reconocedor suele devolver «halvis», «jarvls» o «javis»
 * en lugar de «jarvis».
 *
 * Además la orden se extrae quitando la palabra clave de donde aparezca,
 * así que da igual si el usuario dice «Jarvis, pon música» o «pon música, Jarvis».
 */
object WakeWordDetector {

    private val fillers = setOf(
        "oye", "hey", "hey jarvis", "hola", "ok", "oka", "vale", "por favor", "favor",
        "porfa", "a ver", "assistant", "asistente",
    )

    /** ¿Este token es (casi) la palabra clave? */
    fun isWakeWord(token: String, wakeWord: String): Boolean {
        val candidate = clean(token)
        val target = clean(wakeWord)
        if (candidate.isBlank() || target.isBlank()) return false
        if (candidate == target) return true
        if (candidate.length < 5) return false

        val diff = distance(candidate, target)
        if (diff == 1) return true
        // Con ruido el motor suele cambiar la primera letra («halvis» por
        // «jarvis»): dos diferencias valen si el final sigue sonando igual.
        if (diff == 2 && candidate.endsWith(target.takeLast(2))) return true
        return false
    }

    /**
     * Devuelve la orden sin la palabra clave, o null si no se ha detectado.
     * Ejemplo: «Oye jarvis, pon música» → «pon música».
     */
    fun extractCommand(spoken: String, wakeWord: String): String? {
        val tokens = spoken.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return null

        val wakeIndex = tokens.indexOfFirst { isWakeWord(it, wakeWord) }
        if (wakeIndex < 0) return null

        val withoutWake = tokens.filterIndexed { index, _ -> index != wakeIndex }
        val cleaned = withoutWake
            .map { it.trimEnd('.', ',', '!', '?', ';', ':') }
            .dropWhile { clean(it) in fillers }
            .joinToString(" ")
            .trim()
        return cleaned.ifBlank { null }
    }

    /** ¿La frase contiene la palabra clave (aunque esté mal reconhecida)? */
    fun containsWakeWord(spoken: String, wakeWord: String): Boolean =
        spoken.split(Regex("\\s+")).any { isWakeWord(it, wakeWord) }

    /** Normaliza un token: minúsculas, sin acentos y sin signos. */
    fun clean(text: String): String =
        IntentParser.normalize(text)
            .replace(Regex("[^\\p{L}\\p{N}]"), "")
            .trim()

    /** Distancia de Levenshtein. */
    fun distance(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        val previous = IntArray(b.length + 1) { it }
        val current = IntArray(b.length + 1)
        for (i in 1..a.length) {
            current[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                current[j] = minOf(current[j - 1] + 1, previous[j] + 1, previous[j - 1] + cost)
            }
            System.arraycopy(current, 0, previous, 0, current.size)
        }
        return previous[b.length]
    }
}
