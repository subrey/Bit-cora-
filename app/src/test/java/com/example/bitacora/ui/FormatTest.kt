package com.example.bitacora.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {

    @Test
    fun wordCount_ignoresBlankText() {
        assertEquals(0, wordCount("   "))
        assertEquals(0, wordCount(""))
        assertEquals(3, wordCount("uno dos tres"))
    }

    @Test
    fun readingMinutes_hasMinimumOfOne() {
        assertEquals(1, readingMinutes(""))
        assertEquals(1, readingMinutes("unas pocas palabras"))
    }

    @Test
    fun excerpt_shortensLongTextWithEllipsis() {
        val text = "a".repeat(200)
        assertEquals(151, excerpt(text, 150).length)
    }

    @Test
    fun excerpt_keepsShortText() {
        assertEquals("Hola mundo", excerpt("Hola   mundo"))
    }

    @Test
    fun formatDate_todayIsRelative() {
        assertEquals("Hoy", formatDate(System.currentTimeMillis()))
    }
}
