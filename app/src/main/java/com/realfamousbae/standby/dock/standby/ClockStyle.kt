package com.realfamousbae.standby.dock.standby

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.times
import com.realfamousbae.standby.dock.core.prefs.HourFormat
import com.realfamousbae.standby.dock.core.prefs.StandbySettings
import kotlinx.coroutines.delay
import java.time.LocalDateTime

/**
 * Lo que las caras del reloj necesitan saber para dibujarse: colores, fuente, tamaño y
 * formato de hora. Sale de los ajustes de Iván más el modo noche.
 */
@Immutable
data class ClockStyle(
    val color: Color,
    val secondary: Color,
    val cardColor: Color,
    val flipCardColor: Color,
    val cardBorder: Color,
    val family: FontFamily,
    val weight: FontWeight,
    val sizeScale: Float,
    val is24h: Boolean,
    val showSeconds: Boolean,
    val night: Boolean,
) {
    /** Tamaño base ajustado por el porcentaje elegido en Ajustes. */
    fun size(base: TextUnit): TextUnit = sizeScale * base
}

@Composable
fun rememberClockStyle(settings: StandbySettings, night: Boolean): ClockStyle {
    val context = LocalContext.current
    val systemIs24h = remember { DateFormat.is24HourFormat(context) }
    return remember(settings, night, systemIs24h) {
        ClockStyle(
            color = if (night) StandbyColors.nightPrimary else Color(settings.clockColor),
            secondary = if (night) StandbyColors.nightSecondary else StandbyColors.secondary,
            cardColor = if (night) StandbyColors.nightCard else StandbyColors.card,
            flipCardColor = if (night) StandbyColors.nightFlipCard else StandbyColors.flipCard,
            cardBorder = if (night) StandbyColors.nightCardBorder else StandbyColors.cardBorder,
            family = StandbyFonts.family(settings.clockFont),
            weight = StandbyFonts.weight(settings.clockFont),
            sizeScale = settings.clockSizePercent / 100f,
            is24h = HourFormat.is24h(settings.hourFormat, systemIs24h),
            showSeconds = settings.showSeconds,
            night = night,
        )
    }
}

/**
 * ¿Está prendido el modo noche ahora? Por horario (se revisa cada minuto) o porque el
 * sensor dice que el cuarto está a oscuras.
 */
@Composable
fun rememberNightActive(settings: StandbySettings, lux: Float?): Boolean {
    val now = rememberNow(everySecond = false)
    return settings.nightActive(now.hour * 60 + now.minute, lux)
}

/**
 * La hora, que se refresca sola. Con segundos a la vista hace falta un tic por segundo;
 * si no, alcanza uno por minuto (menos trabajo con la pantalla prendida toda la noche).
 */
@Composable
fun rememberNow(everySecond: Boolean): LocalDateTime {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(everySecond) {
        while (true) {
            now = LocalDateTime.now()
            val ms = System.currentTimeMillis()
            // Despertarse justo cuando empieza el próximo segundo (o minuto), no "cuando salga".
            delay(if (everySecond) 1_000L - ms % 1_000L else 60_000L - ms % 60_000L)
        }
    }
    return now
}
