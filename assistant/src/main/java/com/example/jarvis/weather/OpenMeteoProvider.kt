package com.example.jarvis.weather

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Proveedor del tiempo con **Open-Meteo**: es gratuito, no necesita clave y no
 * guarda datos personales. Solo se envía el nombre de la ciudad.
 *
 * El informe se guarda en memoria unos minutos para no repetir la petición
 * cuando el usuario pregunta varias veces seguidas.
 */
class OpenMeteoProvider(
    private val client: OkHttpClient = defaultClient(),
) : WeatherProvider {

    private var cached: WeatherReport? = null
    private var cachedCity: String = ""
    private var cachedAt: Long = 0L

    override suspend fun report(city: String): WeatherResult = withContext(Dispatchers.IO) {
        val clean = city.trim().ifBlank { DEFAULT_CITY }
        val cache = cached
        if (cache != null && cachedCity.equals(clean, ignoreCase = true) &&
            System.currentTimeMillis() - cachedAt < CACHE_MILLIS
        ) {
            return@withContext WeatherResult.Success(cache)
        }

        val place = findPlace(clean)
        if (place == null) {
            return@withContext WeatherResult.Failure(
                "No encuentro la ciudad «$clean». Puede cambiar la ciudad en Ajustes.",
            )
        }

        val forecast = fetchForecast(place)
        if (forecast == null) {
            return@withContext WeatherResult.Failure(
                "No he podido consultar el tiempo. Compruebe la conexión e inténtelo de nuevo.",
            )
        }

        val report = WeatherReport(
            place = place.name,
            now = forecast.first,
            days = forecast.second,
        )
        cached = report
        cachedCity = clean
        cachedAt = System.currentTimeMillis()
        WeatherResult.Success(report)
    }

    /** Busca la ciudad por su nombre. */
    private suspend fun findPlace(city: String): Place? {
        val url = "$GEOCODING_URL?name=${city.urlEncode()}&count=1&language=es&format=json"
        val body = get(url) ?: return null
        return WeatherJson.parsePlace(body)
    }

    /** Pide el estado actual y la previsión de los próximos días. */
    private suspend fun fetchForecast(place: Place): Pair<CurrentWeather, List<DailyWeather>>? {
        val url = "$FORECAST_URL?latitude=${place.latitude}&longitude=${place.longitude}" +
            "&current=temperature_2m,apparent_temperature,precipitation,weather_code,wind_speed_10m" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max" +
            "&timezone=auto&forecast_days=$FORECAST_DAYS"
        val body = get(url) ?: return null
        return WeatherJson.parseForecast(body)
    }

    private suspend fun get(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .get()
            .build()
        val response = try {
            client.newCall(request).await()
        } catch (e: SocketTimeoutException) {
            return null
        } catch (e: IOException) {
            return null
        }
        return response.use {
            if (!it.isSuccessful) return null
            it.body?.string()
        }
    }

    /** OkHttp bloquea el hilo; se envuelve para poder cancelar con la corrutina. */
    private suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
        enqueue(
            object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (cont.isActive) cont.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (cont.isActive) cont.resume(response) else response.close()
                }
            },
        )
        cont.invokeOnCancellation { runCatching { cancel() } }
    }

    companion object {
        /** Ciudad usada si el usuario no ha configurado ninguna. */
        const val DEFAULT_CITY = "Madrid"

        private const val GEOCODING_URL = "https://geocoding-api.open-meteo.com/v1/search"
        private const val FORECAST_URL = "https://api.open-meteo.com/v1/forecast"
        private const val FORECAST_DAYS = 7

        /** El informe se reutiliza durante 15 minutos. */
        private const val CACHE_MILLIS = 15 * 60 * 1000L

        private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(25, TimeUnit.SECONDS)
            .build()
    }
}

/** Codifica un texto para usarlo dentro de una URL. */
private fun String.urlEncode(): String = java.net.URLEncoder.encode(this, "UTF-8")
