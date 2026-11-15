package com.realfamousbae.standby.dock.core.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EdgeFadeTest {

    @Test
    fun `en el borde el velo tapa todo y en el borde interno no tapa nada`() {
        assertEquals(1f, EdgeFade.veilAlpha(0f), 1e-6f)
        assertEquals(0f, EdgeFade.veilAlpha(1f), 1e-6f)
        assertEquals(0.5f, EdgeFade.veilAlpha(0.5f), 1e-4f)
    }

    @Test
    fun `el perfil baja siempre y es suave en las puntas`() {
        val samples = (0..100).map { EdgeFade.veilAlpha(it / 100f) }
        samples.zipWithNext().forEach { (a, b) -> assertTrue(b <= a) }
        // Sin quiebre: el primer paso del borde es bastante menor que el del medio.
        val edgeStep = samples[0] - samples[1]
        val midStep = samples[50] - samples[51]
        assertTrue(edgeStep < midStep / 5)
    }

    @Test
    fun `los pasos cubren todo el tramo`() {
        val stops = EdgeFade.stops()
        assertEquals(EdgeFade.STOPS, stops.size)
        assertEquals(0f, stops.first().first, 0f)
        assertEquals(1f, stops.last().first, 0f)
    }
}
