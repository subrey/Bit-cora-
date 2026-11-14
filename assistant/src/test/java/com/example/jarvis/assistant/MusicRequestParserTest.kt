package com.example.jarvis.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicRequestParserTest {

    @Test
    fun `entiende un genero`() {
        val request = MusicRequestParser.parse("algo de rock")!!
        assertEquals(MusicRequestKind.GENRE, request.kind)
        assertEquals(listOf("rock"), request.terms)
    }

    @Test
    fun `entiende un genero en ingles`() {
        assertEquals("jazz", MusicRequestParser.parse("algo de jazz")!!.terms.first())
    }

    @Test
    fun `entiende un estado de animo`() {
        val request = MusicRequestParser.parse("musica para estudiar")!!
        assertEquals(MusicRequestKind.MOOD, request.kind)
        assertTrue(request.terms.contains("clasica"))
        assertTrue(request.terms.contains("ambient"))
    }

    @Test
    fun `entende una epoca`() {
        val request = MusicRequestParser.parse("algo de los 90")!!
        assertEquals(MusicRequestKind.ERA, request.kind)
        assertEquals(listOf("90"), request.terms)
        assertEquals(1990..1999, MusicRequestParser.yearsOf("90"))
    }

    @Test
    fun `entiende un artista`() {
        val request = MusicRequestParser.parse("algo de Coldplay")!!
        assertEquals(MusicRequestKind.ARTIST, request.kind)
        assertTrue(request.terms.first().contains("coldplay"))
    }

    @Test
    fun `entiende un artista entre comillas`() {
        val request = MusicRequestParser.parse("pon la canción «Bohemian Rhapsody»")!!
        assertEquals(MusicRequestKind.ARTIST, request.kind)
        assertEquals("bohemian rhapsody", request.terms.first())
    }

    @Test
    fun `sin pistas de busqueda devuelve null`() {
        assertNull(MusicRequestParser.parse(""))
        assertNull(MusicRequestParser.parse("pon algo"))
    }

    @Test
    fun `normaliza acentos`() {
        assertEquals("musica clasica", MusicRequestParser.normalize("Música Clásica"))
    }
}
