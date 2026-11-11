package com.example.jarvis.assistant

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Utilidades puras del asistente: calculadora, conversor de unidades y formatos de fecha.
 * Sin dependencias de Android para poderlas probar en tests.
 */
object Tools {

    private val spanish = Locale.forLanguageTag("es")

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", spanish)
    private val dateFormatter = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", spanish)

    fun formatTime(dateTime: LocalDateTime): String = timeFormatter.format(dateTime)

    fun formatDate(dateTime: LocalDateTime): String = dateFormatter.format(dateTime)

    fun capitalize(text: String): String =
        text.replaceFirstChar { if (it.isLowerCase()) it.titlecase(spanish) else it.toString() }

    /** Evalúa una expresión aritmética ("2 + 3 * 4"). Devuelve null si no es válida. */
    fun evaluate(expression: String): Double? = Parser(expression).parse()

    /** Convierte entre unidades de la misma familia ("10", "km", "milla"). */
    fun convert(value: Double, from: String, to: String): Double? {
        val fromKey = canonicalUnit(from) ?: return null
        val toKey = canonicalUnit(to) ?: return null
        val fromFamily = familyOf(fromKey) ?: return null
        val toFamily = familyOf(toKey) ?: return null
        if (fromFamily != toFamily) return null

        if (fromFamily == "temperatura") {
            val base = when (fromKey) {
                "c" -> value
                "f" -> (value - 32.0) * 5.0 / 9.0
                else -> value - 273.15
            }
            return when (toKey) {
                "c" -> base
                "f" -> base * 9.0 / 5.0 + 32.0
                else -> base + 273.15
            }
        }

        val fromFactor = factors[fromKey] ?: return null
        val toFactor = factors[toKey] ?: return null
        return value * fromFactor / toFactor
    }

    private val factors = mapOf(
        // longitud (base: metro)
        "m" to 1.0, "km" to 1000.0, "cm" to 0.01, "mm" to 0.001,
        "mi" to 1609.344, "yd" to 0.9144, "ft" to 0.3048, "in" to 0.0254,
        // masa (base: kilogramo)
        "kg" to 1.0, "g" to 0.001, "mg" to 0.000001, "lb" to 0.45359237, "oz" to 0.028349523125,
        // volumen (base: litro)
        "l" to 1.0, "ml" to 0.001, "cl" to 0.01, "gal" to 3.78541, "taza" to 0.25,
        // tiempo (base: segundo)
        "s" to 1.0, "min" to 60.0, "h" to 3600.0, "dia" to 86400.0, "semana" to 604800.0,
    )

    private val families = mapOf(
        "m" to "longitud", "km" to "longitud", "cm" to "longitud", "mm" to "longitud",
        "mi" to "longitud", "yd" to "longitud", "ft" to "longitud", "in" to "longitud",
        "kg" to "masa", "g" to "masa", "mg" to "masa", "lb" to "masa", "oz" to "masa",
        "l" to "volumen", "ml" to "volumen", "cl" to "volumen", "gal" to "volumen", "taza" to "volumen",
        "s" to "tiempo", "min" to "tiempo", "h" to "tiempo", "dia" to "tiempo", "semana" to "tiempo",
        "c" to "temperatura", "f" to "temperatura", "k" to "temperatura",
    )

    private val aliases = mapOf(
        "m" to "m", "metro" to "m", "metros" to "m",
        "km" to "km", "kilometro" to "km", "kilometros" to "km",
        "cm" to "cm", "centimetro" to "cm", "centimetros" to "cm",
        "mm" to "mm", "milimetro" to "mm", "milimetros" to "mm",
        "mi" to "mi", "milla" to "mi", "millas" to "mi",
        "yd" to "yd", "yarda" to "yd", "yardas" to "yd",
        "ft" to "ft", "pie" to "ft", "pies" to "ft",
        "in" to "in", "pulgada" to "in", "pulgadas" to "in",
        "kg" to "kg", "kilo" to "kg", "kilos" to "kg", "kilogramo" to "kg", "kilogramos" to "kg",
        "g" to "g", "gramo" to "g", "gramos" to "g",
        "mg" to "mg", "miligramo" to "mg", "miligramos" to "mg",
        "lb" to "lb", "libra" to "lb", "libras" to "lb",
        "oz" to "oz", "onza" to "oz", "onzas" to "oz",
        "l" to "l", "litro" to "l", "litros" to "l",
        "ml" to "ml", "mililitro" to "ml", "mililitros" to "ml",
        "cl" to "cl", "centilitro" to "cl", "centilitros" to "cl",
        "gal" to "gal", "galon" to "gal", "galones" to "gal",
        "taza" to "taza", "tazas" to "taza",
        "s" to "s", "segundo" to "s", "segundos" to "s",
        "min" to "min", "minuto" to "min", "minutos" to "min",
        "h" to "h", "hora" to "h", "horas" to "h",
        "dia" to "dia", "dias" to "dia",
        "semana" to "semana", "semanas" to "semana",
        "c" to "c", "celsius" to "c", "centigrado" to "c", "centigrados" to "c", "grados" to "c",
        "f" to "f", "fahrenheit" to "f", "grado" to "f",
        "k" to "k", "kelvin" to "k",
    )

    fun canonicalUnit(name: String): String? {
        val cleaned = name.trim().lowercase().removePrefix("°").trim()
        return aliases[cleaned]
    }

    fun familyOf(canonical: String): String? = families[canonical]

    /** Convierte una frase como "10 km a milla" en (valor, unidad origen, unidad destino). */
    fun parseConversion(payload: String): Triple<Double, String, String>? {
        val parts = payload.split(" a ")
        if (parts.size < 2) return null
        val left = parts[0].trim().split(" ", limit = 2)
        val right = parts[1].trim()
        if (left.size < 2) return null
        val value = left[0].replace(",", ".").toDoubleOrNull() ?: return null
        return Triple(value, left[1], right)
    }

    /** Evalúa un fragmento de texto con una operación: "2 + 3". */
    private class Parser(private val source: String) {
        private val text: String
        private var index = 0

        init {
            val cleaned = source.replace("×", "*").replace("·", "*").replace("÷", "/")
            if (cleaned.any { !(it.isDigit() || it in "+-*/%()., " ) }) {
                text = ""
            } else {
                text = cleaned.replace(",", ".").replace(" ", "")
            }
        }

        fun parse(): Double? {
            if (text.isEmpty()) return null
            val value = expression() ?: return null
            return if (index == text.length) value else null
        }

        private fun peek(): Char? = if (index < text.length) text[index] else null

        private fun expression(): Double? {
            var left = term() ?: return null
            while (true) {
                val op = peek() ?: break
                if (op != '+' && op != '-') break
                index++
                val right = term() ?: return null
                left = if (op == '+') left + right else left - right
            }
            return left
        }

        private fun term(): Double? {
            var left = factor() ?: return null
            while (true) {
                val op = peek() ?: break
                if (op != '*' && op != '/' && op != '%') break
                index++
                val right = factor() ?: return null
                left = when (op) {
                    '*' -> left * right
                    '/' -> if (right == 0.0) return null else left / right
                    else -> if (right == 0.0) return null else left % right
                }
            }
            return left
        }

        private fun factor(): Double? {
            var value = unary() ?: return null
            while (peek() == '%') {
                index++
                val divisor = unary() ?: return null
                if (divisor == 0.0) return null
                value = value % divisor
            }
            return value
        }

        private fun unary(): Double? {
            val c = peek() ?: return null
            if (c == '-') {
                index++
                val value = unary() ?: return null
                return -value
            }
            if (c == '+') {
                index++
                return unary()
            }
            return primary()
        }

        private fun primary(): Double? {
            val c = peek() ?: return null
            if (c == '(') {
                index++
                val value = expression() ?: return null
                if (peek() != ')') return null
                index++
                return value
            }
            return number()
        }

        private fun number(): Double? {
            val start = index
            while (true) {
                val c = peek() ?: break
                if (c.isDigit() || c == '.') index++ else break
            }
            if (start == index) return null
            return text.substring(start, index).toDoubleOrNull()
        }
    }
}
