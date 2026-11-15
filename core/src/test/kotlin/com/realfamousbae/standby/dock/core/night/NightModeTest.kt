package com.realfamousbae.standby.dock.core.night

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NightModeTest {

    private val start = 22 * 60
    private val end = 7 * 60

    @Test
    fun `el horario puede cruzar la medianoche`() {
        assertTrue(NightMode.inSchedule(23 * 60, start, end))
        assertTrue(NightMode.inSchedule(2 * 60, start, end))
        assertTrue(NightMode.inSchedule(start, start, end))
        assertFalse(NightMode.inSchedule(end, start, end))
        assertFalse(NightMode.inSchedule(15 * 60, start, end))
    }

    @Test
    fun `horario de inicio igual al de fin es siempre`() {
        assertTrue(NightMode.inSchedule(0, 60, 60))
        assertTrue(NightMode.inSchedule(13 * 60, 60, 60))
    }

    @Test
    fun `fuera del horario lo prende la oscuridad, si está permitido`() {
        fun activo(lux: Float?, sensor: Boolean) =
            NightMode.isActive(true, 15 * 60, start, end, sensor, lux)

        assertTrue(activo(0.5f, sensor = true))
        assertFalse(activo(50f, sensor = true))
        assertFalse(activo(0.5f, sensor = false))
        assertFalse(activo(null, sensor = true))
    }

    @Test
    fun `apagado es apagado`() {
        assertFalse(NightMode.isActive(false, 23 * 60, start, end, true, 0f))
    }
}
