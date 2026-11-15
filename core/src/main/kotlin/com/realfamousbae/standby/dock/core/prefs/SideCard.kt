package com.realfamousbae.standby.dock.core.prefs

/**
 * Tarjetas de la derecha, en el orden en que se pasan con el dedo (de arriba a abajo).
 * [Widget] solo entra en la lista si Iván eligió un widget del sistema.
 */
enum class SideCard(val label: String) {
    Clima("Clima"),
    Calendario("Calendario"),
    AlarmaBateria("Alarma y batería"),
    Musica("Reproduciendo"),
    Widget("Widget del sistema");

    companion object {
        /** Las tarjetas disponibles según si hay widget elegido. */
        fun available(widgetAvailable: Boolean): List<SideCard> =
            entries.filter { it != Widget || widgetAvailable }

        /** Índice guardado → página real (la lista cambia si se saca el widget). */
        fun pageOf(card: SideCard, widgetAvailable: Boolean): Int =
            available(widgetAvailable).indexOf(card).coerceAtLeast(0)

        fun parse(name: String?): SideCard = entries.firstOrNull { it.name == name } ?: Clima
    }
}
