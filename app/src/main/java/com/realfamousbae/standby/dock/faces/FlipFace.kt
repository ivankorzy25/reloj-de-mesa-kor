package com.realfamousbae.standby.dock.faces

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.realfamousbae.standby.dock.standby.ClockStyle
import com.realfamousbae.standby.dock.standby.StandbyColors
import com.realfamousbae.standby.dock.standby.StandbyFonts
import com.realfamousbae.standby.dock.standby.rememberNow
import java.time.format.DateTimeFormatter
import kotlin.math.min

/** Cuánto dura el giro de un dígito. */
private const val FLIP_MS = 420

/**
 * Flip clock a la vieja: cada dígito es una fichita que gira sobre su eje del medio, como
 * los carteles de los aeropuertos. La mitad de arriba se cae con el dígito viejo y deja ver
 * el nuevo; después la mitad de abajo del nuevo cae en su lugar.
 */
@Composable
fun FlipFace(style: ClockStyle, modifier: Modifier = Modifier) {
    val now = rememberNow(everySecond = style.showSeconds)
    val pattern = buildString {
        append(if (style.is24h) "HH" else "hh")
        append("mm")
        if (style.showSeconds) append("ss")
    }
    val digits = now.format(DateTimeFormatter.ofPattern(pattern))

    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val groups = digits.length / 2
        // Alto de la ficha: lo que permita el ancho (fichas + separadores) y el alto.
        val perHeightByWidth = maxWidth / (groups * 2 * 0.70f + (groups - 1) * 0.34f + groups * 0.08f)
        val cardHeight = min(min(maxHeight.value * 0.82f, perHeightByWidth.value), 260f)
            .let { it * style.sizeScale }.dp
        val cardWidth = cardHeight * 0.70f

        Row(verticalAlignment = Alignment.CenterVertically) {
            repeat(groups) { group ->
                if (group > 0) Separator(cardHeight, style)
                Row(horizontalArrangement = Arrangement.spacedBy(cardHeight * 0.04f)) {
                    FlipDigit(digits[group * 2], cardWidth, cardHeight, style)
                    FlipDigit(digits[group * 2 + 1], cardWidth, cardHeight, style)
                }
            }
        }
    }
}

@Composable
private fun Separator(cardHeight: Dp, style: ClockStyle) {
    Row {
        Spacer(Modifier.width(cardHeight * 0.08f))
        BasicText(
            ":",
            style = TextStyle(
                color = style.secondary,
                fontFamily = style.family,
                fontWeight = style.weight,
                fontSize = (cardHeight.value * 0.42f).sp,
            ),
        )
        Spacer(Modifier.width(cardHeight * 0.08f))
    }
}

@Composable
private fun FlipDigit(digit: Char, width: Dp, height: Dp, style: ClockStyle) {
    var current by remember { mutableStateOf(digit) }
    var previous by remember { mutableStateOf(digit) }
    val flip = remember { Animatable(1f) }

    LaunchedEffect(digit) {
        if (digit != current) {
            previous = current
            current = digit
            flip.snapTo(0f)
            flip.animateTo(1f, tween(FLIP_MS, easing = FastOutSlowInEasing))
        }
    }

    val p = flip.value
    val fontSize = (height.value * 0.68f).sp
    Box(
        Modifier
            .size(width, height)
            .clip(RoundedCornerShape(height * 0.12f))
            .background(style.flipCardColor),
        contentAlignment = Alignment.Center,
    ) {
        // Fondo: arriba ya se ve el dígito nuevo, abajo sigue el viejo hasta la mitad del giro.
        DigitHalf(current, topHalf = true, fontSize = fontSize, style = style)
        DigitHalf(if (p < 0.5f) previous else current, topHalf = false, fontSize = fontSize, style = style)

        if (p < 1f) {
            val fallingTop = p < 0.5f
            DigitHalf(
                char = if (fallingTop) previous else current,
                topHalf = fallingTop,
                fontSize = fontSize,
                style = style,
                modifier = Modifier.graphicsLayer {
                    // Bisagra en el medio de la ficha; valor chico = más perspectiva.
                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                    cameraDistance = 14f
                    rotationX = if (fallingTop) -180f * p else 180f * (1f - p)
                },
            )
        }

        // Ranura del medio, como la ficha partida en dos.
        Box(
            Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawContent()
                    val y = size.height / 2f
                    drawRect(
                        color = StandbyColors.background,
                        topLeft = androidx.compose.ui.geometry.Offset(0f, y - size.height * 0.008f),
                        size = androidx.compose.ui.geometry.Size(size.width, size.height * 0.016f),
                    )
                },
        )
    }
}

/** El dígito dibujado completo, pero recortado a la mitad de arriba o de abajo. */
@Composable
private fun DigitHalf(
    char: Char,
    topHalf: Boolean,
    fontSize: androidx.compose.ui.unit.TextUnit,
    style: ClockStyle,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .fillMaxSize()
            .drawWithContent {
                clipRect(
                    top = if (topHalf) 0f else size.height / 2f,
                    bottom = if (topHalf) size.height / 2f else size.height,
                ) {
                    this@drawWithContent.drawContent()
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            char.toString(),
            style = TextStyle(
                color = style.color,
                fontFamily = style.family,
                fontWeight = style.weight,
                fontFeatureSettings = StandbyFonts.TABULAR_NUMBERS,
                fontSize = fontSize,
            ),
            maxLines = 1,
        )
    }
}
