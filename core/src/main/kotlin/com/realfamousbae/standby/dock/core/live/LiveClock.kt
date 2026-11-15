package com.realfamousbae.standby.dock.core.live

import kotlin.math.abs

/**
 * El reloj de las actividades en vivo (temporizador y cronómetro): cuánto falta o cuánto va,
 * escrito como lo escribe el teléfono. Es lógica pura, así que se prueba sin teléfono.
 */
object LiveClock {

    /**
     * "9:58", "1:02:30", "0:00". Las horas solo aparecen si las hay, y los minutos y segundos
     * van con dos dígitos salvo el primer número.
     */
    fun format(millis: Long): String {
        val total = (abs(millis) + 999) / 1000
        val hours = total / 3600
        val minutes = (total % 3600) / 60
        val seconds = total % 60
        return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
        else "%d:%02d".format(minutes, seconds)
    }

    /**
     * Lo que muestra un cronómetro de notificación: si cuenta para atrás, lo que falta hasta
     * [baseMs] (nunca menos de cero); si cuenta para adelante, lo que pasó desde [baseMs].
     */
    fun elapsed(baseMs: Long, nowMs: Long, countDown: Boolean): Long =
        if (countDown) (baseMs - nowMs).coerceAtLeast(0L) else (nowMs - baseMs).coerceAtLeast(0L)

    /** Un temporizador que llegó a cero (suena la alarma) se avisa distinto. */
    fun finished(baseMs: Long, nowMs: Long, countDown: Boolean): Boolean =
        countDown && nowMs >= baseMs
}
