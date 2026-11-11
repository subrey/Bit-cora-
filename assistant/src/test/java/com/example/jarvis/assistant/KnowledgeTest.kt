package com.example.jarvis.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class KnowledgeTest {

    @Test
    fun `encuentra la revolucion mexicana`() {
        val entry = Knowledge.search("la revolución mexicana")
        assertNotNull(entry)
        assertEquals("Revolución Mexicana", entry!!.title)
    }

    @Test
    fun `encuentra einstein por palabras sueltas`() {
        val entry = Knowledge.search("quien fue einstein")
        assertNotNull(entry)
        assertEquals("Albert Einstein", entry!!.title)
    }

    @Test
    fun `encuentra el alunizaje`() {
        val entry = Knowledge.search("el alunizaje de 1969")
        assertNotNull(entry)
        assertEquals("El alunizaje", entry!!.title)
    }

    @Test
    fun `no encuentra temas desconocidos`() {
        assertNull(Knowledge.search("tecnologia de como hacer un pastel de chocolate"))
    }

    @Test
    fun `la pregunta sobre la revolucion se entiende`() {
        val intent = IntentParser.parse("dime qué sabes de la Revolución Mexicana")
        assertEquals(IntentKind.KNOWLEDGE, intent.kind)
        assertEquals("la revolucion mexicana", intent.payload)
    }

    @Test
    fun `la pregunta generica se entiende`() {
        val intent = IntentParser.parse("¿qué es el sistema solar?")
        assertEquals(IntentKind.KNOWLEDGE, intent.kind)
        assertEquals("el sistema solar", intent.payload)
    }

    @Test
    fun `las preguntas de hora y fecha siguen siendo eso`() {
        assertEquals(IntentKind.TIME, IntentParser.parse("qué hora es").kind)
        assertEquals(IntentKind.DATE, IntentParser.parse("qué día es hoy").kind)
    }
}
