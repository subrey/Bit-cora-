package com.example.jarvis.weather

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Traduce el informe del tiempo a un texto que JARVIS pueda hablar.
 * Función pura: se puede probar en tests unitarios.
 */
object WeatherText {

    private val spanish = Locale.forLanguageTag("es")
    private val dayName = DateTimeFormatter.ofPattern("EEEE", spanish)

    /** Descripción en español de los códigos WMO de Open-Meteo. */
    fun describe(code: Int): String = when (code) {
        0 -> "despejado"
        1 -> "mayormente despejado"
        2 -> "parcialmente nublado"
        3 -> "nublado"
        45, 48 -> "con niebla"
        51 -> "con llovizna ligera"
        53 -> "con llovizna"
        55 -> "con llovizna intensa"
        56, 57 -> "con llovizna helada"
        61 -> "con lluvia ligera"
        63 -> "con lluvia"
        65 -> "con lluvia intensa"
        66, 67 -> "con lluvia helada"
        71 -> "con nieve ligera"
        73 -> "con nieve"
        75 -> "con nieve intensa"
        77 -> "con granules de nieve"
        80 -> "con chubascos ligeros"
        81 -> "con chubascos"
        82 -> "con chubascos violentos"
        85, 86 -> "con chubascos de nieve"
        95 -> "con tormenta"
        96, 99 -> "con tormenta y granizo"
        else -> "con cielo variable"
    }

    /** true si el código indica lluvia, nieve o tormenta. */
    fun isWet(code: Int): Boolean = code in 51..99

    /** Descripción del tiempo actual. */
    fun now(report: WeatherReport): String {
        val t = report.now
        val text = StringBuilder("En ${report.place} hay ${round(t.temperature)} grados, ")
        text.append(describe(t.code))
        val feelsDifferent = kotlin.math.abs(t.apparentTemperature - t.temperature) >= 2.0
        if (feelsDifferent) {
            text.append(", con sensación de ${round(t.apparentTemperature)}")
        }
        text.append(".")
        if (t.precipitationMm > 0.1) {
            text.append(" Ha caído ${round(t.precipitationMm)} milímetros de lluvia.")
        }
        if (t.windKmh >= 20) {
            text.append(" Hace bastante viento: ${round(t.windKmh)} kilómetros por hora.")
        }
        return text.toString()
    }

    /** Previsión de un día concreto. */
    fun day(report: WeatherReport, date: LocalDate, label: String): String? {
        val day = report.day(date) ?: return null
        val rain = if (day.rainProbability >= 20) {
            " con un ${day.rainProbability} por ciento de probabilidad de lluvia"
        } else {
            ""
        }
        return "$label: ${describe(day.code)}, máximo de ${round(day.maxTemp)} y mínimo de " +
            "${round(day.minTemp)} grados$rain."
    }

    /** Consejo breve según el tiempo que hace. */
    fun advice(report: WeatherReport): String {
        val today = report.now
        val tomorrow = report.days.getOrNull(1)
        return when {
            isWet(today.code) -> "Lleva paraguas, señor: está cayendo agua."
            today.apparentTemperature <= 5 -> "Hace bastante frío: conviene un abrigo."
            today.apparentTemperature >= 30 -> "Mucho calor: procure no salir en las horas de más sol."
            tomorrow != null && isWet(tomorrow.code) && tomorrow.rainProbability >= 50 ->
                "Hoy está seco, pero mañana va a llover con probabilidad alta."
            else -> "El día se ve tranquilo, señor."
        }
    }

    private fun round(value: Double): Int = kotlin.math.round(value).toInt()
}
