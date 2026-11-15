package com.realfamousbae.standby.dock.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.realfamousbae.standby.dock.core.prefs.ClockFace
import com.realfamousbae.standby.dock.core.prefs.StandbySettings
import com.realfamousbae.standby.dock.faces.AnalogFace
import com.realfamousbae.standby.dock.faces.DigitalFace
import com.realfamousbae.standby.dock.faces.FlipFace
import com.realfamousbae.standby.dock.faces.PhotosFace
import com.realfamousbae.standby.dock.graph
import com.realfamousbae.standby.dock.standby.StandbyColors
import com.realfamousbae.standby.dock.standby.rememberClockStyle
import com.realfamousbae.standby.dock.standby.rememberNightActive

/**
 * Cómo va a quedar: la misma cara del reloj que se ve en el modo, con los colores, la
 * fuente y el tamaño elegidos, al toque mientras se tocan los controles.
 */
@Composable
fun LivePreview(settings: StandbySettings, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    // En la previsualización el sensor de luz no cuenta: solo el horario.
    val night = rememberNightActive(settings, lux = null)
    val style = rememberClockStyle(settings, night = night)

    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(2f)
            .clip(RoundedCornerShape(24.dp))
            .background(StandbyColors.background),
    ) {
        when (settings.clockFace) {
            ClockFace.Digital -> DigitalFace(
                style = style,
                maxTimeSize = 110.sp,
                dateSize = 16.sp,
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp),
            )
            ClockFace.Analogico -> AnalogFace(style, Modifier.fillMaxSize().padding(10.dp))
            ClockFace.Flip -> FlipFace(style, Modifier.fillMaxSize().padding(10.dp))
            ClockFace.Fotos -> PhotosFace(settings, style, context.graph.immich, Modifier.fillMaxSize())
        }
    }
}
