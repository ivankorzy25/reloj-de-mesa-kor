package com.realfamousbae.standby.dock.photos

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.realfamousbae.standby.dock.core.prefs.StandbySettings
import com.realfamousbae.standby.dock.diag.DiagLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.time.LocalDate
import java.net.URL

/** Solo nos interesa el id de cada foto; el resto de la respuesta de Immich se ignora. */
@Serializable
private data class ImmichAsset(val id: String)

/** La búsqueda por fecha contesta envuelta: `{"assets":{"items":[…]}}`. */
@Serializable
private data class SearchResponse(val assets: SearchBucket = SearchBucket())

@Serializable
private data class SearchBucket(val items: List<ImmichAsset> = emptyList())

/**
 * Cliente mínimo de Immich (fotos.generadores.ar): pide ids al azar y baja la vista previa
 * de cada foto. Sin librerías de red: `HttpURLConnection` y nada más.
 *
 * La clave de API nunca sale de los ajustes del teléfono; no se escribe en el journal ni
 * viaja a ningún otro lado.
 */
class ImmichClient(private val log: DiagLog) {

    private val json = Json { ignoreUnknownKeys = true }

    /** Ids de fotos al azar. Lista vacía = falta clave, no hay red o Immich dijo que no. */
    suspend fun randomIds(settings: StandbySettings, size: Int): List<String> {
        if (!settings.immichReady) return emptyList()
        val body = buildString {
            append("""{"size":$size,"type":"IMAGE","withArchived":false""")
            if (settings.immichOnlyFavorites) append(""","isFavorite":true""")
            append("}")
        }
        return request(settings, "/api/search/random", body)?.let { text ->
            runCatching { json.decodeFromString<List<ImmichAsset>>(text).map { it.id } }
                .onFailure { log.log("Fotos: respuesta de Immich que no se entiende") }
                .getOrDefault(emptyList())
        } ?: emptyList()
    }

    /**
     * "Recuerdos": las fotos del mismo día de los años anteriores, de la más vieja a la más
     * nueva. Se pregunta año por año porque Immich busca en un rango de fechas por vez.
     * Si no hay nada (ese día nunca pasó nada), devuelve una lista vacía y el carrusel sigue
     * con fotos al azar.
     */
    suspend fun memoryIds(
        settings: StandbySettings,
        today: LocalDate,
        yearsBack: Int = YEARS_BACK,
        perYear: Int = 10,
    ): List<String> {
        if (!settings.immichReady) return emptyList()
        val ids = mutableListOf<String>()
        for (back in yearsBack downTo 1) {
            val day = runCatching { today.minusYears(back.toLong()) }.getOrNull() ?: continue
            val body = buildString {
                append("""{"size":$perYear,"type":"IMAGE","withArchived":false""")
                append(""","takenAfter":"${day}T00:00:00.000Z"""")
                append(""","takenBefore":"${day.plusDays(1)}T00:00:00.000Z"""")
                if (settings.immichOnlyFavorites) append(""","isFavorite":true""")
                append("}")
            }
            val text = request(settings, "/api/search/metadata", body) ?: continue
            runCatching { json.decodeFromString<SearchResponse>(text).assets.items.map { it.id } }
                .onSuccess { ids += it }
                .onFailure { log.log("Fotos: respuesta de recuerdos que no se entiende") }
        }
        return ids
    }

    /** Vista previa (~1920 px de lado largo), lista para dibujar. */
    suspend fun preview(settings: StandbySettings, id: String): ImageBitmap? = withContext(Dispatchers.IO) {
        if (!settings.immichReady) return@withContext null
        val url = "${settings.immichUrl}/api/assets/$id/thumbnail?size=preview"
        runCatching {
            open(settings, url, method = "GET").use { connection ->
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    log.log("Fotos: Immich respondió ${connection.responseCode} a una foto")
                    return@use null
                }
                connection.inputStream.use { BitmapFactory.decodeStream(it) }?.asImageBitmap()
            }
        }.onFailure { log.log("Fotos: no se pudo bajar la foto (${it.javaClass.simpleName})") }
            .getOrNull()
    }

    /** Prueba de conexión para la pantalla de ajustes. Devuelve el mensaje que se muestra. */
    suspend fun test(settings: StandbySettings): String {
        if (!settings.immichReady) return "Falta la dirección o la clave."
        val ids = randomIds(settings, size = 1)
        return if (ids.isEmpty()) "No se pudo conectar (revisá la dirección y la clave)."
        else "Conectado: Immich respondió con fotos."
    }

    private suspend fun request(settings: StandbySettings, path: String, body: String): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                open(settings, settings.immichUrl + path, method = "POST").use { connection ->
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.outputStream.use { it.write(body.toByteArray()) }
                    if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                        log.log("Fotos: Immich respondió ${connection.responseCode}")
                        return@use null
                    }
                    connection.inputStream.bufferedReader().use { it.readText() }
                }
            }.onFailure { log.log("Fotos: sin conexión con Immich (${it.javaClass.simpleName})") }
                .getOrNull()
        }

    private fun open(settings: StandbySettings, url: String, method: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("x-api-key", settings.immichApiKey)
            setRequestProperty("Accept", "application/json")
        }

    private inline fun <T> HttpURLConnection.use(block: (HttpURLConnection) -> T): T =
        try {
            block(this)
        } finally {
            disconnect()
        }

    private companion object {
        const val TIMEOUT_MS = 15_000

        /** Hasta cuántos años para atrás se buscan recuerdos del día de hoy. */
        const val YEARS_BACK = 12
    }
}
