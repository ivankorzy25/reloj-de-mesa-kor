package com.realfamousbae.standby.dock.core.prefs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClockStyleTest {

    @Test
    fun `el orden de las caras es el del carrusel`() {
        assertEquals(ClockFace.Digital, ClockFace.byIndex(0))
        assertEquals(ClockFace.Fotos, ClockFace.byIndex(3))
        // Índice fuera de rango (cara sacada en una versión futura): la de siempre.
        assertEquals(ClockFace.Digital, ClockFace.byIndex(99))
    }

    @Test
    fun `valores guardados que no se entienden caen en el de por defecto`() {
        assertEquals(ClockFace.Flip, ClockFace.parse("Flip"))
        assertEquals(ClockFace.Digital, ClockFace.parse("Otra"))
        assertEquals(ClockFont.CondensadaFina, ClockFont.parse(null))
        assertEquals(HourFormat.Sistema, HourFormat.parse("nada"))
        assertEquals(SideCard.Clima, SideCard.parse(null))
    }

    @Test
    fun `el formato de hora respeta el sistema solo en Sistema`() {
        assertTrue(HourFormat.is24h(HourFormat.Sistema, systemIs24h = true))
        assertEquals(false, HourFormat.is24h(HourFormat.Sistema, systemIs24h = false))
        assertTrue(HourFormat.is24h(HourFormat.H24, systemIs24h = false))
        assertEquals(false, HourFormat.is24h(HourFormat.H12, systemIs24h = true))
    }

    @Test
    fun `la tarjeta de widget solo aparece si hay widget`() {
        val todas = SideCard.entries.size
        assertEquals(todas - 1, SideCard.available(widgetAvailable = false).size)
        assertEquals(todas, SideCard.available(widgetAvailable = true).size)
        assertEquals(0, SideCard.pageOf(SideCard.Reloj, widgetAvailable = false))
        assertEquals(todas - 1, SideCard.pageOf(SideCard.Widget, widgetAvailable = true))
        // Sin widget, la página guardada no existe: vuelve a la primera.
        assertEquals(0, SideCard.pageOf(SideCard.Widget, widgetAvailable = false))
    }

    @Test
    fun `cada columna tiene su valor de fabrica`() {
        assertEquals(SideCard.Clima, SideCard.parse(null))
        assertEquals(SideCard.AlarmaBateria, SideCard.parseSecond(null))
        assertEquals(SideCard.Musica, SideCard.parseSecond("Musica"))
    }
}
