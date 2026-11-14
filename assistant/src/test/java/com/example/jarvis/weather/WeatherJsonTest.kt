package com.example.jarvis.weather

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WeatherJsonTest {

    @Test
    fun `lee la ciudad buscada`() {
        val body = """
            {"results":[{"id":3117735,"name":"Madrid","latitude":40.4165,
            "longitude":-3.70256,"country":"Spain"}]}
        """.trimIndent()
        val place = WeatherJson.parsePlace(body)
        assertNotNull(place)
        assertEquals("Madrid", place!!.name)
        assertEquals(40.4165, place.latitude, 0.001)
        assertEquals(-3.70256, place.longitude, 0.001)
    }

    @Test
    fun `sin resultados no hay ciudad`() {
        assertNull(WeatherJson.parsePlace("""{"results":[]}"""))
        assertNull(WeatherJson.parsePlace("no es json"))
    }

    @Test
    fun `lee el estado actual y los dias`() {
        val body = """
            {
              "current": {
                "time": "2026-11-14T06:45",
                "temperature_2m": 12.0,
                "apparent_temperature": 10.5,
                "precipitation": 0.4,
                "weather_code": 61,
                "wind_speed_10m": 11.3
              },
              "daily": {
                "time": ["2026-11-14", "2026-11-15"],
                "weather_code": [80, 61],
                "temperature_2m_max": [14.1, 12.4],
                "temperature_2m_min": [9.1, 8.4],
                "precipitation_probability_max": [20, 80]
              }
            }
        """.trimIndent()
        val parsed = WeatherJson.parseForecast(body)
        assertNotNull(parsed)
        val (now, days) = parsed!!
        assertEquals(12.0, now.temperature, 0.001)
        assertEquals(10.5, now.apparentTemperature, 0.001)
        assertEquals(61, now.code)
        assertEquals(11.3, now.windKmh, 0.001)
        assertEquals(2, days.size)
        assertEquals(LocalDate.of(2026, 11, 15), days[1].date)
        assertEquals(80, days[1].rainProbability)
        assertEquals(12.4, days[1].maxTemp, 0.001)
        assertTrue(days[1].minTemp < days[1].maxTemp)
    }

    @Test
    fun `sin estado actual no hay prevision`() {
        assertNull(WeatherJson.parseForecast("""{"daily":{}}"""))
        assertNull(WeatherJson.parseForecast("""{"current":{"temperature_2m":"x"}}"""))
    }
}
