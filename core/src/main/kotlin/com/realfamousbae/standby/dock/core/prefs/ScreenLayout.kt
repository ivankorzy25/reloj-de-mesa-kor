package com.realfamousbae.standby.dock.core.prefs

/** Distribución de la pantalla del modo. */
enum class ScreenLayout(val label: String) {
    /** Solo reloj, a toda la pantalla. */
    ClockOnly("Solo reloj"),

    /** Dos tarjetas iguales: reloj a la izquierda, tarjeta (clima, agenda…) a la derecha. */
    ClockAndWidget("Reloj + tarjeta"),

    /**
     * Dos columnas de tarjetas, como la vista "Widgets" del StandBy del iPhone: cada columna
     * se pasa con el dedo de arriba a abajo por su cuenta. El reloj es una tarjeta más.
     */
    Widgets("Dos columnas");

    companion object {
        /**
         * Qué mostrar de verdad: si no hay con qué llenar la segunda tarjeta, queda
         * solo el reloj.
         */
        fun resolve(layout: ScreenLayout, cardAvailable: Boolean): ScreenLayout =
            if (layout != ClockOnly && !cardAvailable) ClockOnly else layout

        /** Lectura del valor guardado; si no se entiende, el de por defecto. */
        fun parse(name: String?): ScreenLayout = entries.firstOrNull { it.name == name } ?: ClockOnly
    }
}
