package com.realfamousbae.standby.dock.core.weather

/** Dibujo que le corresponde al estado del tiempo. */
enum class WeatherIcon {
    Sol,
    Luna,
    SolNubes,
    LunaNubes,
    Nubes,
    Lluvia,
    Chaparron,
    Nieve,
    Tormenta,
    Niebla,
}

/**
 * Códigos WMO que devuelve Open-Meteo, traducidos a algo que se entienda y a un dibujo.
 * Tabla: https://open-meteo.com/en/docs (weather_code).
 */
object WeatherCodes {

    fun description(code: Int): String = when (code) {
        0 -> "Despejado"
        1 -> "Casi despejado"
        2 -> "Parcialmente nublado"
        3 -> "Nublado"
        45, 48 -> "Niebla"
        51, 53, 55 -> "Llovizna"
        56, 57 -> "Llovizna helada"
        61 -> "Lluvia floja"
        63 -> "Lluvia"
        65 -> "Lluvia fuerte"
        66, 67 -> "Lluvia helada"
        71 -> "Nevada floja"
        73 -> "Nevada"
        75 -> "Nevada fuerte"
        77 -> "Nieve en granos"
        80 -> "Chaparrones"
        81 -> "Chaparrones fuertes"
        82 -> "Chaparrones muy fuertes"
        85, 86 -> "Chaparrones de nieve"
        95 -> "Tormenta"
        96, 99 -> "Tormenta con granizo"
        else -> "Sin datos"
    }

    fun icon(code: Int, isDay: Boolean): WeatherIcon = when (code) {
        0, 1 -> if (isDay) WeatherIcon.Sol else WeatherIcon.Luna
        2 -> if (isDay) WeatherIcon.SolNubes else WeatherIcon.LunaNubes
        3 -> WeatherIcon.Nubes
        45, 48 -> WeatherIcon.Niebla
        51, 53, 55, 56, 57, 61, 63, 65, 66, 67 -> WeatherIcon.Lluvia
        71, 73, 75, 77, 85, 86 -> WeatherIcon.Nieve
        80, 81, 82 -> WeatherIcon.Chaparron
        95, 96, 99 -> WeatherIcon.Tormenta
        else -> WeatherIcon.Nubes
    }
}
