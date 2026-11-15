package com.realfamousbae.standby.dock.faces

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.realfamousbae.standby.dock.core.prefs.StandbySettings
import com.realfamousbae.standby.dock.core.prefs.WorldCity
import com.realfamousbae.standby.dock.standby.ClockStyle
import com.realfamousbae.standby.dock.standby.StandbyFonts
import com.realfamousbae.standby.dock.standby.dpAsSp
import com.realfamousbae.standby.dock.standby.rememberNow
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Cara de reloj mundial: la hora de acá bien grande y, abajo, la de las dos o tres ciudades
 * elegidas en Ajustes, con el aviso de si allá ya es mañana o todavía es ayer.
 */
@Composable
fun WorldFace(
    settings: StandbySettings,
    style: ClockStyle,
    maxTimeSize: TextUnit,
    cityTimeSize: TextUnit,
    modifier: Modifier = Modifier,
) {
    val now = rememberNow(everySecond = style.showSeconds)
    val pattern = if (style.is24h) "HH:mm" else "h:mm"
    val formatter = DateTimeFormatter.ofPattern(pattern)

    BoxWithConstraints(modifier.padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
    // La hora de acá no puede comerse el alto: abajo tienen que entrar las ciudades.
    val techo = minOf(style.size(maxTimeSize).value, dpAsSp(maxHeight.value * 0.40f).value).sp
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BasicText(
            text = now.format(formatter),
            modifier = Modifier.fillMaxWidth(),
            style = TextStyle(
                color = style.color,
                fontFamily = style.family,
                fontWeight = style.weight,
                fontFeatureSettings = StandbyFonts.TABULAR_NUMBERS,
                textAlign = TextAlign.Center,
            ),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(
                minFontSize = 32.sp,
                maxFontSize = techo,
                stepSize = 4.sp,
            ),
        )
        BasicText(
            text = "Acá",
            style = TextStyle(
                color = style.secondary,
                fontFamily = StandbyFonts.text,
                fontSize = 15.sp,
                letterSpacing = 1.5.sp,
            ),
            maxLines = 1,
        )
        Row(
            Modifier.fillMaxWidth().padding(top = 18.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            settings.worldCities.forEach { city ->
                CityColumn(city, formatter, cityTimeSize, style)
            }
        }
    }
    }
}

@Composable
private fun CityColumn(
    city: WorldCity,
    formatter: DateTimeFormatter,
    timeSize: TextUnit,
    style: ClockStyle,
) {
    // Un huso que el teléfono no conoce no puede tirar abajo la pantalla: se usa el de acá.
    val zone = runCatching { ZoneId.of(city.zoneId) }.getOrDefault(ZoneId.systemDefault())
    val there = rememberNow(everySecond = false).atZone(ZoneId.systemDefault()).withZoneSameInstant(zone)
    val dayHint = when (there.toLocalDate()) {
        LocalDate.now(ZoneId.systemDefault()) -> null
        LocalDate.now(ZoneId.systemDefault()).plusDays(1) -> "mañana"
        else -> "ayer"
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        BasicText(
            text = there.format(formatter),
            style = TextStyle(
                color = style.color,
                fontFamily = style.family,
                fontWeight = style.weight,
                fontFeatureSettings = StandbyFonts.TABULAR_NUMBERS,
                fontSize = style.size(timeSize),
            ),
            maxLines = 1,
        )
        BasicText(
            text = city.name + if (dayHint != null) " · $dayHint" else "",
            style = TextStyle(
                color = style.secondary,
                fontFamily = StandbyFonts.text,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
