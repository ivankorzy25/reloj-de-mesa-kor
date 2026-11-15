package com.realfamousbae.standby.dock.core.ui

/** Los dos colores con los que se pinta la hora en la cara Solar, y cómo se llama ese momento. */
data class SolarLook(val top: Long, val bottom: Long, val label: String)

/**
 * El color de la cara Solar según la hora del día. El fondo se deja negro (pantalla OLED) y
 * lo que cambia son los dígitos: azul de madrugada, rosa al amanecer, celeste al mediodía,
 * naranja al atardecer y violeta al anochecer.
 *
 * Entre un momento y el siguiente el color se mezcla de a poco, así que a las 6:45 ya se ve
 * el amanecer empezado. Es lógica pura: se prueba sin teléfono.
 */
object SolarPalette {

    /** Momentos del día: minuto desde las 00:00, color de arriba, color de abajo y nombre. */
    private val keyframes = listOf(
        Key(0, 0xFF3A5A9B, 0xFF1B2A4A, "Madrugada"),
        Key(5 * 60 + 30, 0xFF9A6BB8, 0xFFE0788F, "Amanecer"),
        Key(7 * 60 + 30, 0xFFF0A868, 0xFFE8707A, "Mañana"),
        Key(12 * 60, 0xFF9ADCFF, 0xFF4F96DE, "Mediodía"),
        Key(13 * 60 + 30, 0xFFC8D8F0, 0xFF7FA6D6, "Tarde"),
        Key(18 * 60, 0xFFFFC46B, 0xFFE8803F, "Tardecita"),
        Key(19 * 60 + 30, 0xFFF08A4B, 0xFFC04A64, "Atardecer"),
        Key(21 * 60, 0xFF8A62B5, 0xFF4A3C84, "Anochecer"),
        Key(24 * 60, 0xFF3A5A9B, 0xFF1B2A4A, "Madrugada"),
    )

    fun at(minutesOfDay: Int): SolarLook {
        val minutes = minutesOfDay.coerceIn(0, 24 * 60)
        val next = keyframes.indexOfFirst { it.minutes >= minutes }.coerceAtLeast(1)
        val from = keyframes[next - 1]
        val to = keyframes[next]
        val span = (to.minutes - from.minutes).coerceAtLeast(1)
        val t = (minutes - from.minutes).toFloat() / span
        return SolarLook(
            top = mix(from.top, to.top, t),
            bottom = mix(from.bottom, to.bottom, t),
            // El nombre no se mezcla: cambia recién cuando se llega al momento siguiente.
            label = from.label,
        )
    }

    /** Mezcla dos colores ARGB canal por canal. */
    private fun mix(a: Long, b: Long, t: Float): Long {
        fun channel(shift: Int): Long {
            val from = (a shr shift) and 0xFF
            val to = (b shr shift) and 0xFF
            return (from + (to - from) * t).toLong().coerceIn(0, 255)
        }
        return (0xFFL shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }

    private data class Key(val minutes: Int, val top: Long, val bottom: Long, val label: String)
}
