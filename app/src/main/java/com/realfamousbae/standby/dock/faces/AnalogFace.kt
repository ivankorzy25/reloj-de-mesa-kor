package com.realfamousbae.standby.dock.faces

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.realfamousbae.standby.dock.standby.ClockStyle
import com.realfamousbae.standby.dock.standby.rememberNow
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Cara analógica minimalista: marcas finas en las 12 horas, agujas redondeadas y un punto
 * en el centro. Sin números, sin marco.
 */
@Composable
fun AnalogFace(style: ClockStyle, modifier: Modifier = Modifier) {
    val now = rememberNow(everySecond = style.showSeconds)

    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        // Cuadrado del lado más corto: la esfera entra entera tanto a pantalla completa
        // como en media tarjeta.
        val side = minOf(maxWidth, maxHeight)
        Canvas(Modifier.size(side).padding(8.dp)) {
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            // Marcas de las horas: las de 12/3/6/9 más largas.
            repeat(12) { i ->
                val long = i % 3 == 0
                val from = radius * if (long) 0.80f else 0.87f
                val to = radius * 0.95f
                val angle = (-PI / 2 + i * PI / 6).toFloat()
                drawLine(
                    color = if (long) style.color else style.secondary,
                    start = center + Offset(cos(angle) * from, sin(angle) * from),
                    end = center + Offset(cos(angle) * to, sin(angle) * to),
                    strokeWidth = radius * if (long) 0.018f else 0.012f,
                    cap = StrokeCap.Round,
                )
            }

            fun hand(fraction: Float, length: Float, width: Float, color: androidx.compose.ui.graphics.Color) {
                val angle = (-PI / 2 + fraction * 2 * PI).toFloat()
                drawLine(
                    color = color,
                    // Un pedacito pasado del centro, como los relojes de verdad.
                    start = center - Offset(cos(angle) * radius * 0.08f, sin(angle) * radius * 0.08f),
                    end = center + Offset(cos(angle) * radius * length, sin(angle) * radius * length),
                    strokeWidth = radius * width,
                    cap = StrokeCap.Round,
                )
            }

            val seconds = now.second + now.nano / 1_000_000_000f
            val minutes = now.minute + seconds / 60f
            val hours = now.hour % 12 + minutes / 60f
            hand(hours / 12f, length = 0.52f, width = 0.035f, color = style.color)
            hand(minutes / 60f, length = 0.78f, width = 0.022f, color = style.color)
            if (style.showSeconds) {
                hand(seconds / 60f, length = 0.88f, width = 0.010f, color = style.secondary)
            }
            drawCircle(color = style.color, radius = radius * 0.028f, center = center)
        }
    }
}
