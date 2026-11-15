package com.realfamousbae.standby.dock.cards

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.realfamousbae.standby.dock.media.NowPlaying
import com.realfamousbae.standby.dock.media.NowPlayingSource
import com.realfamousbae.standby.dock.standby.ClockStyle
import kotlinx.coroutines.delay

/** Tarjeta de lo que está sonando: carátula, título, quién lo toca y play/pausa. */
@Composable
fun NowPlayingCard(style: ClockStyle, source: NowPlayingSource) {
    var playing by remember { mutableStateOf<NowPlaying?>(null) }
    var allowed by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        while (true) {
            allowed = source.hasAccess()
            playing = if (allowed) source.read(playing) else null
            delay(1_000L)
        }
    }

    CardFrame(style, "Reproduciendo") {
        val now = playing
        when {
            !allowed -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                CardText("Falta el acceso a notificaciones.", style, strong = true)
                CardText("Se da desde Ajustes de la app.", style)
            }
            now == null -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                CardBig("Silencio", style, size = 54.sp)
                CardText("Cuando pongas música aparece acá.", style, maxLines = 2)
            }
            else -> Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                now.art?.let {
                    Image(
                        bitmap = it,
                        contentDescription = null,
                        modifier = Modifier.size(130.dp).clip(RoundedCornerShape(18.dp)),
                        contentScale = ContentScale.Crop,
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    CardText(now.title, style, size = 26.sp, strong = true, maxLines = 2)
                    now.artist?.let { CardText(it, style, size = 19.sp) }
                    CardText(now.app, style, size = 15.sp)
                }
                TransportButton(playing = now.playing, style = style) { source.playPause() }
                NextButton(style) { source.next() }
            }
        }
    }
}

@Composable
private fun TransportButton(playing: Boolean, style: ClockStyle, onClick: () -> Unit) {
    val noRipple = remember { MutableInteractionSource() }
    Canvas(
        Modifier
            .size(64.dp)
            .clickable(interactionSource = noRipple, indication = null, onClick = onClick),
    ) {
        val s = size.minDimension
        if (playing) {
            // Pausa: dos barras.
            val w = s * 0.14f
            drawRoundRect(
                color = style.color,
                topLeft = Offset(s * 0.28f, s * 0.24f),
                size = androidx.compose.ui.geometry.Size(w, s * 0.52f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w / 3, w / 3),
            )
            drawRoundRect(
                color = style.color,
                topLeft = Offset(s * 0.58f, s * 0.24f),
                size = androidx.compose.ui.geometry.Size(w, s * 0.52f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w / 3, w / 3),
            )
        } else {
            drawPath(
                Path().apply {
                    moveTo(s * 0.32f, s * 0.22f)
                    lineTo(s * 0.78f, s * 0.50f)
                    lineTo(s * 0.32f, s * 0.78f)
                    close()
                },
                style.color,
            )
        }
    }
}

@Composable
private fun NextButton(style: ClockStyle, onClick: () -> Unit) {
    val noRipple = remember { MutableInteractionSource() }
    Canvas(
        Modifier
            .size(52.dp)
            .clickable(interactionSource = noRipple, indication = null, onClick = onClick),
    ) {
        val s = size.minDimension
        drawPath(
            Path().apply {
                moveTo(s * 0.26f, s * 0.26f)
                lineTo(s * 0.62f, s * 0.50f)
                lineTo(s * 0.26f, s * 0.74f)
                close()
            },
            style.color,
        )
        drawRoundRect(
            color = style.color,
            topLeft = Offset(s * 0.66f, s * 0.26f),
            size = androidx.compose.ui.geometry.Size(s * 0.10f, s * 0.48f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.03f, s * 0.03f),
        )
    }
}
