package com.example.jarvis.weather

/** Resultado de pedir el tiempo: informe o mensaje de error. */
sealed class WeatherResult {

    data class Success(val report: WeatherReport) : WeatherResult()

    data class Failure(val message: String) : WeatherResult()
}

/**
 * Fuente del tiempo. La implementación real usa Open-Meteo, que es gratuita y
 * no necesita clave.
 */
interface WeatherProvider {

    /** Informe del tiempo de la ciudad indicada. */
    suspend fun report(city: String): WeatherResult
}
