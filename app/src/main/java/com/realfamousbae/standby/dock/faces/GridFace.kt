package com.realfamousbae.standby.dock.faces

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.realfamousbae.standby.dock.standby.ClockStyle
import com.realfamousbae.standby.dock.standby.StandbyColors
import com.realfamousbae.standby.dock.standby.StandbyFonts
import com.realfamousbae.standby.dock.standby.dpAsSp
import com.realfamousbae.standby.dock.standby.rememberNow

/**
 * Cara de cuadrícula: los cuatro dígitos de la hora, cada uno en su celda de color pleno
 * (las horas más fuertes, los minutos un poco apagados). Es la que más se ve de lejos.
 */
@Composable
fun GridFace(style: ClockStyle, modifier: Modifier = Modifier) {
    val now = rememberNow(everySecond = false)
    val hour = if (style.is24h) now.hour else (now.hour % 12).let { if (it == 0) 12 else it }
    val digits = "%02d%02d".format(hour, now.minute)

    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        // Cuatro celdas cuadradas en dos filas y dos columnas: entre las dos de cada fila hay
        // un solo hueco, así que el lado es (lo que hay - un hueco) / 2, por el lado que menos
        // sobre.
        val gap = 16.dp
        val side = minOf((maxWidth - gap) / 2, (maxHeight - gap) / 2)
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                Cell(digits[0], side, style, strong = true)
                Cell(digits[1], side, style, strong = true)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                Cell(digits[2], side, style, strong = false)
                Cell(digits[3], side, style, strong = false)
            }
        }
    }
}

@Composable
private fun Cell(digit: Char, side: Dp, style: ClockStyle, strong: Boolean) {
    val fill = if (strong) style.color else style.color.copy(alpha = 0.55f)
    Box(
        Modifier
            .size(side)
            .aspectRatio(1f)
            .background(fill, RoundedCornerShape(side * 0.22f)),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            digit.toString(),
            style = TextStyle(
                // El dígito va calado: el color pleno es la celda, no la letra.
                color = StandbyColors.background,
                fontFamily = style.family,
                fontWeight = style.weight,
                fontFeatureSettings = StandbyFonts.TABULAR_NUMBERS,
                fontSize = dpAsSp(side.value * 0.70f),
            ),
            maxLines = 1,
        )
    }
}
