package com.realfamousbae.standby.dock.faces

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.realfamousbae.standby.dock.standby.ClockStyle
import com.realfamousbae.standby.dock.standby.StandbyFonts
import com.realfamousbae.standby.dock.standby.dpAsSp
import com.realfamousbae.standby.dock.standby.rememberNow

/**
 * Cara flotante: la hora arriba y los minutos abajo, con números redondos bien gordos del
 * color elegido. Siempre usa la tipografía redonda, que es lo que le da el aire: acá la
 * forma de la letra es la cara, no una preferencia.
 */
@Composable
fun FloatingFace(style: ClockStyle, modifier: Modifier = Modifier) {
    val now = rememberNow(everySecond = false)
    val hour = if (style.is24h) now.hour else (now.hour % 12).let { if (it == 0) 12 else it }

    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        // Con la caja de cada línea recortada al cuerpo de la letra (ver abajo), dos líneas
        // miden casi exactamente dos veces el cuerpo: por eso el tope es la mitad del alto.
        val size = minOf(maxHeight.value * 0.44f, maxWidth.value * 0.45f) * style.sizeScale
        Column(
            Modifier.fillMaxWidth(),
            // Las dos líneas casi se tocan: así los números quedan apretados, como en iOS.
            verticalArrangement = Arrangement.spacedBy((-size * 0.04f).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Numero("%02d".format(hour), dpAsSp(size), style)
            Numero("%02d".format(now.minute), dpAsSp(size), style, dim = true)
        }
    }
}

@Composable
private fun Numero(text: String, size: TextUnit, style: ClockStyle, dim: Boolean = false) {
    BasicText(
        text,
        modifier = Modifier.fillMaxWidth(),
        style = TextStyle(
            color = if (dim) style.color.copy(alpha = 0.6f) else style.color,
            fontFamily = StandbyFonts.redonda,
            fontWeight = FontWeight.Bold,
            fontFeatureSettings = StandbyFonts.TABULAR_NUMBERS,
            fontSize = size,
            // Sin el relleno de la fuente y con la línea del tamaño del cuerpo, la caja del
            // texto mide lo que miden los números: así se puede calcular cuánto entra.
            lineHeight = 1.0.em,
            platformStyle = PlatformTextStyle(includeFontPadding = false),
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.Both,
            ),
            letterSpacing = (-0.04).em,
            textAlign = TextAlign.Center,
        ),
        maxLines = 1,
    )
}
