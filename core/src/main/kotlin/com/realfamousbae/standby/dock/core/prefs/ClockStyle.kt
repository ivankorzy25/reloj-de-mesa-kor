package com.realfamousbae.standby.dock.core.prefs

/**
 * Caras del reloj, en el orden en que se pasan con el dedo (de izquierda a derecha).
 * El orden del enum es el orden de la pantalla: no cambiarlo sin querer.
 */
enum class ClockFace {
    /** Dígitos grandes a todo el ancho. */
    Digital,

    /** Analógico minimalista: agujas y marcas, sin números. */
    Analogico,

    /** Flip clock: cada dígito gira como el cartel de un aeropuerto viejo. */
    Flip,

    /** Carrusel de fotos de Immich con la hora chica en una esquina. */
    Fotos;

    companion object {
        fun parse(name: String?): ClockFace = entries.firstOrNull { it.name == name } ?: Digital

        /** Cara guardada por índice de página (lo que usa el carrusel). */
        fun byIndex(index: Int): ClockFace = entries.getOrElse(index) { Digital }
    }
}

/** Fuentes para la hora. El nombre es el que se ve en Ajustes. */
enum class ClockFont(val label: String) {
    CondensadaFina("Condensada fina"),
    Redonda("Redonda gruesa"),
    Geometrica("Geométrica"),
    Mono("Monoespaciada");

    companion object {
        fun parse(name: String?): ClockFont = entries.firstOrNull { it.name == name } ?: CondensadaFina
    }
}

/** Formato de hora: lo que diga el sistema, o forzado. */
enum class HourFormat(val label: String) {
    Sistema("Como el sistema"),
    H24("24 horas"),
    H12("12 horas");

    companion object {
        fun parse(name: String?): HourFormat = entries.firstOrNull { it.name == name } ?: Sistema

        /** @param systemIs24h `DateFormat.is24HourFormat`, que solo conoce la capa :app. */
        fun is24h(format: HourFormat, systemIs24h: Boolean): Boolean = when (format) {
            Sistema -> systemIs24h
            H24 -> true
            H12 -> false
        }
    }
}

/** Colores que se ofrecen para la hora (ARGB). Gris claro es el de siempre. */
object ClockColors {
    const val GRIS = 0xFFB8B8B8L
    const val BLANCO = 0xFFF2F2F2L
    const val AMBAR = 0xFFE8B059L
    const val DURAZNO = 0xFFE98C6AL
    const val VERDE = 0xFF7FC08AL
    const val CELESTE = 0xFF7FB2D9L
    const val LILA = 0xFFB69AD6L
    const val ROJO_NOCHE = 0xFFD2563CL

    /** Paleta en el orden en que se muestra, con el nombre que lee Iván. */
    val palette: List<Pair<String, Long>> = listOf(
        "Gris" to GRIS,
        "Blanco" to BLANCO,
        "Ámbar" to AMBAR,
        "Durazno" to DURAZNO,
        "Verde" to VERDE,
        "Celeste" to CELESTE,
        "Lila" to LILA,
        "Rojo" to ROJO_NOCHE,
    )
}
