package com.realfamousbae.standby.dock.core.prefs

import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenLayoutTest {

    @Test
    fun `sin tarjeta queda solo el reloj`() {
        assertEquals(ScreenLayout.ClockOnly, ScreenLayout.resolve(ScreenLayout.ClockAndWidget, cardAvailable = false))
        assertEquals(ScreenLayout.ClockAndWidget, ScreenLayout.resolve(ScreenLayout.ClockAndWidget, cardAvailable = true))
        assertEquals(ScreenLayout.ClockOnly, ScreenLayout.resolve(ScreenLayout.ClockOnly, cardAvailable = true))
    }

    @Test
    fun `lectura del valor guardado`() {
        assertEquals(ScreenLayout.ClockAndWidget, ScreenLayout.parse("ClockAndWidget"))
        assertEquals(ScreenLayout.ClockOnly, ScreenLayout.parse("basura"))
        assertEquals(ScreenLayout.ClockOnly, ScreenLayout.parse(null))
    }
}
