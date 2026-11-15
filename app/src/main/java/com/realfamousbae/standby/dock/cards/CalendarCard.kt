package com.realfamousbae.standby.dock.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.realfamousbae.standby.dock.calendar.AgendaEvent
import com.realfamousbae.standby.dock.calendar.CalendarEvents
import com.realfamousbae.standby.dock.standby.ClockStyle
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Tarjeta de la agenda: lo que viene, con el día en criollo ("Hoy 19:30"). */
@Composable
fun CalendarCard(style: ClockStyle) {
    val context = LocalContext.current
    var events by remember { mutableStateOf<List<AgendaEvent>?>(null) }
    var allowed by remember { mutableStateOf(CalendarEvents.hasPermission(context)) }

    LaunchedEffect(Unit) {
        while (true) {
            allowed = CalendarEvents.hasPermission(context)
            events = if (allowed) CalendarEvents.next(context) else emptyList()
            delay(5 * 60 * 1000L)
        }
    }

    CardFrame(style, "Agenda") {
        val list = events
        when {
            !allowed -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                CardText("Falta el permiso de calendario.", style, strong = true)
                CardText("Se da desde Ajustes de la app.", style)
            }
            list == null -> CardText("Mirando la agenda…", style)
            list.isEmpty() -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                CardBig("Nada", style, size = 64.sp)
                CardText("No hay eventos en los próximos días.", style)
            }
            else -> Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                list.forEachIndexed { index, event ->
                    EventRow(event, style, big = index == 0)
                }
            }
        }
    }
}

@Composable
private fun EventRow(event: AgendaEvent, style: ClockStyle, big: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            Modifier
                .size(if (big) 14.dp else 10.dp)
                .background(dotColor(event, style), CircleShape),
        )
        Column {
            CardText(event.title, style, size = if (big) 24.sp else 18.sp, strong = true)
            CardText(whenText(event), style, size = if (big) 18.sp else 15.sp)
        }
    }
}

private fun dotColor(event: AgendaEvent, style: ClockStyle): Color =
    if (style.night || event.color == 0) style.color else Color(event.color).copy(alpha = 1f)

/** "Hoy 19:30", "Mañana 09:00", "Lun 17 10:00", "Todo el día". */
private fun whenText(event: AgendaEvent): String {
    val zone = ZoneId.systemDefault()
    val start = Instant.ofEpochMilli(event.startMs).atZone(zone)
    val today = LocalDate.now(zone)
    val day = when (start.toLocalDate()) {
        today -> "Hoy"
        today.plusDays(1) -> "Mañana"
        else -> start.format(DateTimeFormatter.ofPattern("EEE d", Locale.forLanguageTag("es-AR")))
            .replaceFirstChar { it.titlecase(Locale.forLanguageTag("es-AR")) }
    }
    if (event.allDay) return "$day · todo el día"
    return "$day ${start.format(DateTimeFormatter.ofPattern("HH:mm"))}"
}
