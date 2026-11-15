package com.realfamousbae.standby.dock.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.realfamousbae.standby.dock.standby.ClockStyle
import kotlinx.coroutines.delay

/**
 * Tarjeta de la próxima alarma, bien grande: la hora a la que suena, qué día y cuánto falta.
 * Es la que sirve para la mesa de luz, cuando lo único que importa es a qué hora te levantás.
 */
@Composable
fun BigAlarmCard(style: ClockStyle) {
    val context = LocalContext.current
    var trigger by remember { mutableStateOf(Alarms.next(context)) }
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            trigger = Alarms.next(context)
            nowMs = System.currentTimeMillis()
            delay(30_000L)
        }
    }

    CardFrame(style, "Próxima alarma") {
        val next = trigger
        if (next == null) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CardBig("Sin alarma", style, size = 54.sp)
                CardText("No hay ninguna puesta en el teléfono.", style, maxLines = 2)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                CardText(Alarms.dayText(next), style, size = 22.sp, strong = true)
                CardBig(Alarms.hourText(next), style, size = 104.sp)
                CardText(Alarms.remainingText(next, nowMs), style, size = 20.sp)
            }
        }
    }
}
