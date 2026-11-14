package com.example.jarvis.weather

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WeatherTextTest {

    private val report = WeatherReport(
        place = "Madrid",
        now = CurrentWeather(
            temperature = 12.0,
            apparentTemperature = 10.0,
            precipitationMm = 0.0,
            windKmh = 11.0,
            code = 3,
        ),
        days = listOf(
            DailyWeather(
                date = LocalDate.of(2026, 11, 14),
                code = 3,
                maxTemp = 14.0,
                minTemp = 8.0,
                rainProbability = 10,
            ),
            DailyWeather(
                date = LocalDate.of(2026, 11, 15),
                code = 61,
                maxTemp = 12.0,
                minTemp = 7.0,
                rainProbability = 80,
            ),
        ),
    )

    @Test
    fun `describe los codigos de tiempo`() {
        assertEquals("despejado", WeatherText.describe(0))
        assertEquals("nublado", WeatherText.describe(3))
        assertTrue(WeatherText.describe(61).contains("lluvia"))
        assertTrue(WeatherText.describe(95).contains("tormenta"))
        assertEquals("con cielo variable", WeatherText.describe(999))
    }

    @Test
    fun `detecta el tiempo de agua`() {
        assertTrue(WeatherText.isWet(61))
        assertTrue(WeatherText.isWet(95))
        assertTrue(!WeatherText.isWet(0))
        assertTrue(!WeatherText.isWet(3))
    }

    @Test
    fun `describe el tiempo actual`() {
        val text = WeatherText.now(report)
        assertTrue(text.contains("Madrid"))
        assertTrue(text.contains("12 grados"))
        assertTrue(text.contains("nublado"))
        // La sensación térmica difiere en más de 2 grados: se menciona.
        assertTrue(text.contains("sensación de 10"))
    }

    @Test
    fun `no repite la sensacion si es casi igual`() {
        val calm = report.copy(now = report.now.copy(apparentTemperature = 12.5))
        assertTrue(!WeatherText.now(calm).contains("sensación"))
    }

    @Test
    fun `describe la prevision de un dia`() {
        val text = WeatherText.day(report, LocalDate.of(2026, 11, 15), "Mañana")!!
        assertTrue(text.startsWith("Mañana"))
        assertTrue(text.contains("lluvia"))
        assertTrue(text.contains("80 por ciento"))
    }

    @Test
    fun `sin prevision devuelve null`() {
        assertNull(WeatherText.day(report, LocalDate.of(2026, 12, 25), "Navidad"))
    }

    @Test
    fun `aconseja paraguas cuando llueve`() {
        val wet = report.copy(now = report.now.copy(code = 61))
        assertTrue(WeatherText.advice(wet).contains("paraguas"))
    }

    @Test
    fun `aconseja abrigo cuando hace frio`() {
        val cold = report.copy(now = report.now.copy(apparentTemperature = 2.0))
        assertTrue(WeatherText.advice(cold).contains("abrigo"))
    }

    @Test
    fun `avisa de la lluvia de manana`() {
        assertTrue(WeatherText.advice(report).contains("mañana va a llover"))
    }
}
