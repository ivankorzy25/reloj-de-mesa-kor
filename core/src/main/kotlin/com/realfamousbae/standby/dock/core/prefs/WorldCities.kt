package com.realfamousbae.standby.dock.core.prefs

/** Una ciudad del reloj mundial: cómo se llama y en qué huso horario está. */
data class WorldCity(val name: String, val zoneId: String)

/**
 * Las ciudades que se ofrecen para la cara "Reloj mundial" y cómo se guardan en los ajustes
 * (una sola línea de texto, para no agregar una base de datos por tres nombres).
 */
object WorldCities {

    /** No entran más de tres en la pantalla sin que queden ilegibles. */
    const val MAX = 3

    /** Las que se pueden elegir, pensadas para la familia y los amigos de Iván. */
    val catalog: List<WorldCity> = listOf(
        WorldCity("Buenos Aires", "America/Argentina/Buenos_Aires"),
        WorldCity("Madrid", "Europe/Madrid"),
        WorldCity("Miami", "America/New_York"),
        WorldCity("Nueva York", "America/New_York"),
        WorldCity("Los Ángeles", "America/Los_Angeles"),
        WorldCity("Ciudad de México", "America/Mexico_City"),
        WorldCity("San Pablo", "America/Sao_Paulo"),
        WorldCity("Santiago", "America/Santiago"),
        WorldCity("Londres", "Europe/London"),
        WorldCity("París", "Europe/Paris"),
        WorldCity("Roma", "Europe/Rome"),
        WorldCity("Berlín", "Europe/Berlin"),
        WorldCity("Moscú", "Europe/Moscow"),
        WorldCity("Dubái", "Asia/Dubai"),
        WorldCity("Pekín", "Asia/Shanghai"),
        WorldCity("Tokio", "Asia/Tokyo"),
        WorldCity("Sídney", "Australia/Sydney"),
    )

    /** De fábrica: donde hay parientes y donde se mira el fútbol. */
    val defaults: List<WorldCity> = listOf(
        WorldCity("Madrid", "Europe/Madrid"),
        WorldCity("Miami", "America/New_York"),
        WorldCity("Tokio", "Asia/Tokyo"),
    )

    /** "Madrid|Europe/Madrid;Tokio|Asia/Tokyo" — una línea, fácil de guardar y de leer. */
    fun encode(cities: List<WorldCity>): String =
        cities.take(MAX).joinToString(";") { "${it.name}|${it.zoneId}" }

    /** Lo guardado; si está vacío o no se entiende, las de fábrica. */
    fun decode(text: String?): List<WorldCity> {
        if (text.isNullOrBlank()) return defaults
        val cities = text.split(";").mapNotNull { part ->
            val pieces = part.split("|")
            val name = pieces.getOrNull(0)?.trim().orEmpty()
            val zone = pieces.getOrNull(1)?.trim().orEmpty()
            if (name.isEmpty() || zone.isEmpty()) null else WorldCity(name, zone)
        }
        return cities.take(MAX).ifEmpty { defaults }
    }

    /** Prender o apagar una ciudad de la lista, sin pasarse de [MAX] ni quedar en cero. */
    fun toggle(cities: List<WorldCity>, city: WorldCity): List<WorldCity> = when {
        cities.any { it.name == city.name } ->
            cities.filterNot { it.name == city.name }.ifEmpty { cities }
        cities.size >= MAX -> cities.drop(1) + city
        else -> cities + city
    }
}
