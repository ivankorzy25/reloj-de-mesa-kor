package com.realfamousbae.standby.dock.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.realfamousbae.standby.dock.core.prefs.StandbySettings
import com.realfamousbae.standby.dock.core.weather.WeatherCodes
import com.realfamousbae.standby.dock.standby.ClockStyle
import com.realfamousbae.standby.dock.weather.WeatherRepository
import kotlinx.coroutines.delay

/** Tarjeta del clima: temperatura grande, qué está pasando, y máxima/mínima del día. */
@Composable
fun WeatherCard(settings: StandbySettings, style: ClockStyle, repository: WeatherRepository) {
    val weather by repository.weather.collectAsStateWithLifecycle()
    val failed by repository.failed.collectAsStateWithLifecycle()

    LaunchedEffect(settings.weatherLatitude, settings.weatherLongitude) {
        while (true) {
            repository.refreshIfStale(settings)
            delay(5 * 60 * 1000L)
        }
    }

    CardFrame(style, "Clima · ${settings.weatherPlace}") {
        val now = weather
        when {
            now != null -> Row(verticalAlignment = Alignment.CenterVertically) {
                // La columna se queda con el ancho que sobra; el dibujo tiene tamaño fijo.
                Column(Modifier.weight(1f)) {
                    CardBig("${now.temperature}°", style, size = 86.sp)
                    CardText(WeatherCodes.description(now.code), style, size = 20.sp, strong = true)
                    CardText("Máx ${now.max}°  ·  Mín ${now.min}°", style, size = 17.sp)
                    CardText("Se siente ${now.feelsLike}°", style, size = 17.sp)
                }
                Spacer(Modifier.width(16.dp))
                WeatherGlyph(
                    icon = WeatherCodes.icon(now.code, now.isDay),
                    color = style.color,
                    background = style.cardColor,
                    size = 96.dp,
                )
            }
            failed -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                CardText("Sin conexión con el servicio del clima.", style, strong = true)
                CardText("Se vuelve a intentar solo.", style)
            }
            else -> CardText("Buscando el clima…", style)
        }
    }
}
