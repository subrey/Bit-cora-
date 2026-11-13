package com.example.jarvis.ai

/**
 * Construcción del contexto que se envía al modelo. Es Kotlin puro para poder
 * probarlo sin red ni Android.
 */
object AiPrompt {

    /** Cuántos turnos recientes se envían como contexto. */
    const val MAX_TURNS = 12

    /** Cuántos recuerdos del usuario se añaden al prompt. */
    private const val MAX_MEMORIES = 10

    /**
     * Añade los recuerdos guardados al prompt de personalidad. Si no hay
     * recuerdos, devuelve el prompt original sin tocar.
     */
    fun systemPrompt(base: String, memories: List<String>): String {
        val clean = memories
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .takeLast(MAX_MEMORIES)
        if (clean.isEmpty()) return base

        return buildString {
            append(base.trimEnd())
            appendLine()
            appendLine()
            appendLine("Recuerdos guardados del usuario (úsalos solo si vienen al caso):")
            clean.forEach { appendLine("- $it") }
        }.trimEnd()
    }

    /**
     * Recorta el historial y garantiza que termine en el mensaje actual del
     * usuario, sin duplicarlo si ya está guardado en la base de datos.
     */
    fun turns(history: List<AiTurn>, currentText: String): List<AiTurn> {
        val clean = currentText.trim()
        val filtered = history.filter { it.text.isNotBlank() }
        val last = filtered.lastOrNull()
        val alreadyIncluded = last != null && last.fromUser && last.text.trim() == clean
        val trimmed = filtered.takeLast(if (alreadyIncluded) MAX_TURNS else MAX_TURNS - 1)
        return if (alreadyIncluded) trimmed else trimmed + AiTurn(fromUser = true, text = clean)
    }
}
