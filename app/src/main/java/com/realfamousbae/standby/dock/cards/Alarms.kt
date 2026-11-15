package com.realfamousbae.standby.dock.cards

import android.app.AlarmManager
import android.content.Context
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Cuentas de la próxima alarma del sistema, compartidas por las dos tarjetas que la muestran. */
internal object Alarms {

    private val esAR = Locale.forLanguageTag("es-AR")

    /** La próxima alarma puesta en el teléfono (cualquier app de reloj), o null si no hay. */
    fun next(context: Context): Long? =
        context.getSystemService(AlarmManager::class.java)?.nextAlarmClock?.triggerTime

    /** "Hoy", "Mañana", "Lun 17". */
    fun dayText(triggerMs: Long): String {
        val time = Instant.ofEpochMilli(triggerMs).atZone(ZoneId.systemDefault())
        val today = LocalDate.now(ZoneId.systemDefault())
        return when (time.toLocalDate()) {
            today -> "Hoy"
            today.plusDays(1) -> "Mañana"
            else -> time.format(DateTimeFormatter.ofPattern("EEE d", esAR))
                .replaceFirstChar { it.titlecase(esAR) }
        }
    }

    /** "07:30" (la alarma siempre se muestra en 24 h, como la app de reloj de Samsung). */
    fun hourText(triggerMs: Long): String = Instant.ofEpochMilli(triggerMs)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("HH:mm"))

    /** "Hoy 07:30" en una sola línea. */
    fun shortText(triggerMs: Long?): String {
        triggerMs ?: return "No hay ninguna puesta"
        return "${dayText(triggerMs)} ${hourText(triggerMs)}"
    }

    /** "En 7 h 20 min", para la tarjeta grande. */
    fun remainingText(triggerMs: Long, nowMs: Long): String {
        val minutes = ((triggerMs - nowMs) / 60_000L).coerceAtLeast(0L)
        val hours = minutes / 60
        val rest = minutes % 60
        return when {
            minutes < 1 -> "Suena ahora"
            hours == 0L -> "En $rest min"
            rest == 0L -> "En $hours h"
            else -> "En $hours h $rest min"
        }
    }
}
