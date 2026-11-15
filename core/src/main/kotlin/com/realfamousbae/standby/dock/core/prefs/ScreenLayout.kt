package com.realfamousbae.standby.dock.core.prefs

/** Distribución de la pantalla del modo. */
enum class ScreenLayout {
    /** Solo reloj, a toda la pantalla. */
    ClockOnly,

    /** Dos tarjetas iguales: reloj a la izquierda, tarjeta (clima, agenda…) a la derecha. */
    ClockAndWidget;

    companion object {
        /**
         * Qué mostrar de verdad: si no hay con qué llenar la segunda tarjeta, queda
         * solo el reloj.
         */
        fun resolve(layout: ScreenLayout, cardAvailable: Boolean): ScreenLayout =
            if (layout == ClockAndWidget && !cardAvailable) ClockOnly else layout

        /** Lectura del valor guardado; si no se entiende, el de por defecto. */
        fun parse(name: String?): ScreenLayout = entries.firstOrNull { it.name == name } ?: ClockOnly
    }
}
