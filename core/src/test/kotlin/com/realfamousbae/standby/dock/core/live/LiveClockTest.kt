package com.realfamousbae.standby.dock.core.live

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveClockTest {

    @Test
    fun `minutos y segundos, y horas solo si las hay`() {
        assertEquals("0:00", LiveClock.format(0))
        assertEquals("0:01", LiveClock.format(1))
        assertEquals("9:58", LiveClock.format(9 * 60_000L + 58_000L))
        assertEquals("1:02:30", LiveClock.format(3_750_000L))
        assertEquals("10:00:00", LiveClock.format(36_000_000L))
    }

    @Test
    fun `los segundos se redondean para arriba, como el teléfono`() {
        // A falta de 9,5 s el teléfono muestra 10, no 9: el número cambia recién al llegar.
        assertEquals("0:10", LiveClock.format(9_500L))
        assertEquals("0:09", LiveClock.format(9_000L))
    }

    @Test
    fun `para atrás y para adelante`() {
        assertEquals(30_000L, LiveClock.elapsed(baseMs = 100_000L, nowMs = 70_000L, countDown = true))
        assertEquals(30_000L, LiveClock.elapsed(baseMs = 70_000L, nowMs = 100_000L, countDown = false))
        // Nunca en negativo.
        assertEquals(0L, LiveClock.elapsed(baseMs = 100_000L, nowMs = 200_000L, countDown = true))
        assertEquals(0L, LiveClock.elapsed(baseMs = 200_000L, nowMs = 100_000L, countDown = false))
    }

    @Test
    fun `el temporizador que llegó a cero se marca`() {
        assertTrue(LiveClock.finished(baseMs = 100L, nowMs = 100L, countDown = true))
        assertFalse(LiveClock.finished(baseMs = 100L, nowMs = 99L, countDown = true))
        assertFalse(LiveClock.finished(baseMs = 100L, nowMs = 9_999L, countDown = false))
    }
}
