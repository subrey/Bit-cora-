package com.example.jarvis.ai

/**
 * Limpieza de las citas automáticas que añade Gemini al buscar en la web.
 *
 * El modelo devuelve marcas como `【1†L14-L19】` o `[2†L34-L36]` pegadas al
 * texto. No aportan información al usuario y, leídas en voz alta, suenan a
 * números sueltos. JARVIS muestra las fuentes al final con sus títulos, así
 * que estas marcas sobran.
 */
object AiText {

    private val fullWidthCitations = Regex("【[^】]*】")
    private val bracketedCitations = Regex("\\[\\s*\\d+[^\\]]*\\]")
    private val spaceBeforePunctuation = Regex("[ \\t]+([.,;:!?])")
    private val repeatedSpaces = Regex("[ \\t]{2,}")

    /** Texto de la IA sin marcas de cita. Conserva los saltos de línea. */
    fun clean(text: String): String {
        var result = fullWidthCitations.replace(text, "")
        result = bracketedCitations.replace(result, "")
        result = result.replace(spaceBeforePunctuation, "$1")
        result = result.replace(repeatedSpaces, " ")
        return result.trim()
    }
}
