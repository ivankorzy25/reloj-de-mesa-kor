package com.realfamousbae.standby.dock.standby

import android.appwidget.AppWidgetHostView
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.realfamousbae.standby.dock.cards.SideCardHost
import android.os.SystemClock
import com.realfamousbae.standby.dock.core.dim.DimPolicy
import com.realfamousbae.standby.dock.core.prefs.ClockFace
import com.realfamousbae.standby.dock.core.prefs.SideCard
import com.realfamousbae.standby.dock.core.prefs.ScreenLayout
import com.realfamousbae.standby.dock.core.prefs.StandbySettings
import com.realfamousbae.standby.dock.core.ui.EdgeFade
import com.realfamousbae.standby.dock.faces.ClockFaceHost
import com.realfamousbae.standby.dock.live.LiveActivity
import com.realfamousbae.standby.dock.live.LiveActivityScreen
import com.realfamousbae.standby.dock.notifications.BannerNotification
import com.realfamousbae.standby.dock.media.NowPlayingSource
import com.realfamousbae.standby.dock.photos.ImmichClient
import com.realfamousbae.standby.dock.weather.WeatherRepository
import com.realfamousbae.standby.dock.widgets.StandbyWidgetHost
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

private val CardShape = RoundedCornerShape(36.dp)

/**
 * Cierra el modo con un toque, pero no cuando el dedo arrastra: pasar de cara o de tarjeta
 * no tiene que cerrar nada (ni siquiera cuando el carrusel ya está en la última página y
 * nadie se queda con el movimiento). Tampoco cuando el toque se lo quedó algo de adentro:
 * atender una llamada o tocar play no es "cerrame el reloj".
 */
private fun Modifier.exitOnTap(onExit: () -> Unit): Modifier = pointerInput(onExit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        var dragged = false
        var consumed = down.isConsumed
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (change.isConsumed) consumed = true
            if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                dragged = true
            }
            if (!change.pressed) break
        }
        if (!dragged && !consumed) onExit()
    }
}

/** Ancho del desvanecido del widget en los bordes de la tarjeta. */
private val WidgetFadeWidth = 40.dp

/**
 * Pantalla del modo: el reloj (con sus caras, que se pasan con el dedo) a toda la pantalla,
 * o dos tarjetas iguales "reloj | tarjeta", y arriba los avisos de notificaciones. Un toque
 * en el reloj cierra el modo; lo que se toca dentro de la tarjeta lo recibe la tarjeta.
 */
@Composable
fun StandbyScreen(
    settings: StandbySettings,
    style: ClockStyle,
    widgetId: Int?,
    widgetHost: StandbyWidgetHost,
    immich: ImmichClient,
    weather: WeatherRepository,
    nowPlaying: NowPlayingSource,
    banners: Flow<BannerNotification>,
    live: LiveActivity?,
    forceDim: Boolean,
    onDimChange: (Boolean) -> Unit,
    onOpenNotification: (BannerNotification) -> Unit,
    onFaceChange: (ClockFace) -> Unit,
    onCardChange: (SideCard) -> Unit,
    onCard2Change: (SideCard) -> Unit,
    onExit: () -> Unit,
) {
    // Las tarjetas (clima, agenda, alarma, música) están siempre: la segunda columna ya no
    // depende de que haya un widget del sistema elegido.
    val layout = ScreenLayout.resolve(settings.layout, cardAvailable = true)
    // Lo que está pasando ahora y se lleva toda la pantalla: la llamada o el temporizador que
    // vienen del oyente de notificaciones, y la música, que se pregunta acá.
    val music = rememberMusicLive(
        enabled = settings.liveActivitiesEnabled && settings.liveMusicFullScreen,
        source = nowPlaying,
    )
    val liveNow = if (settings.liveActivitiesEnabled) live ?: music else null

    // Siempre encendido: a los N minutos sin tocar nada, y solo cargando, la pantalla baja al
    // mínimo y queda un reloj chiquito. Cualquier toque la vuelve a encender.
    val charging = rememberCharging(LocalContext.current)
    var lastTouch by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    var dimmed by remember { mutableStateOf(false) }
    LaunchedEffect(settings.dimEnabled, settings.dimAfterMinutes, charging, liveNow != null, lastTouch, forceDim) {
        dimmed = forceDim
        if (forceDim) return@LaunchedEffect
        while (true) {
            val idle = SystemClock.elapsedRealtime() - lastTouch
            if (DimPolicy.shouldDim(
                    enabled = settings.dimEnabled,
                    charging = charging,
                    liveActivity = liveNow != null,
                    idleMs = idle,
                    afterMinutes = settings.dimAfterMinutes,
                )
            ) {
                dimmed = true
                return@LaunchedEffect
            }
            delay(10_000L)
        }
    }
    LaunchedEffect(dimmed) { onDimChange(dimmed) }

    Box(
        Modifier
            .fillMaxSize()
            .background(StandbyColors.background)
            // Cualquier toque, aunque sea el principio de un arrastre, despierta la pantalla.
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    lastTouch = SystemClock.elapsedRealtime()
                }
            }
            // Atenuada, el toque solo despierta: cerrar el modo sin querer sería lo peor.
            .exitOnTap { if (!dimmed) onExit() },
    ) {
        if (dimmed) {
            DimScreen(style, Modifier.fillMaxSize())
        } else if (liveNow != null) {
            LiveActivityScreen(
                activity = liveNow,
                style = style,
                nowPlaying = nowPlaying,
                modifier = Modifier.fillMaxSize(),
            )
        } else when (layout) {
            ScreenLayout.ClockAndWidget -> Row(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Card(Modifier.weight(1f), style) {
                    ClockFaceHost(
                        settings = settings,
                        style = style,
                        immich = immich,
                        maxTimeSize = 180.sp,
                        dateSize = 22.sp,
                        onFaceChange = onFaceChange,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                // La tarjeta se queda con sus toques: tocarla no cierra el modo.
                Card(Modifier.weight(1f).exitOnTap { }, style) {
                    SideCardHost(
                        settings = settings,
                        style = style,
                        weather = weather,
                        nowPlaying = nowPlaying,
                        widgetAvailable = widgetId != null,
                        widget = { if (widgetId != null) key(widgetId) { WidgetView(widgetHost, widgetId, style) } },
                        selected = settings.sideCard,
                        onCardChange = onCardChange,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            // Vista "Widgets" del StandBy del iPhone: dos columnas de tarjetas apiladas, cada
            // una con su propio dedo. El reloj es una tarjeta más (la de arriba de la lista).
            ScreenLayout.Widgets -> Row(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Card(Modifier.weight(1f).exitOnTap { }, style) {
                    SideCardHost(
                        settings = settings,
                        style = style,
                        weather = weather,
                        nowPlaying = nowPlaying,
                        // El widget del sistema va en la primera columna: una sola vista de
                        // widget por pantalla (el host de Android no admite dos iguales).
                        widgetAvailable = widgetId != null,
                        widget = { if (widgetId != null) key(widgetId) { WidgetView(widgetHost, widgetId, style) } },
                        selected = settings.sideCard,
                        onCardChange = onCardChange,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Card(Modifier.weight(1f).exitOnTap { }, style) {
                    SideCardHost(
                        settings = settings,
                        style = style,
                        weather = weather,
                        nowPlaying = nowPlaying,
                        widgetAvailable = false,
                        widget = {},
                        selected = settings.sideCard2,
                        onCardChange = onCard2Change,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            ScreenLayout.ClockOnly -> ClockFaceHost(
                settings = settings,
                style = style,
                immich = immich,
                maxTimeSize = 420.sp,
                dateSize = 28.sp,
                onFaceChange = onFaceChange,
                modifier = Modifier.fillMaxSize(),
            )
        }
        NotificationBannerHost(
            banners = banners,
            style = style,
            onOpen = onOpenNotification,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

/**
 * ¿Hay música sonando para mostrarla grande? Se pregunta cada dos segundos y sin tocar la
 * carátula: lo caro (bajar la imagen) lo hace recién la pantalla de la música.
 */
@Composable
private fun rememberMusicLive(enabled: Boolean, source: NowPlayingSource): LiveActivity? {
    var sonando by remember { mutableStateOf(false) }
    LaunchedEffect(enabled) {
        if (!enabled) {
            sonando = false
            return@LaunchedEffect
        }
        while (true) {
            sonando = source.isPlaying()
            delay(2_000L)
        }
    }
    return LiveActivity.Music.takeIf { sonando }
}

@Composable
internal fun Card(modifier: Modifier, style: ClockStyle, content: @Composable () -> Unit) {
    Box(
        modifier
            .fillMaxHeight()
            .clip(CardShape)
            .background(style.cardColor)
            .border(1.dp, style.cardBorder, CardShape),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** Widget del sistema; le avisamos el tamaño real de la tarjeta para que se acomode. */
@Composable
private fun WidgetView(widgetHost: StandbyWidgetHost, widgetId: Int, style: ClockStyle) {
    val density = LocalDensity.current
    val context = LocalContext.current
    val hostView: AppWidgetHostView? = remember { widgetHost.createView(context, widgetId) }
    AndroidView(
        factory = { hostView ?: View(it) },
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .fadeEdges(WidgetFadeWidth, style.cardColor)
            .onSizeChanged { size ->
                val view = hostView ?: return@onSizeChanged
                with(density) { widgetHost.updateSize(view, size.width.toDp().value, size.height.toDp().value) }
            },
    )
}

/**
 * Desvanece el borde del widget contra la tarjeta: encima del widget, en cada borde, se
 * dibuja un "velo" del color de la tarjeta con perfil gaussiano de transparencia
 * ([EdgeFade]). El fondo de la tarjeta es sólido, así que es lo mismo que una máscara de
 * transparencia, pero con tramado: entre el fondo del widget y el de la tarjeta hay pocos
 * niveles de brillo y sin tramado el degradado se vería escalonado.
 */
private fun Modifier.fadeEdges(width: Dp, color: Color): Modifier = drawWithCache {
    val fade = minOf(width.toPx(), size.minDimension / 2)
    val stops = EdgeFade.stops()
    val positions = stops.map { it.first }.toFloatArray()
    val colors = stops.map { color.copy(alpha = it.second).toArgb() }.toIntArray()
    fun paint(x0: Float, y0: Float, x1: Float, y1: Float) = Paint().apply {
        isDither = true
        isAntiAlias = false
        shader = LinearGradient(x0, y0, x1, y1, colors, positions, Shader.TileMode.CLAMP)
    }
    val w = size.width
    val h = size.height
    val left = paint(0f, 0f, fade, 0f)
    val right = paint(w, 0f, w - fade, 0f)
    val top = paint(0f, 0f, 0f, fade)
    val bottom = paint(0f, h, 0f, h - fade)
    onDrawWithContent {
        drawContent()
        if (fade <= 0f) return@onDrawWithContent
        drawIntoCanvas { canvas ->
            val c = canvas.nativeCanvas
            c.drawRect(0f, 0f, fade, h, left)
            c.drawRect(w - fade, 0f, w, h, right)
            c.drawRect(0f, 0f, w, fade, top)
            c.drawRect(0f, h - fade, w, h, bottom)
        }
    }
}
