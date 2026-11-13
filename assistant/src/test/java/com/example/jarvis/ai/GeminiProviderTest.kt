package com.example.jarvis.ai

import org.junit.Assert.assertEquals
import org.junit.Test

class GeminiProviderTest {

    @Test
    fun `un modelo retirado se migra al actual`() {
        assertEquals(
            GeminiProvider.DEFAULT_MODEL,
            GeminiProvider.effectiveModel("gemini-2.5-flash"),
        )
    }

    @Test
    fun `un modelo vacio usa el actual`() {
        assertEquals(GeminiProvider.DEFAULT_MODEL, GeminiProvider.effectiveModel("  "))
    }

    @Test
    fun `un modelo valido se respeta`() {
        assertEquals("gemini-3.8-flash", GeminiProvider.effectiveModel("gemini-3.8-flash"))
    }
}
