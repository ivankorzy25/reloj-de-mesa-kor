package com.realfamousbae.standby.dock.core.brightness

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoBrightnessTest {

    private val min = 0.01f
    private val max = 0.6f

    @Test
    fun `la curva — a oscuras el mínimo, con mucha luz el máximo, y el medio logarítmico`() {
        assertEquals(min, BrightnessCurve.map(0f, min, max), 1e-6f)
        assertEquals(min, BrightnessCurve.map(1f, min, max), 1e-6f)
        assertEquals(max, BrightnessCurve.map(5_000f, min, max), 1e-6f)
        // 31,6 lux es justo el medio de la escala log10 entre 1 y 1000.
        assertEquals((min + max) / 2, BrightnessCurve.map(31.62f, min, max), 0.002f)
    }

    @Test
    fun `la primera lectura se aplica de una`() {
        val auto = AutoBrightness()
        assertEquals(max, auto.update(2_000f, 0, min, max)!!, 1e-6f)
    }

    @Test
    fun `si se apaga la luz, el brillo baja suave hasta el mínimo`() {
        val auto = AutoBrightness()
        auto.update(500f, 0, min, max)
        val first = auto.update(0f, 200, min, max)!!
        assertTrue("en 200 ms no más de 0.05", first > 0.45f)

        var last = first
        var t = 200L
        while (t < 30_000) {
            t += 200
            auto.update(0f, t, min, max)?.let {
                assertTrue("el brillo solo baja", it <= last)
                last = it
            }
        }
        assertEquals(min, last, 0.01f)
    }

    @Test
    fun `un destello corto de luz casi no cambia el brillo`() {
        val auto = AutoBrightness()
        auto.update(0f, 0, min, max)
        val afterFlash = auto.update(2_000f, 300, min, max) ?: min
        assertTrue("destello 300 ms — menos de 0.1", afterFlash < 0.1f)
    }

    @Test
    fun `con luz estable no hay actualizaciones de más`() {
        val auto = AutoBrightness()
        auto.update(50f, 0, min, max)
        var t = 0L
        repeat(50) {
            t += 200
            assertNull(auto.update(50f, t, min, max))
        }
    }

    @Test
    fun `si se baja el máximo en Ajustes, el valor vuelve adentro de los límites`() {
        val auto = AutoBrightness()
        auto.update(2_000f, 0, min, max)
        val next = auto.update(2_000f, 200, min, 0.3f)!!
        assertTrue(next <= 0.3f)
    }
}
