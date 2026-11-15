package com.realfamousbae.standby.dock.faces

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.realfamousbae.standby.dock.core.prefs.StandbySettings
import com.realfamousbae.standby.dock.photos.ImmichClient
import com.realfamousbae.standby.dock.standby.ClockStyle
import com.realfamousbae.standby.dock.standby.StandbyFonts
import com.realfamousbae.standby.dock.standby.rememberNow
import kotlinx.coroutines.delay
import java.time.format.DateTimeFormatter

/** Cada cuánto cambia la foto. */
private const val PHOTO_MS = 20_000L

/** Cuántos ids se piden de una (se vuelven a pedir cuando se terminan). */
private const val BATCH = 40

/**
 * Carrusel de fotos de Immich con la hora chica abajo a la izquierda. Las fotos se bajan de
 * a una y se cambian cada 20 s con un fundido; si no hay clave o no hay red, queda la hora
 * sobre negro y un cartelito explicando qué falta.
 */
@Composable
fun PhotosFace(
    settings: StandbySettings,
    style: ClockStyle,
    client: ImmichClient,
    modifier: Modifier = Modifier,
) {
    var photo by remember { mutableStateOf<ImageBitmap?>(null) }
    var problem by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(settings.immichUrl, settings.immichApiKey, settings.immichOnlyFavorites) {
        if (!settings.immichReady) {
            problem = "Para ver tus fotos, pegá la clave de Immich en Ajustes."
            return@LaunchedEffect
        }
        val pending = ArrayDeque<String>()
        while (true) {
            if (pending.isEmpty()) {
                pending.addAll(client.randomIds(settings, BATCH))
                if (pending.isEmpty()) {
                    problem = "No se pudo hablar con Immich. Se reintenta en un rato."
                    delay(60_000L)
                    continue
                }
            }
            val next = client.preview(settings, pending.removeFirst())
            if (next != null) {
                photo = next
                problem = null
                delay(PHOTO_MS)
            } else {
                // Una foto que falla no corta el carrusel: se pasa a la siguiente.
                delay(2_000L)
            }
        }
    }

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val current = photo
        // Crossfade: la foto nueva aparece mientras la vieja se va, sin parpadeo a negro.
        Crossfade(targetState = current, animationSpec = tween(900), label = "foto") { shown ->
            if (shown != null) {
                Image(
                    bitmap = shown,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        if (current != null) {
            // De noche, las fotos también se apagan: un velo rojo bien tenue.
            if (style.night) {
                Box(Modifier.fillMaxSize().background(Color(0x66330A00)))
            }
            // Sombra abajo para que la hora se lea sobre cualquier foto.
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(0.55f to Color.Transparent, 1f to Color(0xCC000000)),
                ),
            )
        }

        Column(
            Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Bottom,
        ) {
            val now = rememberNow(everySecond = style.showSeconds)
            val pattern = buildString {
                append(if (style.is24h) "HH:mm" else "h:mm")
                if (style.showSeconds) append(":ss")
            }
            BasicText(
                now.format(DateTimeFormatter.ofPattern(pattern)),
                style = TextStyle(
                    color = if (current == null) style.color else Color(0xFFF2F2F2),
                    fontFamily = style.family,
                    fontWeight = style.weight,
                    fontFeatureSettings = StandbyFonts.TABULAR_NUMBERS,
                    fontSize = style.size(64.sp),
                ),
                maxLines = 1,
            )
            problem?.let {
                BasicText(
                    it,
                    style = TextStyle(
                        color = style.secondary,
                        fontFamily = StandbyFonts.text,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Start,
                    ),
                )
            }
        }
    }
}
