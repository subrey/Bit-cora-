package com.example.jarvis.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IntentParserTest {

    @Test
    fun `detecta poner musica`() {
        val intent = IntentParser.parse("Pon música por favor")
        assertEquals(IntentKind.MUSIC_PLAY, intent.kind)
    }

    @Test
    fun `detecta pausa`() {
        assertEquals(IntentKind.MUSIC_PAUSE, IntentParser.parse("pausa la música").kind)
    }

    @Test
    fun `detecta siguiente cancion`() {
        assertEquals(IntentKind.MUSIC_NEXT, IntentParser.parse("siguiente canción").kind)
    }

    @Test
    fun `detecta cancion anterior`() {
        assertEquals(IntentKind.MUSIC_PREVIOUS, IntentParser.parse("la canción anterior").kind)
    }

    @Test
    fun `detecta volumen`() {
        assertEquals(IntentKind.VOLUME_UP, IntentParser.parse("sube el volumen").kind)
        assertEquals(IntentKind.VOLUME_DOWN, IntentParser.parse("baja volumen").kind)
    }

    @Test
    fun `detecta la hora`() {
        assertEquals(IntentKind.TIME, IntentParser.parse("¿qué hora es?").kind)
    }

    @Test
    fun `detecta la fecha`() {
        assertEquals(IntentKind.DATE, IntentParser.parse("qué día es hoy").kind)
    }

    @Test
    fun `extrae la expresion de calculo`() {
        val intent = IntentParser.parse("calcula 12 * (3 + 4)")
        assertEquals(IntentKind.CALCULATE, intent.kind)
        assertEquals("12 * (3 + 4)", intent.payload)
    }

    @Test
    fun `extrae la conversion`() {
        val intent = IntentParser.parse("convierte 10 km a millas")
        assertEquals(IntentKind.CONVERT, intent.kind)
        assertEquals("10 km a millas", intent.payload)
    }

    @Test
    fun `detecta recordatorio por voz`() {
        val intent = IntentParser.parse("recuérdame comprar el pan a las 5")
        assertEquals(IntentKind.REMINDER, intent.kind)
        assertTrue(intent.payload.contains("comprar el pan"))
    }

    @Test
    fun `detecta listar recordatorios`() {
        assertEquals(IntentKind.LIST_REMINDERS, IntentParser.parse("qué recordatorios tengo").kind)
        assertEquals(IntentKind.LIST_REMINDERS, IntentParser.parse("lista mis recordatorios").kind)
    }

    @Test
    fun `detecta cancelar recordatorio`() {
        val intent = IntentParser.parse("cancela el recordatorio de comprar pan")
        assertEquals(IntentKind.CANCEL_REMINDER, intent.kind)
        assertEquals("de comprar pan", intent.payload)
    }

    @Test
    fun `detecta temporizador`() {
        assertEquals(IntentKind.TIMER, IntentParser.parse("pon un temporizador de 10 minutos").kind)
    }

    @Test
    fun `detecta tarea`() {
        val intent = IntentParser.parse("añade tarea comprar leche")
        assertEquals(IntentKind.TODO, intent.kind)
        assertEquals("comprar leche", intent.payload)
    }

    @Test
    fun `detecta memoria`() {
        val intent = IntentParser.parse("memoriza el número del hotel")
        assertEquals(IntentKind.MEMORY, intent.kind)
        assertTrue(intent.payload.contains("hotel"))
    }

    @Test
    fun `detecta conversacion libre`() {
        assertEquals(IntentKind.CHAT, IntentParser.parse("¿cómo estás?").kind)
        assertEquals(IntentKind.CHAT, IntentParser.parse("hablemos un rato").kind)
    }

    @Test
    fun `las preguntas no se confunden con ordenes`() {
        assertEquals(IntentKind.KNOWLEDGE, IntentParser.parse("¿Qué es la música?").kind)
        assertEquals(IntentKind.CHAT, IntentParser.parse("¿Qué opinas de la música?").kind)
        assertEquals(IntentKind.CHAT, IntentParser.parse("Me gusta la música").kind)
        assertEquals(IntentKind.CHAT, IntentParser.parse("La música es genial").kind)
        assertEquals(IntentKind.STORAGE, IntentParser.parse("¿Cuánto espacio libre tengo?").kind)
        assertEquals(IntentKind.KNOWLEDGE, IntentParser.parse("¿Tienes una calculadora?").kind)
        assertEquals(IntentKind.KNOWLEDGE, IntentParser.parse("¿Por qué los chistes son graciosos?").kind)
    }

    @Test
    fun `las ordenes en forma de pregunta siguen funcionando`() {
        assertEquals(IntentKind.MUSIC_PLAY, IntentParser.parse("¿Puedes poner música?").kind)
        assertEquals(IntentKind.MUSIC_PLAY, IntentParser.parse("¿Me pones música?").kind)
        assertEquals(IntentKind.MUSIC_PLAY, IntentParser.parse("Hola, pon música").kind)
        assertEquals(IntentKind.VOLUME_UP, IntentParser.parse("¿Puedes subir el volumen?").kind)
        assertEquals(IntentKind.VOLUME_DOWN, IntentParser.parse("¿Me bajas el volumen?").kind)
        assertEquals(IntentKind.ALARM_CREATE, IntentParser.parse("¿Puedes despertarme a las 7?").kind)
        assertEquals(IntentKind.REMINDER, IntentParser.parse("¿Me recuerdas comprar pan?").kind)
        assertEquals("comprar pan", IntentParser.parse("¿Me recuerdas comprar pan?").payload)
        assertEquals(IntentKind.OPEN_APP, IntentParser.parse("¿Me abres WhatsApp?").kind)
        assertEquals("whatsapp", IntentParser.parse("¿Me abres WhatsApp?").payload)
        assertEquals(IntentKind.CHAT, IntentParser.parse("Hola, ¿cómo estás?").kind)
    }

    @Test
    fun `limpia la puntuacion de las ordenes`() {
        val calc = IntentParser.parse("¿Puedes calcular 2+2?")
        assertEquals(IntentKind.CALCULATE, calc.kind)
        assertEquals("2+2", calc.payload)

        val convert = IntentParser.parse("¿Puedes convertir 2 km a millas?")
        assertEquals(IntentKind.CONVERT, convert.kind)
        assertEquals("2 km a millas", convert.payload)

        assertEquals("rock", IntentParser.parse("¿Puedes poner algo de rock?").payload)
        assertEquals(IntentKind.CHAT, IntentParser.parse("¿Puedes añadir una tarea?").kind)
        assertEquals(IntentKind.CHAT, IntentParser.parse("¿Puedes memorizar esto?").kind)
    }

    @Test
    fun `detecta la busqueda de musica`() {
        val genre = IntentParser.parse("pon algo de rock")
        assertEquals(IntentKind.MUSIC_SEARCH, genre.kind)
        assertEquals("rock", genre.payload)

        val mood = IntentParser.parse("pon música para estudiar")
        assertEquals(IntentKind.MUSIC_SEARCH, mood.kind)
        assertEquals("estudiar", mood.payload)
    }

    @Test
    fun `detecta crear una rutina`() {
        val intent = IntentParser.parse("cuando suene el temporizador de los huevos, apaga la música")
        assertEquals(IntentKind.ROUTINE_CREATE, intent.kind)
    }

    @Test
    fun `detecta listar y borrar rutinas`() {
        assertEquals(IntentKind.ROUTINE_LIST, IntentParser.parse("qué rutinas tengo").kind)
        assertEquals(IntentKind.ROUTINE_DELETE, IntentParser.parse("borra la rutina de los huevos").kind)
    }

    @Test
    fun `detecta la agenda del dia`() {
        assertEquals(IntentKind.AGENDA_TODAY, IntentParser.parse("qué tengo hoy").kind)
        assertEquals(IntentKind.AGENDA_TODAY, IntentParser.parse("mi agenda de hoy").kind)
    }

    @Test
    fun `detecta el tiempo`() {
        assertEquals(IntentKind.WEATHER, IntentParser.parse("¿qué tiempo hace?").kind)
        assertEquals(IntentKind.WEATHER, IntentParser.parse("cuál es la temperatura exterior").kind)
    }

    @Test
    fun `detecta el tiempo de manana`() {
        assertEquals(IntentKind.WEATHER_TOMORROW, IntentParser.parse("qué tiempo hace mañana").kind)
    }

    @Test
    fun `detecta el consejo del tiempo`() {
        assertEquals(IntentKind.WEATHER_ADVICE, IntentParser.parse("¿debo llevar paraguas?").kind)
        assertEquals(IntentKind.WEATHER_ADVICE, IntentParser.parse("hace frío").kind)
    }

    @Test
    fun `detecta ayuda`() {
        assertEquals(IntentKind.HELP, IntentParser.parse("¿qué sabes hacer?").kind)
    }

    @Test
    fun `detecta quitar la musica`() {
        assertEquals(IntentKind.MUSIC_STOP, IntentParser.parse("quita la música").kind)
        assertEquals(IntentKind.MUSIC_STOP, IntentParser.parse("apaga el sonido").kind)
        assertEquals(IntentKind.MUSIC_STOP, IntentParser.parse("no quiero música").kind)
        assertEquals(IntentKind.MUSIC_STOP, IntentParser.parse("silencia la música").kind)
    }

    @Test
    fun `detecta el historial de la conversacion`() {
        assertEquals(IntentKind.HISTORY, IntentParser.parse("¿qué dijiste antes?").kind)
    }

    @Test
    fun `detecta exportar el historial`() {
        assertEquals(IntentKind.EXPORT_HISTORY, IntentParser.parse("exporta el historial").kind)
        assertEquals(IntentKind.EXPORT_HISTORY, IntentParser.parse("guarda el historial").kind)
    }

    @Test
    fun `la pausa tiene prioridad sobre el silencio`() {
        assertEquals(IntentKind.MUSIC_PAUSE, IntentParser.parse("para la música").kind)
    }

    @Test
    fun `normaliza acentos y mayusculas`() {
        assertEquals("pon musica", IntentParser.normalize("  ¡Pon MÚSICA!  "))
    }

    @Test
    fun `detecta busqueda en internet y extrae el tema`() {
        val intent = IntentParser.parse("Busca en internet la fotosíntesis")
        assertEquals(IntentKind.WEB_SEARCH, intent.kind)
        assertEquals("fotosintesis", intent.payload)
    }

    @Test
    fun `detecta investigar sobre un tema`() {
        val intent = IntentParser.parse("Investiga sobre la Revolución Mexicana")
        assertEquals(IntentKind.WEB_SEARCH, intent.kind)
        assertEquals("revolucion mexicana", intent.payload)
    }

    @Test
    fun `detecta resumen de lo mas importante`() {
        val intent = IntentParser.parse("Resúmenme lo más importante de la inteligencia artificial")
        assertEquals(IntentKind.WEB_SEARCH, intent.kind)
        assertEquals("inteligencia artificial", intent.payload)
    }

    @Test
    fun `buscar en la web tambien activa la busqueda`() {
        assertEquals(IntentKind.WEB_SEARCH, IntentParser.parse("búscame en la web quién es Einstein").kind)
    }

    @Test
    fun `buscar musica no activa la busqueda en internet`() {
        assertEquals(IntentKind.MUSIC_PLAY, IntentParser.parse("busca la música").kind)
    }

    @Test
    fun `detecta mandar un whatsapp y extrae destinatario y mensaje`() {
        val intent = IntentParser.parse("Manda un WhatsApp a Juan diciendo llego tarde")
        assertEquals(IntentKind.WHATSAPP_SEND, intent.kind)
        val (recipient, message) = IntentParser.whatsappParts(intent.payload)
        assertEquals("juan", recipient)
        assertEquals("llego tarde", message)
    }

    @Test
    fun `detecta whatsapp con numero de telefono`() {
        val intent = IntentParser.parse("envía un whatsapp al 612345678 con hola")
        assertEquals(IntentKind.WHATSAPP_SEND, intent.kind)
        val (recipient, message) = IntentParser.whatsappParts(intent.payload)
        assertEquals("612345678", recipient)
        assertEquals("hola", message)
    }

    @Test
    fun `detecta el diagnostico de red`() {
        assertEquals(IntentKind.NETWORK_TEST, IntentParser.parse("diagnóstico de red").kind)
        assertEquals(IntentKind.NETWORK_TEST, IntentParser.parse("prueba de conexión").kind)
    }
}
