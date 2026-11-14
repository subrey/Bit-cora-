package com.example.jarvis.assistant

/**
 * Busca en la biblioteca musical las pistas que encajan con lo que ha pedido el
 * usuario. Función pura: se puede probar en tests.
 */
object MusicSearch {

    /** Puntuación de una pista para una petición. */
    data class Hit(
        val track: Track,
        val score: Int,
    )

    /**
     * Devuelve las pistas ordenadas de mejor a peor ajuste. Solo las que
     * tienen alguna coincidencia.
     */
    fun search(tracks: List<Track>, request: MusicRequest): List<Hit> {
        val hits = tracks.mapNotNull { track ->
            val score = score(track, request)
            if (score > 0) Hit(track, score) else null
        }
        return hits.sortedWith(
            compareByDescending<Hit> { it.score }.thenBy { it.track.title },
        )
    }

    /** Puntuación de una pista: 0 significa que no encaja. */
    fun score(track: Track, request: MusicRequest): Int {
        val title = MusicRequestParser.normalize(track.title)
        val artist = MusicRequestParser.normalize(track.artist)
        val album = MusicRequestParser.normalize(track.album)
        val genre = MusicRequestParser.normalize(track.genre)

        return when (request.kind) {
            MusicRequestKind.GENRE -> scoreGenre(request, genre, title, artist, album)
            MusicRequestKind.MOOD -> scoreMood(request, genre, title, artist, album)
            MusicRequestKind.ARTIST -> scoreArtist(request, title, artist, album)
            MusicRequestKind.TITLE -> scoreTitle(request, title, artist)
            MusicRequestKind.ERA -> scoreEra(request, track)
        }
    }

    /** El género manda: si MediaStore lo conoce, decide. */
    private fun scoreGenre(
        request: MusicRequest,
        genre: String,
        title: String,
        artist: String,
        album: String,
    ): Int {
        var score = 0
        val wanted = request.terms.flatMap { term ->
            MusicRequestParser.GENRES[term].orEmpty()
        }
        if (genre.isNotBlank()) {
            val matches = wanted.any { alias ->
                genre.contains(MusicRequestParser.normalize(alias))
            }
            if (matches) score += 100
            // Un género totalmente distinto resta: no sirve de nada.
            if (!matches && wanted.isNotEmpty()) score -= 20
        }
        // Fallback: el género aparece en el título, el artista o el álbum.
        if (wanted.any { alias ->
                val clean = MusicRequestParser.normalize(alias)
                title.contains(clean) || artist.contains(clean) || album.contains(clean)
            }
        ) {
            score += 45
        }
        return score.coerceAtLeast(0)
    }

    /** El estado de ánimo se traduce a géneros y se puntúa como un género. */
    private fun scoreMood(
        request: MusicRequest,
        genre: String,
        title: String,
        artist: String,
        album: String,
    ): Int {
        val asGenre = MusicRequest(
            kind = MusicRequestKind.GENRE,
            terms = request.terms,
            label = request.label,
        )
        // Sin metadatos de género el acierto es más flojo: en ese caso la IA
        // puede elegir la pista después.
        return scoreGenre(asGenre, genre, title, artist, album)
    }

    /** Busqueda por nombre de artista o banda. */
    private fun scoreArtist(request: MusicRequest, title: String, artist: String, album: String): Int {
        var score = 0
        request.terms.forEach { term ->
            val clean = MusicRequestParser.normalize(term)
            if (clean.isBlank()) return@forEach
            if (artist.contains(clean)) score += 90
            else if (album.contains(clean)) score += 60
            else if (title.contains(clean)) score += 50
        }
        return score
    }

    /** Búsqueda por título de canción. */
    private fun scoreTitle(request: MusicRequest, title: String, artist: String): Int {
        var score = 0
        request.terms.forEach { term ->
            val clean = MusicRequestParser.normalize(term)
            if (clean.isBlank()) return@forEach
            if (title.contains(clean)) score += 90
            else if (artist.contains(clean)) score += 40
        }
        return score
    }

    /** Búsqueda por década: «algo de los 90». */
    private fun scoreEra(request: MusicRequest, track: Track): Int {
        val decade = request.terms.firstOrNull()?.trim() ?: return 0
        val years = MusicRequestParser.yearsOf(decade)
        if (track.year <= 0) return 0
        return if (track.year in years) 100 else 0
    }

    /** Lista de artistas disponibles, para preguntarle a la IA cuál encaja. */
    fun artists(tracks: List<Track>, limit: Int = 40): List<String> =
        tracks.map { it.artist.trim() }
            .filter { it.isNotBlank() && !it.equals("desconocido", ignoreCase = true) }
            .distinct()
            .sorted()
            .take(limit)
}
