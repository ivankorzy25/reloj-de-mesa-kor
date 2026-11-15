package com.realfamousbae.standby.dock.core.dim

/**
 * "Siempre encendido": en vez de apagar la pantalla, bajarla al mínimo y dejar un reloj
 * chiquito. Vuelve todo al tocar.
 *
 * Solo se atenúa cargando: con la pantalla prendida toda la noche a batería, el teléfono no
 * llega a la mañana. Y si está pasando algo (una llamada, el temporizador), no se atenúa
 * nada: justo eso es lo que hay que ver.
 */
object DimPolicy {

    const val MIN_MINUTES = 1
    const val MAX_MINUTES = 30

    fun shouldDim(
        enabled: Boolean,
        charging: Boolean,
        liveActivity: Boolean,
        idleMs: Long,
        afterMinutes: Int,
    ): Boolean = enabled &&
        charging &&
        !liveActivity &&
        idleMs >= afterMinutes.coerceIn(MIN_MINUTES, MAX_MINUTES) * 60_000L

    /**
     * Dónde dibujar el reloj chiquito, de 0 a 1 en cada lado. Se corre cada minuto siguiendo
     * dos vueltas de distinto largo (7 y 5), así tarda 35 minutos en repetir el mismo lugar:
     * en una pantalla OLED, un reloj siempre en el mismo punto se termina quemando.
     */
    fun burnInSpot(minuteOfDay: Int): Pair<Float, Float> {
        val minute = ((minuteOfDay % (7 * 5)) + 7 * 5) % (7 * 5)
        val x = (minute % 7) / 6f
        val y = (minute / 7) / 4f
        return x to y
    }
}
