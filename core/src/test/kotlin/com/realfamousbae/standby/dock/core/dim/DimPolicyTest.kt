package com.realfamousbae.standby.dock.core.dim

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DimPolicyTest {

    private fun dim(
        enabled: Boolean = true,
        charging: Boolean = true,
        live: Boolean = false,
        idleMs: Long = 10 * 60_000L,
        minutes: Int = 5,
    ) = DimPolicy.shouldDim(enabled, charging, live, idleMs, minutes)

    @Test
    fun `se atenúa recién cuando pasó el tiempo`() {
        assertFalse(dim(idleMs = 4 * 60_000L))
        assertTrue(dim(idleMs = 5 * 60_000L))
    }

    @Test
    fun `sin cargador no se atenúa, se apaga como siempre`() {
        assertFalse(dim(charging = false))
    }

    @Test
    fun `apagado en ajustes, o con algo pasando, no se atenúa`() {
        assertFalse(dim(enabled = false))
        assertFalse(dim(live = true))
    }

    @Test
    fun `el tiempo elegido se mantiene dentro de lo razonable`() {
        // Un valor absurdo no deja la pantalla prendida para siempre ni la apaga al toque.
        assertTrue(dim(idleMs = DimPolicy.MAX_MINUTES * 60_000L, minutes = 9_999))
        assertFalse(dim(idleMs = 30_000L, minutes = 0))
        assertTrue(dim(idleMs = 60_000L, minutes = 0))
    }

    @Test
    fun `el reloj chiquito se va corriendo y tarda 35 minutos en repetirse`() {
        val lugares = (0 until 35).map { DimPolicy.burnInSpot(it) }
        assertEquals(35, lugares.toSet().size)
        assertEquals(lugares[0], DimPolicy.burnInSpot(35))
        assertEquals(lugares[3], DimPolicy.burnInSpot(35 + 3))
        // Siempre dentro de la pantalla.
        assertTrue(lugares.all { (x, y) -> x in 0f..1f && y in 0f..1f })
    }
}
