package com.realfamousbae.standby.dock.core.place

import com.realfamousbae.standby.dock.core.prefs.ClockFace
import com.realfamousbae.standby.dock.core.prefs.ScreenLayout
import com.realfamousbae.standby.dock.core.prefs.SideCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaceMemoryTest {

    private val mesaDeLuz = PlacePrefs(
        face = ClockFace.Flip,
        card = SideCard.AlarmaGrande,
        card2 = SideCard.Clima,
        layout = ScreenLayout.Widgets,
    )
    private val escritorio = PlacePrefs(
        face = ClockFace.Digital,
        card = SideCard.Calendario,
        card2 = SideCard.Musica,
        layout = ScreenLayout.ClockAndWidget,
    )

    @Test
    fun `el nombre del lugar distingue cargadores`() {
        assertEquals("Inalambrico-5V", PlaceMemory.key(PlaceMemory.Charger.Inalambrico, 5))
        assertEquals("Pared-9V", PlaceMemory.key(PlaceMemory.Charger.Pared, 9))
        // Sin voltaje informado, el lugar es solo el tipo de cargador.
        assertEquals("Usb", PlaceMemory.key(PlaceMemory.Charger.Usb, null))
        assertEquals("Usb", PlaceMemory.key(PlaceMemory.Charger.Usb, 0))
    }

    @Test
    fun `ida y vuelta de lo guardado`() {
        val places = mapOf("Inalambrico-5V" to mesaDeLuz, "Pared-9V" to escritorio)
        assertEquals(places, PlaceMemory.decode(PlaceMemory.encode(places)))
    }

    @Test
    fun `lo que no se entiende se descarta sin romper nada`() {
        assertEquals(emptyMap<String, PlacePrefs>(), PlaceMemory.decode(null))
        assertEquals(emptyMap<String, PlacePrefs>(), PlaceMemory.decode(""))
        assertEquals(emptyMap<String, PlacePrefs>(), PlaceMemory.decode("basura;otra=cosa"))
        // Una entrada rota no se lleva puestas a las buenas.
        val mezcla = PlaceMemory.decode("rota;Usb=Flip,AlarmaGrande,Clima,Widgets")
        assertEquals(1, mezcla.size)
        assertEquals(mesaDeLuz, mezcla["Usb"])
    }

    @Test
    fun `guardar pisa el lugar y no guarda más de ocho`() {
        val uno = PlaceMemory.remember(emptyMap(), "Usb", mesaDeLuz)
        assertEquals(mesaDeLuz, uno["Usb"])
        assertEquals(escritorio, PlaceMemory.remember(uno, "Usb", escritorio)["Usb"])

        var muchos = emptyMap<String, PlacePrefs>()
        repeat(PlaceMemory.MAX_PLACES + 3) { i ->
            muchos = PlaceMemory.remember(muchos, "lugar$i", mesaDeLuz)
        }
        assertEquals(PlaceMemory.MAX_PLACES, muchos.size)
        // El más viejo se cayó; el último está.
        assertTrue("lugar0" !in muchos)
        assertTrue("lugar${PlaceMemory.MAX_PLACES + 2}" in muchos)
    }
}
