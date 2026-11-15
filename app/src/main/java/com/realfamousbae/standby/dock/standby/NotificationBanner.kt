package com.realfamousbae.standby.dock.standby

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.realfamousbae.standby.dock.notifications.BannerNotification
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withTimeoutOrNull

/** Cuánto se queda el aviso en pantalla antes de irse solo. */
private const val HOLD_MS = 8_000L
private const val EXIT_MS = 380
private const val GAP_MS = 200L

/** Cuánto está "echado para atrás" el aviso al aparecer, en grados. */
private const val START_TILT_DEGREES = 75f

/**
 * Avisos de notificaciones arriba de la pantalla, de a uno, al estilo del StandBy del
 * iPhone: burbuja grande con el ícono de la app, el título y el texto. Aparece girando
 * sobre su borde de arriba, se queda [HOLD_MS] y se va sola. Tocarla abre la app;
 * deslizarla para arriba la saca sin abrir nada.
 */
@Composable
fun NotificationBannerHost(
    banners: Flow<BannerNotification>,
    style: ClockStyle,
    onOpen: (BannerNotification) -> Unit,
    modifier: Modifier = Modifier,
) {
    var current by remember { mutableStateOf<BannerNotification?>(null) }
    val progress = remember { Animatable(0f) }
    val dismiss = remember { Channel<Unit>(Channel.CONFLATED) }

    LaunchedEffect(banners) {
        // collect procesa de a uno: mientras hay un aviso en pantalla, los nuevos esperan.
        banners.collect { banner ->
            dismiss.tryReceive() // un toque al aviso anterior no debe cerrar el nuevo
            current = banner
            progress.snapTo(0f)
            progress.animateTo(1f, spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow))
            withTimeoutOrNull(HOLD_MS) { dismiss.receive() }
            progress.animateTo(0f, tween(EXIT_MS, easing = FastOutLinearInEasing))
            current = null
            delay(GAP_MS)
        }
    }

    val banner = current ?: return
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        BannerCard(
            banner = banner,
            style = style,
            modifier = Modifier
                .padding(top = 20.dp, start = 24.dp, end = 24.dp)
                .widthIn(max = 720.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    val p = progress.value
                    val hidden = 1f - p
                    transformOrigin = TransformOrigin(0.5f, 0f)
                    // Ya está en unidades de densidad; más chico = más perspectiva.
                    cameraDistance = 7f
                    rotationX = START_TILT_DEGREES * hidden
                    translationY = -size.height * 0.5f * hidden
                    scaleX = 0.92f + 0.08f * p
                    scaleY = scaleX
                    alpha = p.coerceIn(0f, 1f)
                }
                // Para arriba: lo saco sin abrir nada.
                .pointerInput(banner.key) {
                    detectVerticalDragGestures { change, dragAmount ->
                        if (dragAmount < -4f) {
                            change.consume()
                            dismiss.trySend(Unit)
                        }
                    }
                }
                .pointerInput(banner.key) {
                    detectTapGestures {
                        dismiss.trySend(Unit)
                        onOpen(banner)
                    }
                },
        )
    }
}

@Composable
private fun BannerCard(banner: BannerNotification, style: ClockStyle, modifier: Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(34.dp))
            .background(if (style.night) StandbyColors.nightFlipCard else StandbyColors.banner)
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        banner.icon?.let {
            Image(bitmap = it, contentDescription = null, modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)))
            Spacer(Modifier.size(18.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            BasicText(
                banner.appName,
                style = TextStyle(
                    color = if (style.night) style.secondary else StandbyColors.bannerText,
                    fontFamily = StandbyFonts.text,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // El contenido lo esconden los ajustes de la pantalla de bloqueo: solo la app.
            val title = banner.title ?: if (banner.text == null) "Notificación nueva" else null
            title?.let {
                BasicText(
                    it,
                    style = TextStyle(
                        color = if (style.night) style.color else StandbyColors.bannerTitle,
                        fontFamily = StandbyFonts.text,
                        fontWeight = FontWeight.Medium,
                        fontSize = 23.sp,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            banner.text?.let {
                BasicText(
                    it,
                    style = TextStyle(
                        color = if (style.night) style.secondary else StandbyColors.bannerText,
                        fontFamily = StandbyFonts.text,
                        fontSize = 19.sp,
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
