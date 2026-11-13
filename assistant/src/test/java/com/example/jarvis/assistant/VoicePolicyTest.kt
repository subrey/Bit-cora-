package com.example.jarvis.assistant

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoicePolicyTest {

    @Test
    fun `sigue escuchando tras una orden hablada con conversacion continua`() {
        assertTrue(
            VoicePolicy.shouldAutoListen(
                continuous = true,
                fromVoice = true,
                appVisible = true,
                interrupted = false,
            ),
        )
    }

    @Test
    fun `no escucha si el usuario escribio la orden`() {
        assertFalse(
            VoicePolicy.shouldAutoListen(
                continuous = true,
                fromVoice = false,
                appVisible = true,
                interrupted = false,
            ),
        )
    }

    @Test
    fun `no escucha si la conversacion continua esta apagada`() {
        assertFalse(
            VoicePolicy.shouldAutoListen(
                continuous = false,
                fromVoice = true,
                appVisible = true,
                interrupted = false,
            ),
        )
    }

    @Test
    fun `no escucha con la app en segundo plano`() {
        assertFalse(
            VoicePolicy.shouldAutoListen(
                continuous = true,
                fromVoice = true,
                appVisible = false,
                interrupted = false,
            ),
        )
    }

    @Test
    fun `no reabre el micro si el usuario interrumpio`() {
        assertFalse(
            VoicePolicy.shouldAutoListen(
                continuous = true,
                fromVoice = true,
                appVisible = true,
                interrupted = true,
            ),
        )
    }

    @Test
    fun `clasifica errores transitorios y de silencio`() {
        assertTrue(VoicePolicy.isTransientError(5)) // ERROR_CLIENT
        assertTrue(VoicePolicy.isTransientError(8)) // ERROR_RECOGNIZER_BUSY
        assertFalse(VoicePolicy.isTransientError(7)) // ERROR_NO_MATCH

        assertTrue(VoicePolicy.isSilenceError(6)) // ERROR_SPEECH_TIMEOUT
        assertTrue(VoicePolicy.isSilenceError(7)) // ERROR_NO_MATCH
        assertFalse(VoicePolicy.isSilenceError(8)) // ERROR_RECOGNIZER_BUSY
    }
}
