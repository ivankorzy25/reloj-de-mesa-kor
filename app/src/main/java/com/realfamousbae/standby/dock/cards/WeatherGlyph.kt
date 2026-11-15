package com.realfamousbae.standby.dock.cards

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import com.realfamousbae.standby.dock.core.weather.WeatherIcon

/**
 * El dibujito del clima, hecho a mano con formas: no hay que meter un pack de íconos ni
 * depender de los emojis de la fuente (que en el reloj se ven como cuadraditos).
 *
 * @param background color de la tarjeta: con él se "muerde" la luna para que quede creciente.
 */
@Composable
fun WeatherGlyph(icon: WeatherIcon, color: Color, background: Color, size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        when (icon) {
            WeatherIcon.Sol -> sun(color, center = Offset(s / 2, s / 2), radius = s * 0.20f, rays = true)
            WeatherIcon.Luna -> moon(color, background, s)
            WeatherIcon.SolNubes -> {
                sun(color, center = Offset(s * 0.34f, s * 0.32f), radius = s * 0.15f, rays = true)
                cloud(color, s, yShift = s * 0.10f, scale = 0.86f)
            }
            WeatherIcon.LunaNubes -> {
                moon(color, background, s * 0.62f)
                cloud(color, s, yShift = s * 0.10f, scale = 0.86f)
            }
            WeatherIcon.Nubes -> cloud(color, s)
            WeatherIcon.Lluvia -> {
                cloud(color, s, yShift = -s * 0.08f)
                drops(color, s, slanted = false)
            }
            WeatherIcon.Chaparron -> {
                cloud(color, s, yShift = -s * 0.08f)
                drops(color, s, slanted = true)
            }
            WeatherIcon.Nieve -> {
                cloud(color, s, yShift = -s * 0.08f)
                repeat(3) { i ->
                    drawCircle(color, radius = s * 0.035f, center = Offset(s * (0.30f + i * 0.20f), s * 0.80f))
                }
            }
            WeatherIcon.Tormenta -> {
                cloud(color, s, yShift = -s * 0.10f)
                bolt(color, s)
            }
            WeatherIcon.Niebla -> repeat(3) { i ->
                drawLine(
                    color = color,
                    start = Offset(s * 0.18f, s * (0.38f + i * 0.16f)),
                    end = Offset(s * 0.82f, s * (0.38f + i * 0.16f)),
                    strokeWidth = s * 0.055f,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

private fun DrawScope.sun(color: Color, center: Offset, radius: Float, rays: Boolean) {
    drawCircle(color, radius = radius, center = center)
    if (!rays) return
    repeat(8) { i ->
        val angle = (i * Math.PI / 4).toFloat()
        val from = radius * 1.45f
        val to = radius * 1.95f
        drawLine(
            color = color,
            start = center + Offset(kotlin.math.cos(angle) * from, kotlin.math.sin(angle) * from),
            end = center + Offset(kotlin.math.cos(angle) * to, kotlin.math.sin(angle) * to),
            strokeWidth = radius * 0.22f,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.moon(color: Color, background: Color, s: Float) {
    val center = Offset(s * 0.52f, s * 0.46f)
    val radius = s * 0.26f
    drawCircle(color, radius = radius, center = center)
    // El mordisco del color de la tarjeta deja la luna en cuarto creciente.
    drawCircle(background, radius = radius * 0.92f, center = center + Offset(radius * 0.62f, -radius * 0.42f))
}

private fun DrawScope.cloud(color: Color, s: Float, yShift: Float = 0f, scale: Float = 1f) {
    val y = s * 0.60f + yShift
    val w = s * 0.62f * scale
    drawCircle(color, radius = w * 0.34f, center = Offset(s * 0.38f, y - w * 0.12f))
    drawCircle(color, radius = w * 0.26f, center = Offset(s * 0.62f, y - w * 0.18f))
    drawRoundedBase(color, Offset(s * 0.5f - w / 2, y), Size(w, w * 0.30f))
}

private fun DrawScope.drawRoundedBase(color: Color, topLeft: Offset, size: Size) {
    drawRoundRect(
        color = color,
        topLeft = topLeft,
        size = size,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2, size.height / 2),
    )
}

private fun DrawScope.drops(color: Color, s: Float, slanted: Boolean) {
    repeat(3) { i ->
        val x = s * (0.32f + i * 0.18f)
        drawLine(
            color = color,
            start = Offset(x, s * 0.74f),
            end = Offset(if (slanted) x - s * 0.06f else x, s * 0.90f),
            strokeWidth = s * 0.045f,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.bolt(color: Color, s: Float) {
    val path = Path().apply {
        moveTo(s * 0.52f, s * 0.70f)
        lineTo(s * 0.40f, s * 0.90f)
        lineTo(s * 0.50f, s * 0.90f)
        lineTo(s * 0.44f, s * 1.02f)
        lineTo(s * 0.62f, s * 0.84f)
        lineTo(s * 0.52f, s * 0.84f)
        close()
    }
    drawPath(path, color, style = Stroke(width = s * 0.04f, cap = StrokeCap.Round))
    drawPath(path, color)
}
