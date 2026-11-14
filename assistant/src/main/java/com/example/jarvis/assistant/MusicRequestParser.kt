package com.example.jarvis.assistant

import java.text.Normalizer
import java.util.Locale

/** Tipo de búsqueda musical que ha entendido el usuario. */
enum class MusicRequestKind {
    /** Un género: «pon algo de rock». */
    GENRE,

    /** Un estado de ánimo: «pon música para estudiar». */
    MOOD,

    /** Un artista: «pon algo de Coldplay». */
    ARTIST,

    /** Un título: «pon la canción Bohemian Rhapsody». */
    TITLE,

    /** Una época: «pon algo de los 90». */
    ERA,
}

/** Lo que el usuario ha pedido, ya interpretado. */
data class MusicRequest(
    val kind: MusicRequestKind,
    /** Términos de búsqueda (género, artista, palabras clave…). */
    val terms: List<String>,
    /** Texto para decir en voz alta: «algo de rock». */
    val label: String,
)

/**
 * Interpreta frases como «pon algo de rock», «pon música para estudiar» o
 * «pon algo de Coldplay». Función pura: se puede probar en tests.
 */
object MusicRequestParser {

    /** Géneros reconocidos, con sus sinónimos en español e inglés. */
    val GENRES: Map<String, List<String>> = mapOf(
        "rock" to listOf("rock", "roca"),
        "pop" to listOf("pop", "pop latino"),
        "jazz" to listOf("jazz", "jazz"),
        "blues" to listOf("blues", "blues"),
        "clasica" to listOf("clasica", "clásica", "classica", "classical", "orquesta"),
        "electronica" to listOf("electronica", "electrónica", "electronic", "techno", "house", "dubstep", "dnb"),
        "hip hop" to listOf("hip hop", "hiphop", "rap", "rap"),
        "reggaeton" to listOf("reggaeton", "reguetón"),
        "salsa" to listOf("salsa"),
        "bachata" to listOf("bachata"),
        "flamenco" to listOf("flamenco", "flamencoa"),
        "folk" to listOf("folk"),
        "country" to listOf("country", "western"),
        "indie" to listOf("indie", "indie rock"),
        "metal" to listOf("metal", "metallica"),
        "punk" to listOf("punk"),
        "soul" to listOf("soul", "funk", "r&b"),
        "cumbia" to listOf("cumbia"),
        "merengue" to listOf("merengue"),
        "banda" to listOf("banda", "grupera"),
        "balada" to listOf("balada", "baladas", "ballad"),
        "instrumental" to listOf("instrumental", "instrumentales", "piano", "sin letra"),
        "latino" to listOf("latino", "latina"),
        "vaporwave" to listOf("vaporwave", "vaporwave", "synthwave", "synthpop"),
        "ambient" to listOf("ambient", "ambiente"),
        "chill" to listOf("chill", "chillout", "lofi", "lo-fi"),
    )

    /** Estados de ánimo y los géneros que suelen encajar. */
    val MOODS: Map<String, List<String>> = mapOf(
        "tranquilo" to listOf(
            "ambient", "chill", "clasica", "instrumental", "jazz", "folk", "balada", "soul",
        ),
        "estudiar" to listOf(
            "ambient", "chill", "clasica", "instrumental", "piano",
        ),
        "energia" to listOf(
            "rock", "electronica", "hip hop", "punk", "metal", "pop",
        ),
        "alegre" to listOf(
            "pop", "salsa", "reggaeton", "cumbia", "merengue", "soul", "dance",
        ),
        "triste" to listOf(
            "blues", "balada", "soul", "clasica", "flamenco",
        ),
        "romantico" to listOf(
            "balada", "soul", "pop", "flamenco", "jazz", "banda",
        ),
        "fiesta" to listOf(
            "salsa", "reggaeton", "cumbia", "merengue", "electronica", "pop", "latin",
        ),
    )

    private val MOOD_WORDS = mapOf(
        "tranquilo" to listOf("tranquilo", "tranquila", "relax", "relajado", "relajada", "calma", "calmo", "suave", "dormir", "descanso", "chill"),
        "estudiar" to listOf("estudiar", "estudio", "concentrarme", "concentrarse", "trabajar", "trabajo", "leer", "examen", "focus"),
        "energia" to listOf("energia", "energico", "energetico", "gimnasio", "gym", "entrenar", "correr", "motivacion", "motivante", "fiestita"),
        "alegre" to listOf("alegre", "alegria", "feliz", "divertido", "diversion", "contenta"),
        "triste" to listOf("triste", "melancolico", "melancolica", "solo", "sombrío"),
        "romantico" to listOf("romantico", "romantica", "amor", "enamorado", "pareja"),
        "fiesta" to listOf("fiesta", "celebrar", "celebracion", "bailar", "baile"),
    )

    /** Palabras que indican que se pide algo concreto por su nombre. */
    private val ARTIST_HINTS = listOf("de", "del", "por", "artista", "grupo", "banda de")

    /** Palabras que indican el título de una canción concreta. */
    private val TITLE_HINTS = listOf(
        "cancion", "pista", "tema", "canciones",
    )

    /** Normaliza como hace el resto del asistente: sin acentos y en minúsculas. */
    fun normalize(text: String): String {
        val lower = text.lowercase(Locale.forLanguageTag("es"))
        val noAccents = Normalizer.normalize(lower, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        return noAccents.replace(Regex("\\s+"), " ").trim()
    }

    /**
     * Interpreta la frase. Devuelve null si no hay ninguna pista de búsqueda.
     */
    fun parse(query: String): MusicRequest? {
        val text = normalize(query)
        if (text.isBlank()) return null

        // 1. Estado de ánimo: «algo para estudiar», «música tranquila».
        for ((mood, words) in MOOD_WORDS) {
            val hit = words.firstOrNull { text.contains(it) }
            if (hit != null) {
                val genres = MOODS[mood].orEmpty()
                return MusicRequest(
                    kind = MusicRequestKind.MOOD,
                    terms = genres,
                    label = "algo $mood",
                )
            }
        }

        // 2. Época: «algo de los 90», «música de los 80».
        ERA_REGEX.find(text)?.let { match ->
            val decade = match.groupValues[1]
            return MusicRequest(
                kind = MusicRequestKind.ERA,
                terms = listOf(decade),
                label = "algo de los $decade",
            )
        }

        // 3. Género: «algo de rock», «música jazz».
        val genre = findGenre(text)
        if (genre != null) {
            return MusicRequest(
                kind = MusicRequestKind.GENRE,
                terms = listOf(genre),
                label = "algo de $genre",
            )
        }

        // 4. Artista o título: «algo de Coldplay», «la canción Bohemian Rhapsody».
        val quoted = QUOTED_REGEX.find(text)?.groupValues?.get(1)
        if (!quoted.isNullOrBlank()) {
            return MusicRequest(
                kind = MusicRequestKind.ARTIST,
                terms = listOf(normalize(quoted)),
                label = quoted,
            )
        }
        val afterHint = ARTIST_HINTS.firstNotNullOfOrNull { hint ->
            val index = text.indexOf(" $hint ")
            if (index < 0) null else text.substring(index + hint.length + 2).trim()
        }
        if (!afterHint.isNullOrBlank()) {
            val name = afterHint.substringBefore(" en ").trim()
            if (name.length >= 3) {
                return MusicRequest(
                    kind = MusicRequestKind.ARTIST,
                    terms = listOf(name),
                    label = name,
                )
            }
        }

        // Título: «la canción Billie Jean», «pon la pista X».
        val title = TITLE_HINTS.firstNotNullOfOrNull { hint ->
            val index = text.indexOf(" $hint ")
            if (index < 0) null else text.substring(index + hint.length + 2).trim()
        }
        if (!title.isNullOrBlank()) {
            val name = title
                .removePrefix("de ")
                .removePrefix("por ")
                .substringBefore(" en ")
                .trim()
            if (name.length >= 3) {
                return MusicRequest(
                    kind = MusicRequestKind.TITLE,
                    terms = listOf(name),
                    label = name,
                )
            }
        }

        return null
    }

    /** Devuelve el nombre del género si la frase lo menciona. */
    fun findGenre(text: String): String? {
        val clean = normalize(text)
        for ((genre, aliases) in GENRES) {
            for (alias in aliases) {
                if (clean.contains(alias)) return genre
            }
        }
        return null
    }

    private val ERA_REGEX = Regex("\\b(?:de|del|los|las)\\s+(70|80|90|00)s?\\b")
    private val QUOTED_REGEX = Regex("[\"'«”]([^\"'«”]+)[\"'»]")

    /** Años que encajan con una década pedida («90» → 1990-1999). */
    fun yearsOf(decade: String): IntRange {
        val value = decade.toIntOrNull() ?: return 0..0
        val start = if (value == 0) 2000 else value + 1900
        return start..(start + 9)
    }
}
