package com.realfamousbae.standby.dock.live

import android.app.PendingIntent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.realfamousbae.standby.dock.core.live.LiveClock
import com.realfamousbae.standby.dock.media.NowPlaying
import com.realfamousbae.standby.dock.media.NowPlayingSource
import com.realfamousbae.standby.dock.standby.ClockStyle
import com.realfamousbae.standby.dock.standby.StandbyFonts
import com.realfamousbae.standby.dock.standby.dpAsSp
import com.realfamousbae.standby.dock.standby.rememberNow
import kotlinx.coroutines.delay
import java.time.format.DateTimeFormatter

/** Verde de atender y rojo de colgar. De noche van apagados, como todo lo demás. */
private val VERDE = Color(0xFF2FA85C)
private val ROJO = Color(0xFFD1392B)

/**
 * Una actividad en vivo a toda la pantalla: la llamada que está entrando, el temporizador que
 * corre o la música que suena. Arriba a la derecha queda la hora chica, para que el reloj no
 * desaparezca del todo.
 */
@Composable
fun LiveActivityScreen(
    activity: LiveActivity,
    style: ClockStyle,
    nowPlaying: NowPlayingSource,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize()) {
        when (activity) {
            is LiveActivity.Call -> CallBody(activity, style)
            is LiveActivity.Timer -> TimerBody(activity, style)
            LiveActivity.Music -> MusicBody(style, nowPlaying)
        }
        SmallClock(style, Modifier.align(Alignment.TopEnd).padding(28.dp))
    }
}

@Composable
private fun SmallClock(style: ClockStyle, modifier: Modifier = Modifier) {
    val now = rememberNow(everySecond = false)
    val pattern = if (style.is24h) "HH:mm" else "h:mm"
    BasicText(
        now.format(DateTimeFormatter.ofPattern(pattern)),
        modifier = modifier,
        style = TextStyle(
            color = style.secondary,
            fontFamily = style.family,
            fontWeight = style.weight,
            fontFeatureSettings = StandbyFonts.TABULAR_NUMBERS,
            fontSize = 34.sp,
        ),
        maxLines = 1,
    )
}

@Composable
private fun CallBody(call: LiveActivity.Call, style: ClockStyle) {
    BoxWithConstraints(
        Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
    // El nombre es lo que más crece; los botones tienen que entrar sí o sí, así que el
    // nombre se mide contra el alto de la pantalla y no al revés.
    val nombre = dpAsSp(minOf(maxHeight.value * 0.22f, 96f) * style.sizeScale)
    val boton = minOf(maxHeight.value * 0.28f, 104f).dp
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        call.icon?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                modifier = Modifier.size(72.dp).clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        }
        BasicText(
            if (call.incoming) "Llamada entrante" else "En llamada",
            style = TextStyle(
                color = style.secondary,
                fontFamily = StandbyFonts.text,
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp,
                letterSpacing = 2.sp,
            ),
            maxLines = 1,
        )
        BasicText(
            call.name,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
            style = TextStyle(
                color = style.color,
                fontFamily = style.family,
                fontWeight = style.weight,
                fontSize = nombre,
                textAlign = TextAlign.Center,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        BasicText(
            call.app,
            style = TextStyle(color = style.secondary, fontFamily = StandbyFonts.text, fontSize = 18.sp),
            maxLines = 1,
        )
        Row(
            Modifier.padding(top = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(56.dp),
        ) {
            call.hangUp?.let { intent ->
                RoundButton(ROJO, style, boton, onClick = { intent.sendQuietly() }) { HangUpGlyph(boton) }
            }
            call.answer?.let { intent ->
                RoundButton(VERDE, style, boton, onClick = { intent.sendQuietly() }) { AnswerGlyph(boton) }
            }
        }
    }
    }
}

@Composable
private fun TimerBody(timer: LiveActivity.Timer, style: ClockStyle) {
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(timer) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(1_000L - System.currentTimeMillis() % 1_000L)
        }
    }
    val left = LiveClock.elapsed(timer.baseMs, nowMs, timer.countDown)
    val finished = LiveClock.finished(timer.baseMs, nowMs, timer.countDown)

    BoxWithConstraints(Modifier.fillMaxSize().padding(40.dp), contentAlignment = Alignment.Center) {
        val cuerpo = minOf(maxHeight.value * 0.42f, maxWidth.value * 0.22f)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BasicText(
                if (finished) "¡Se terminó!" else timer.label,
                style = TextStyle(
                    color = style.secondary,
                    fontFamily = StandbyFonts.text,
                    fontWeight = FontWeight.Medium,
                    fontSize = 22.sp,
                    letterSpacing = 2.sp,
                ),
                maxLines = 1,
            )
            BasicText(
                LiveClock.format(left),
                style = TextStyle(
                    color = style.color,
                    fontFamily = style.family,
                    fontWeight = style.weight,
                    fontFeatureSettings = StandbyFonts.TABULAR_NUMBERS,
                    fontSize = dpAsSp(cuerpo * style.sizeScale),
                ),
                maxLines = 1,
            )
            Row(Modifier.padding(top = 28.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                timer.actions.forEach { action ->
                    PillButton(action.title, style) { action.intent.sendQuietly() }
                }
            }
        }
    }
}

@Composable
private fun MusicBody(style: ClockStyle, source: NowPlayingSource) {
    var playing by remember { mutableStateOf<NowPlaying?>(null) }
    LaunchedEffect(Unit) {
        while (true) {
            playing = source.read(playing)
            delay(1_000L)
        }
    }
    val now = playing
    if (now == null) {
        // Puede pasar con la actividad de mentira del DEMO, o si la música se corta justo.
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            BasicText(
                "Silencio",
                style = TextStyle(
                    color = style.secondary,
                    fontFamily = style.family,
                    fontWeight = style.weight,
                    fontSize = style.size(64.sp),
                ),
                maxLines = 1,
            )
        }
        return
    }

    Row(
        Modifier.fillMaxSize().padding(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(44.dp),
    ) {
        now.art?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                modifier = Modifier.size(260.dp).clip(RoundedCornerShape(28.dp)),
                contentScale = ContentScale.Crop,
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BasicText(
                now.title,
                style = TextStyle(
                    color = style.color,
                    fontFamily = style.family,
                    fontWeight = style.weight,
                    fontSize = style.size(56.sp),
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            now.artist?.let {
                BasicText(
                    it,
                    style = TextStyle(color = style.secondary, fontFamily = StandbyFonts.text, fontSize = 28.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BasicText(
                now.app,
                style = TextStyle(color = style.secondary, fontFamily = StandbyFonts.text, fontSize = 18.sp),
                maxLines = 1,
            )
            Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                SkipButton(style, back = true) { source.previous() }
                PlayPauseButton(now.playing, style) { source.playPause() }
                SkipButton(style, back = false) { source.next() }
            }
        }
    }
}

// --- Botones ---

@Composable
private fun RoundButton(
    color: Color,
    style: ClockStyle,
    side: Dp,
    onClick: () -> Unit,
    glyph: @Composable () -> Unit,
) {
    val noRipple = remember { MutableInteractionSource() }
    Box(
        Modifier
            .size(side)
            // De noche los botones también se apagan: encandilar con un verde pleno a las 3
            // de la mañana es exactamente lo que el modo noche viene a evitar.
            .background(if (style.night) color.copy(alpha = 0.45f) else color, CircleShape)
            .clickable(interactionSource = noRipple, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { glyph() }
}

@Composable
private fun AnswerGlyph(side: Dp) = PhoneGlyph(side, rotated = false)

@Composable
private fun HangUpGlyph(side: Dp) = PhoneGlyph(side, rotated = true)

/** Un tubo de teléfono dibujado a mano; el de colgar es el mismo dado vuelta. */
@Composable
private fun PhoneGlyph(side: Dp, rotated: Boolean) {
    Canvas(Modifier.size(side / 2)) {
        val s = size.minDimension
        val path = Path().apply {
            moveTo(s * 0.22f, s * 0.30f)
            cubicTo(s * 0.20f, s * 0.60f, s * 0.40f, s * 0.80f, s * 0.70f, s * 0.78f)
            lineTo(s * 0.78f, s * 0.62f)
            lineTo(s * 0.58f, s * 0.52f)
            lineTo(s * 0.48f, s * 0.62f)
            cubicTo(s * 0.40f, s * 0.58f, s * 0.42f, s * 0.60f, s * 0.38f, s * 0.52f)
            lineTo(s * 0.48f, s * 0.42f)
            lineTo(s * 0.38f, s * 0.22f)
            close()
        }
        rotate(degrees = if (rotated) 135f else 0f, pivot = Offset(s / 2, s / 2)) {
            drawPath(path, Color.White)
        }
    }
}

@Composable
private fun PillButton(text: String, style: ClockStyle, onClick: () -> Unit) {
    val noRipple = remember { MutableInteractionSource() }
    Box(
        Modifier
            .clip(CircleShape)
            .background(style.cardColor)
            .clickable(interactionSource = noRipple, indication = null, onClick = onClick)
            .padding(horizontal = 28.dp, vertical = 14.dp),
    ) {
        BasicText(
            text,
            style = TextStyle(
                color = style.color,
                fontFamily = StandbyFonts.text,
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp,
            ),
            maxLines = 1,
        )
    }
}

@Composable
private fun PlayPauseButton(playing: Boolean, style: ClockStyle, onClick: () -> Unit) {
    val noRipple = remember { MutableInteractionSource() }
    Canvas(
        Modifier.size(76.dp).clickable(interactionSource = noRipple, indication = null, onClick = onClick),
    ) {
        val s = size.minDimension
        if (playing) {
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
private fun SkipButton(style: ClockStyle, back: Boolean, onClick: () -> Unit) {
    val noRipple = remember { MutableInteractionSource() }
    Canvas(
        Modifier.size(62.dp).clickable(interactionSource = noRipple, indication = null, onClick = onClick),
    ) {
        val s = size.minDimension
        val dir = if (back) -1f else 1f
        val cx = s / 2
        drawPath(
            Path().apply {
                moveTo(cx - dir * s * 0.22f, s * 0.26f)
                lineTo(cx + dir * s * 0.12f, s * 0.50f)
                lineTo(cx - dir * s * 0.22f, s * 0.74f)
                close()
            },
            style.color,
        )
        drawLine(
            color = style.color,
            start = Offset(cx + dir * s * 0.20f, s * 0.26f),
            end = Offset(cx + dir * s * 0.20f, s * 0.74f),
            strokeWidth = s * 0.09f,
            cap = StrokeCap.Round,
        )
    }
}

/** Si la app que mandó la notificación ya no está, tocar el botón no puede tirar el modo. */
private fun PendingIntent.sendQuietly() {
    runCatching { send() }
}
