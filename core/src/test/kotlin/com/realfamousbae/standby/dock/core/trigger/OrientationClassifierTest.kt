package com.realfamousbae.standby.dock.core.trigger

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class OrientationClassifierTest {

    private val g = 9.81f

    /** Teléfono de costado, echado para atrás [deg] grados (como en el soporte). */
    private fun onStandTilted(deg: Double) = Gravity(
        x = (g * cos(Math.toRadians(deg))).toFloat(),
        y = 0f,
        z = (g * sin(Math.toRadians(deg))).toFloat(),
    )

    @Test
    fun `parado de costado bien derecho — desviación 0`() {
        assertEquals(0f, OrientationClassifier.tiltFromVerticalDegrees(Gravity(g, 0f, 0f))!!, 0.01f)
        assertEquals(0f, OrientationClassifier.tiltFromVerticalDegrees(Gravity(-g, 0f, 0f))!!, 0.01f)
    }

    @Test
    fun `un soporte con 30 grados de inclinación cuenta como de costado`() {
        assertTrue(OrientationClassifier.isOnSide(onStandTilted(30.0), maxTiltDegrees = 45f))
    }

    @Test
    fun `más inclinado que el umbral — no está de costado`() {
        assertFalse(OrientationClassifier.isOnSide(onStandTilted(50.0), maxTiltDegrees = 45f))
    }

    @Test
    fun `acostado boca arriba — no está de costado`() {
        assertFalse(OrientationClassifier.isOnSide(Gravity(0f, 0f, g), maxTiltDegrees = 45f))
    }

    @Test
    fun `parado en vertical — no está de costado`() {
        assertFalse(OrientationClassifier.isOnSide(Gravity(0f, g, 0f), maxTiltDegrees = 45f))
    }

    @Test
    fun `módulo demasiado chico — la medición no sirve`() {
        assertNull(OrientationClassifier.tiltFromVerticalDegrees(Gravity(0.5f, 0.5f, 0.5f)))
        assertFalse(OrientationClassifier.isOnSide(Gravity(0.5f, 0f, 0f), maxTiltDegrees = 45f))
    }
}
