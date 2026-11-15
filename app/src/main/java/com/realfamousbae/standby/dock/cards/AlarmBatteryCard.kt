package com.realfamousbae.standby.dock.cards

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.realfamousbae.standby.dock.standby.ClockStyle
import kotlinx.coroutines.delay

private data class PowerState(val percent: Int, val charging: Boolean, val nextAlarmMs: Long?)

/** Tarjeta de la mesa de luz: la próxima alarma y cuánta batería queda. */
@Composable
fun AlarmBatteryCard(style: ClockStyle) {
    val context = LocalContext.current
    var state by remember { mutableStateOf(read(context)) }

    LaunchedEffect(Unit) {
        while (true) {
            state = read(context)
            delay(30_000L)
        }
    }

    CardFrame(style, "Alarma y batería") {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Column {
                CardText(if (state.charging) "Cargando" else "Batería", style)
                CardBig("${state.percent}%", style, size = 86.sp)
                BatteryBar(state.percent, state.charging, style)
            }
            Column {
                CardText("Próxima alarma", style)
                CardText(Alarms.shortText(state.nextAlarmMs), style, size = 24.sp, strong = true)
            }
        }
    }
}

@Composable
private fun BatteryBar(percent: Int, charging: Boolean, style: ClockStyle) {
    Box(
        Modifier
            .fillMaxWidth(0.72f)
            .height(10.dp)
            .background(style.secondary.copy(alpha = 0.25f), RoundedCornerShape(5.dp)),
    ) {
        Box(
            Modifier
                .fillMaxWidth((percent.coerceIn(0, 100)) / 100f)
                .height(10.dp)
                .background(if (charging) style.color else style.color.copy(alpha = 0.8f), RoundedCornerShape(5.dp)),
        )
    }
}

private fun read(context: Context): PowerState {
    val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
    val status = battery?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val alarm = Alarms.next(context)
    return PowerState(
        percent = if (level >= 0 && scale > 0) level * 100 / scale else 0,
        charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL,
        nextAlarmMs = alarm,
    )
}
