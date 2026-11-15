package com.realfamousbae.standby.dock.core.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SolarPaletteTest {

    @Test
    fun `cada momento tiene su nombre`() {
        assertEquals("Madrugada", SolarPalette.at(2 * 60).label)
        assertEquals("Amanecer", SolarPalette.at(6 * 60).label)
        assertEquals("Mediodía", SolarPalette.at(12 * 60 + 30).label)
        assertEquals("Tarde", SolarPalette.at(15 * 60).label)
        assertEquals("Atardecer", SolarPalette.at(20 * 60).label)
        assertEquals("Anochecer", SolarPalette.at(22 * 60).label)
    }

    @Test
    fun `el dia da la vuelta entero sin saltos`() {
        // Un minuto antes de medianoche y un minuto después tienen que parecerse.
        val tarde = SolarPalette.at(24 * 60 - 1)
        val temprano = SolarPalette.at(0)
        assertTrue(cerca(tarde.top, temprano.top))
        assertTrue(cerca(tarde.bottom, temprano.bottom))
    }

    @Test
    fun `el color cambia a lo largo del dia`() {
        assertNotEquals(SolarPalette.at(3 * 60).top, SolarPalette.at(12 * 60).top)
        assertNotEquals(SolarPalette.at(12 * 60).top, SolarPalette.at(20 * 60).top)
    }

    @Test
    fun `siempre sale un color opaco y los bordes no se van de rango`() {
        for (minute in 0..24 * 60) {
            val look = SolarPalette.at(minute)
            assertEquals(0xFFL, (look.top shr 24) and 0xFF)
            assertEquals(0xFFL, (look.bottom shr 24) and 0xFF)
        }
        assertEquals(SolarPalette.at(0), SolarPalette.at(-100))
        assertEquals(SolarPalette.at(24 * 60), SolarPalette.at(99_999))
    }

    private fun cerca(a: Long, b: Long): Boolean =
        (0..2).all { i ->
            val shift = i * 8
            kotlin.math.abs(((a shr shift) and 0xFF) - ((b shr shift) and 0xFF)) <= 2
        }
}
