package com.realfamousbae.standby.dock.standby

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.realfamousbae.standby.dock.core.dim.DimPolicy
import kotlinx.coroutines.delay

/**
 * La pantalla atenuada del "siempre encendido": la hora chiquita y nada más, en el brillo
 * más bajo que tiene el teléfono. Cada minuto se corre un poco de lugar, porque un reloj
 * clavado en el mismo punto toda la noche termina quemando la pantalla OLED.
 */
@Composable
fun DimScreen(style: ClockStyle, modifier: Modifier = Modifier) {
    val now = rememberNow(everySecond = false)
    val (fx, fy) = DimPolicy.burnInSpot(now.hour * 60 + now.minute)
    // El salto de un lugar al otro se hace despacio, para que de reojo no parezca un parpadeo.
    val x by animateFloatAsState(fx, tween(4_000), label = "x")
    val y by animateFloatAsState(fy, tween(4_000), label = "y")

    BoxWithConstraints(modifier.fillMaxSize()) {
        val libreX = (maxWidth - 220.dp).coerceAtLeast(0.dp)
        val libreY = (maxHeight - 90.dp).coerceAtLeast(0.dp)
        Box(
            Modifier
                .offset(x = libreX * x, y = libreY * y)
                .padding(8.dp),
        ) {
            BasicText(
                now.format(
                    java.time.format.DateTimeFormatter.ofPattern(if (style.is24h) "HH:mm" else "h:mm"),
                ),
                style = TextStyle(
                    // Bien apagado: de noche la pantalla tiene que ser casi un reloj de papel.
                    color = style.color.copy(alpha = if (style.night) 0.45f else 0.6f),
                    fontFamily = style.family,
                    fontWeight = style.weight,
                    fontFeatureSettings = StandbyFonts.TABULAR_NUMBERS,
                    fontSize = 64.sp,
                ),
                maxLines = 1,
            )
        }
    }
}

/**
 * ¿Está cargando ahora? Lo necesita la atenuación: sin cargador la pantalla se apaga como
 * siempre, porque dejarla prendida toda la noche a batería no termina bien.
 */
@Composable
fun rememberCharging(context: Context): Boolean {
    var charging by remember { mutableStateOf(readCharging(context)) }
    LaunchedEffect(Unit) {
        while (true) {
            charging = readCharging(context)
            delay(20_000L)
        }
    }
    return charging
}

private fun readCharging(context: Context): Boolean {
    val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    // Alcanza con que esté enchufado: con la batería llena el teléfono dice "no cargando",
    // y sin embargo está en el cargador, que es lo que acá importa.
    return (battery?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0) != 0
}
