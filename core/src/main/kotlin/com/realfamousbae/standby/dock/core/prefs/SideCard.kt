package com.realfamousbae.standby.dock.core.prefs

/**
 * Tarjetas, en el orden en que se pasan con el dedo (de arriba a abajo). Son las mismas para
 * la tarjeta de al lado del reloj y para las dos columnas del layout "Dos columnas".
 * [Widget] solo entra en la lista si Iván eligió un widget del sistema.
 */
enum class SideCard(val label: String) {
    Reloj("Reloj"),
    Clima("Clima"),
    Calendario("Calendario"),
    AlarmaBateria("Alarma y batería"),
    AlarmaGrande("Próxima alarma"),
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

        /** Valor guardado de la segunda columna; de fábrica, la alarma y la batería. */
        fun parseSecond(name: String?): SideCard = entries.firstOrNull { it.name == name } ?: AlarmaBateria
    }
}
