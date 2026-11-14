package com.example.jarvis.assistant

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicSearchTest {

    private fun track(
        id: Long,
        title: String,
        artist: String,
        genre: String = "",
        album: String = "",
        year: Int = 0,
    ) = Track(
        id = id,
        title = title,
        artist = artist,
        durationMs = 200_000,
        uri = Uri.EMPTY,
        album = album,
        genre = genre,
        year = year,
    )

    private val library = listOf(
        track(1, "Californication", "Red Hot Chili Peppers", "Rock", "By the Way", 1999),
        track(2, "Hey Jude", "The Beatles", "Rock", "Past Masters", 1968),
        track(3, "Clair de Lune", "Claude Debussy", "Classical", "Suite bergamasque", 1905),
        track(4, "Billie Jean", "Michael Jackson", "Pop", "Thriller", 1982),
        track(5, "Despacito", "Luis Fonsi", "Latin Pop", "VIDA", 2017),
    )

    @Test
    fun `encuentra por genero`() {
        val request = MusicRequestParser.parse("algo de rock")!!
        val hits = MusicSearch.search(library, request)
        assertTrue(hits.size >= 2)
        assertTrue(hits.first().track.title == "Californication" || hits.first().track.title == "Hey Jude")
        assertTrue(hits.all { it.track.genre == "Rock" })
    }

    @Test
    fun `encuentra por artista`() {
        val request = MusicRequestParser.parse("algo de Beatles")!!
        val hits = MusicSearch.search(library, request)
        assertEquals(1, hits.size)
        assertEquals("Hey Jude", hits.first().track.title)
    }

    @Test
    fun `encuentra por titulo`() {
        val request = MusicRequestParser.parse("la canción Billie Jean")!!
        val hits = MusicSearch.search(library, request)
        assertEquals("Billie Jean", hits.first().track.title)
    }

    @Test
    fun `encuentra por epoca`() {
        val request = MusicRequestParser.parse("algo de los 90")!!
        val hits = MusicSearch.search(library, request)
        assertEquals(1, hits.size)
        assertEquals("Californication", hits.first().track.title)
    }

    @Test
    fun `un estado de animo usa los generos relacionados`() {
        val request = MusicRequestParser.parse("música para estudiar")!!
        val hits = MusicSearch.search(library, request)
        // La pista clásica es la que mejor encaja.
        assertEquals("Clair de Lune", hits.first().track.title)
    }

    @Test
    fun `sin coincidencia no devuelve nada`() {
        val request = MusicRequestParser.parse("algo de polka")!!
        assertTrue(MusicSearch.search(library, request).isEmpty())
    }

    @Test
    fun `lista los artistas disponibles`() {
        val artists = MusicSearch.artists(library)
        assertEquals(5, artists.size)
        assertTrue(artists.contains("The Beatles"))
    }
}
