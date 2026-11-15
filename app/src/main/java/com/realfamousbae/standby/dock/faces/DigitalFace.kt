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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.realfamousbae.standby.dock.standby.ClockStyle
import com.realfamousbae.standby.dock.standby.StandbyFonts
import com.realfamousbae.standby.dock.standby.rememberNow
import java.time.format.DateTimeFormatter

/**
 * Cara digital: la hora a todo el ancho con dígitos tabulares y, abajo, la fecha.
 * El cuerpo de la letra lo elige [TextAutoSize] según el ancho que haya, así el mismo
 * bloque sirve a pantalla completa y en media tarjeta.
 */
@Composable
fun DigitalFace(
    style: ClockStyle,
    maxTimeSize: TextUnit,
    dateSize: TextUnit,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    val now = rememberNow(everySecond = style.showSeconds)

    val pattern = buildString {
        append(if (style.is24h) "HH:mm" else "h:mm")
        if (style.showSeconds) append(":ss")
    }
    val time = now.format(DateTimeFormatter.ofPattern(pattern))
    val date = now.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", locale))
        .replaceFirstChar { it.titlecase(locale) }

    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
    // El cuerpo de la letra no puede pasarse de alto: si no, la fecha queda fuera de la
    // pantalla cuando la hora ocupa todo el ancho.
    val techo = minOf(style.size(maxTimeSize).value, maxHeight.value * 0.62f).sp
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        BasicText(
            text = time,
            modifier = Modifier.fillMaxWidth(),
            style = TextStyle(
                color = style.color,
                fontFamily = style.family,
                fontWeight = style.weight,
                fontFeatureSettings = StandbyFonts.TABULAR_NUMBERS,
                letterSpacing = (-0.02).em,
                textAlign = TextAlign.Center,
            ),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(
                minFontSize = 36.sp,
                maxFontSize = techo,
                stepSize = 4.sp,
            ),
        )
        BasicText(
            text = date,
            style = TextStyle(
                color = style.secondary,
                fontFamily = StandbyFonts.text,
                fontSize = dateSize,
                textAlign = TextAlign.Center,
            ),
            maxLines = 1,
        )
    }
    }
}
