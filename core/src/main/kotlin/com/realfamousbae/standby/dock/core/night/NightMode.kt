package com.realfamousbae.standby.dock.core.night

/**
 * Modo noche: la pantalla pasa a rojo tenue. Se prende por horario (de [startMinutes] a
 * [endMinutes], que puede cruzar la medianoche) y, si está permitido, también cuando el
 * sensor de luz dice que el cuarto está a oscuras.
 */
object NightMode {

    /** Debajo de esta luz (lux) se considera cuarto a oscuras. */
    const val DARK_LUX = 3f

    /** @param nowMinutes minutos desde las 00:00. */
    fun inSchedule(nowMinutes: Int, startMinutes: Int, endMinutes: Int): Boolean {
        val now = wrap(nowMinutes)
        val start = wrap(startMinutes)
        val end = wrap(endMinutes)
        // Mismo valor de inicio y fin = siempre (horario de 24 h).
        if (start == end) return true
        return if (start < end) now >= start && now < end else now >= start || now < end
    }

    /**
     * @param lux última lectura del sensor de luz; null si no hay sensor o todavía no llegó.
     */
    fun isActive(
        enabled: Boolean,
        nowMinutes: Int,
        startMinutes: Int,
        endMinutes: Int,
        useLightSensor: Boolean,
        lux: Float?,
    ): Boolean {
        if (!enabled) return false
        if (inSchedule(nowMinutes, startMinutes, endMinutes)) return true
        return useLightSensor && lux != null && lux < DARK_LUX
    }

    private fun wrap(minutes: Int): Int = ((minutes % 1440) + 1440) % 1440
}
