package com.example.jarvis.weather

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * Lectura del JSON de Open-Meteo. Separado del cliente HTTP para poder probar
 * el formato exacto de la respuesta sin hacer red.
 */
object WeatherJson {

    /** Primer resultado de la búsqueda de ciudades. */
    fun parsePlace(body: String): Place? = runCatching {
        val results = Json.parseToJsonElement(body).jsonObject["results"]?.jsonArray
        val first = results?.firstOrNull()?.jsonObject ?: return null
        val name = first["name"]?.jsonPrimitive?.contentOrNull ?: return null
        Place(
            name = name,
            latitude = first["latitude"]?.jsonPrimitive?.doubleOrNull ?: return null,
            longitude = first["longitude"]?.jsonPrimitive?.doubleOrNull ?: return null,
            country = first["country"]?.jsonPrimitive?.contentOrNull.orEmpty(),
        )
    }.getOrNull()

    /** Previsión: estado actual y días. */
    fun parseForecast(body: String): Pair<CurrentWeather, List<DailyWeather>>? = runCatching {
        val root = Json.parseToJsonElement(body).jsonObject
        val current = root["current"]?.jsonObject ?: return null
        val now = CurrentWeather(
            temperature = current["temperature_2m"]?.jsonPrimitive?.doubleOrNull ?: return null,
            apparentTemperature = current["apparent_temperature"]?.jsonPrimitive?.doubleOrNull
                ?: current["temperature_2m"]?.jsonPrimitive?.doubleOrNull ?: return null,
            precipitationMm = current["precipitation"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
            windKmh = current["wind_speed_10m"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
            code = current["weather_code"]?.jsonPrimitive?.intOrNull ?: 0,
        )

        val daily = root["daily"]?.jsonObject
        val days = if (daily == null) {
            emptyList()
        } else {
            val dates = daily["time"]?.jsonArray.orEmpty()
            val codes = daily["weather_code"]?.jsonArray.orEmpty()
            val max = daily["temperature_2m_max"]?.jsonArray.orEmpty()
            val min = daily["temperature_2m_min"]?.jsonArray.orEmpty()
            val rain = daily["precipitation_probability_max"]?.jsonArray.orEmpty()
            dates.mapIndexedNotNull { index, element ->
                val date = parseDate(element.jsonPrimitive.contentOrNull ?: return@mapIndexedNotNull null)
                    ?: return@mapIndexedNotNull null
                DailyWeather(
                    date = date,
                    code = codes.getOrNull(index)?.jsonPrimitive?.intOrNull ?: 0,
                    maxTemp = max.getOrNull(index)?.jsonPrimitive?.doubleOrNull ?: now.temperature,
                    minTemp = min.getOrNull(index)?.jsonPrimitive?.doubleOrNull ?: now.temperature,
                    rainProbability = rain.getOrNull(index)?.jsonPrimitive?.intOrNull ?: 0,
                )
            }
        }
        now to days
    }.getOrNull()

    private fun parseDate(text: String): LocalDate? = try {
        LocalDate.parse(text)
    } catch (_: DateTimeParseException) {
        null
    }
}
