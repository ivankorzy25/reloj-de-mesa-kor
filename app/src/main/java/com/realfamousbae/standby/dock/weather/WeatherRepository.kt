package com.realfamousbae.standby.dock.weather

import com.realfamousbae.standby.dock.core.prefs.StandbySettings
import com.realfamousbae.standby.dock.diag.DiagLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

/** El clima de ahora, ya masticado para la tarjeta. */
data class WeatherNow(
    val temperature: Int,
    val feelsLike: Int,
    val code: Int,
    val isDay: Boolean,
    val max: Int,
    val min: Int,
    val place: String,
)

@Serializable
private data class Response(val current: Current, val daily: Daily)

@Serializable
private data class Current(
    @SerialName("temperature_2m") val temperature: Float,
    @SerialName("apparent_temperature") val feelsLike: Float = 0f,
    @SerialName("weather_code") val code: Int,
    @SerialName("is_day") val isDay: Int = 1,
)

@Serializable
private data class Daily(
    @SerialName("temperature_2m_max") val max: List<Float>,
    @SerialName("temperature_2m_min") val min: List<Float>,
)

/**
 * Clima de Open-Meteo: sin clave, sin cuenta y sin pedir la ubicación del teléfono (usa el
 * lugar elegido en Ajustes, Quilmes por defecto). Se guarda el último valor para no pedirlo
 * de nuevo cada vez que se mira la tarjeta.
 */
class WeatherRepository(private val log: DiagLog) {

    private val json = Json { ignoreUnknownKeys = true }
    private val lock = Mutex()
    private var lastFetchMs = 0L
    private var lastKey = ""

    private val _weather = MutableStateFlow<WeatherNow?>(null)
    val weather: StateFlow<WeatherNow?> = _weather.asStateFlow()

    /** true = hubo un error en el último intento y no hay nada que mostrar. */
    private val _failed = MutableStateFlow(false)
    val failed: StateFlow<Boolean> = _failed.asStateFlow()

    suspend fun refreshIfStale(settings: StandbySettings, nowMs: Long = System.currentTimeMillis()) {
        val key = "${settings.weatherLatitude},${settings.weatherLongitude}"
        lock.withLock {
            val fresh = nowMs - lastFetchMs < FRESH_MS && _weather.value != null && key == lastKey
            if (fresh) return
            lastFetchMs = nowMs
            lastKey = key
        }
        val result = fetch(settings)
        if (result != null) {
            _weather.value = result
            _failed.value = false
        } else {
            _failed.value = _weather.value == null
        }
    }

    private suspend fun fetch(settings: StandbySettings): WeatherNow? = withContext(Dispatchers.IO) {
        val url = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=${settings.weatherLatitude}&longitude=${settings.weatherLongitude}" +
            "&current=temperature_2m,apparent_temperature,weather_code,is_day" +
            "&daily=temperature_2m_max,temperature_2m_min&timezone=auto&forecast_days=1"
        runCatching {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
            }
            try {
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    log.log("Clima: Open-Meteo respondió ${connection.responseCode}")
                    return@runCatching null
                }
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                val parsed = json.decodeFromString<Response>(body)
                WeatherNow(
                    temperature = Math.round(parsed.current.temperature),
                    feelsLike = Math.round(parsed.current.feelsLike),
                    code = parsed.current.code,
                    isDay = parsed.current.isDay != 0,
                    max = Math.round(parsed.daily.max.firstOrNull() ?: parsed.current.temperature),
                    min = Math.round(parsed.daily.min.firstOrNull() ?: parsed.current.temperature),
                    place = settings.weatherPlace,
                )
            } finally {
                connection.disconnect()
            }
        }.onFailure { log.log("Clima: no se pudo consultar (${it.javaClass.simpleName})") }
            .getOrNull()
    }

    private companion object {
        /** Open-Meteo actualiza cada 15 minutos: pedirlo más seguido no sirve de nada. */
        const val FRESH_MS = 15 * 60 * 1000L
        const val TIMEOUT_MS = 15_000
    }
}
