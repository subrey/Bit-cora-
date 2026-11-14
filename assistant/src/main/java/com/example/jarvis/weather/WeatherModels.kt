package com.example.jarvis.weather

import java.time.LocalDate

/** Lugar resuelto a partir del nombre de la ciudad. */
data class Place(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String = "",
)

/** Condición actual en un sitio. */
data class CurrentWeather(
    val temperature: Double,
    val apparentTemperature: Double,
    val precipitationMm: Double,
    val windKmh: Double,
    val code: Int,
)

/** Previsión de un día. */
data class DailyWeather(
    val date: LocalDate,
    val code: Int,
    val maxTemp: Double,
    val minTemp: Double,
    val rainProbability: Int,
)

/** Informe completo: lo que hay ahora y los próximos días. */
data class WeatherReport(
    val place: String,
    val now: CurrentWeather,
    val days: List<DailyWeather> = emptyList(),
) {
    /** Previsión de un día concreto, si está en los datos. */
    fun day(date: LocalDate): DailyWeather? = days.firstOrNull { it.date == date }
}
