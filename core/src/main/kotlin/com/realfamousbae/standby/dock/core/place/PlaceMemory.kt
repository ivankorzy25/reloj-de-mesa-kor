package com.realfamousbae.standby.dock.core.place

import com.realfamousbae.standby.dock.core.prefs.ClockFace
import com.realfamousbae.standby.dock.core.prefs.ScreenLayout
import com.realfamousbae.standby.dock.core.prefs.SideCard

/** Lo que se recuerda de cada lugar: qué cara del reloj y qué tarjetas quedaron. */
data class PlacePrefs(
    val face: ClockFace,
    val card: SideCard,
    val card2: SideCard,
    val layout: ScreenLayout,
)

/**
 * "Memoria por cargador": el reloj se acuerda de cómo lo dejaste en cada lugar. La mesa de
 * luz y el escritorio casi nunca tienen el mismo cargador, así que el lugar se reconoce por
 * **cómo está cargando** (inalámbrico, cargador de pared, USB de la compu) y por la fuerza
 * con que carga, que delata al cargador. No se pide el permiso de ubicación: leer el nombre
 * de la red wifi lo exige, y una app de reloj que pide ubicación asusta (y con razón).
 *
 * Todo esto es lógica pura: se prueba sin teléfono.
 */
object PlaceMemory {

    /** Cómo está enchufado. Los nombres son los que ve Iván en Ajustes. */
    enum class Charger(val label: String) {
        Inalambrico("Cargador inalámbrico"),
        Pared("Cargador de pared"),
        Usb("USB"),
        Ninguno("Sin cargador"),
    }

    /** Más de esto no tiene sentido guardar: son lugares, no un archivo. */
    const val MAX_PLACES = 8

    /**
     * El nombre del lugar. Los voltios redondeados distinguen dos cargadores del mismo tipo
     * (el de 9 V del escritorio del de 5 V de la mesa de luz); si el teléfono no los informa,
     * el lugar es solo el tipo de cargador.
     */
    fun key(charger: Charger, chargeVolts: Int?): String =
        if (chargeVolts == null || chargeVolts <= 0) charger.name else "${charger.name}-${chargeVolts}V"

    /** Lo que se muestra en Ajustes: "Cargador inalámbrico · 9 V". */
    fun label(charger: Charger, chargeVolts: Int?): String =
        if (chargeVolts == null || chargeVolts <= 0) charger.label else "${charger.label} · $chargeVolts V"

    fun encode(places: Map<String, PlacePrefs>): String = places.entries
        .take(MAX_PLACES)
        .joinToString(";") { (key, prefs) ->
            "$key=${prefs.face.name},${prefs.card.name},${prefs.card2.name},${prefs.layout.name}"
        }

    fun decode(text: String?): Map<String, PlacePrefs> {
        if (text.isNullOrBlank()) return emptyMap()
        return text.split(";").mapNotNull { entry ->
            val key = entry.substringBefore('=', "").trim()
            val values = entry.substringAfter('=', "").split(",")
            if (key.isEmpty() || values.size < 4) return@mapNotNull null
            key to PlacePrefs(
                face = ClockFace.parse(values[0]),
                card = SideCard.parse(values[1]),
                card2 = SideCard.parseSecond(values[2]),
                layout = ScreenLayout.parse(values[3]),
            )
        }.toMap()
    }

    /**
     * Guardar cómo quedó este lugar. Si ya hay demasiados lugares, el más viejo se cae: el
     * mapa conserva el orden en que se fueron agregando.
     */
    fun remember(
        places: Map<String, PlacePrefs>,
        key: String,
        prefs: PlacePrefs,
    ): Map<String, PlacePrefs> {
        val updated = places - key + (key to prefs)
        return if (updated.size <= MAX_PLACES) updated
        else updated.entries.drop(updated.size - MAX_PLACES).associate { it.key to it.value }
    }
}
