package com.realfamousbae.standby.dock.faces

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.realfamousbae.standby.dock.core.ui.SolarPalette
import com.realfamousbae.standby.dock.standby.ClockStyle
import com.realfamousbae.standby.dock.standby.StandbyFonts
import com.realfamousbae.standby.dock.standby.rememberNow
import java.time.format.DateTimeFormatter

/**
 * Cara solar: la misma hora grande de la cara digital, pero pintada con los colores del
 * momento del día (azul de madrugada, rosa al amanecer, celeste al mediodía, naranja al
 * atardecer). Los colores salen de [SolarPalette], que es lógica pura y está probada.
 *
 * Con el modo noche prendido manda el rojo: de noche no se le discute al modo noche.
 */
@Composable
fun SolarFace(
    style: ClockStyle,
    maxTimeSize: TextUnit,
    dateSize: TextUnit,
    modifier: Modifier = Modifier,
) {
    val now = rememberNow(everySecond = style.showSeconds)
    val look = SolarPalette.at(now.hour * 60 + now.minute)
    val top = if (style.night) style.color else Color(look.top)
    val bottom = if (style.night) style.color else Color(look.bottom)

    val pattern = buildString {
        append(if (style.is24h) "HH:mm" else "h:mm")
        if (style.showSeconds) append(":ss")
    }
    val time = now.format(DateTimeFormatter.ofPattern(pattern))

    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val techo = minOf(style.size(maxTimeSize).value, maxHeight.value * 0.62f).sp
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            BasicText(
                text = time,
                modifier = Modifier.fillMaxWidth(),
                style = TextStyle(
                    // El degradado se pinta sobre los dígitos: el fondo sigue negro (OLED).
                    brush = Brush.linearGradient(
                        colors = listOf(top, bottom),
                        start = Offset.Zero,
                        end = Offset(0f, Float.POSITIVE_INFINITY),
                    ),
                    fontFamily = style.family,
                    fontWeight = style.weight,
                    fontFeatureSettings = StandbyFonts.TABULAR_NUMBERS,
                    letterSpacing = (-0.02).em,
                    textAlign = TextAlign.Center,
                ),
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 36.sp, maxFontSize = techo, stepSize = 4.sp),
            )
            BasicText(
                text = look.label.uppercase(),
                style = TextStyle(
                    color = bottom.copy(alpha = 0.75f),
                    fontFamily = StandbyFonts.text,
                    fontSize = dateSize,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center,
                ),
                maxLines = 1,
            )
        }
    }
}
