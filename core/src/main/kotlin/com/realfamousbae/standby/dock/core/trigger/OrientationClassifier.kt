package com.realfamousbae.standby.dock.core.trigger

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sqrt

/**
 * El vector de la gravedad en los ejes del teléfono (m/s²), tal como lo da el acelerómetro.
 * El eje X va por el lado corto de la pantalla, el Y por el largo y el Z sale de la pantalla.
 */
data class Gravity(val x: Float, val y: Float, val z: Float) {
    val magnitude: Float get() = sqrt(x * x + y * y + z * z)
}

/**
 * Clasificación instantánea (sin rebote) de la posición del teléfono.
 *
 * El teléfono "está de costado" cuando el eje X casi coincide con la vertical: ahí se apoya
 * sobre el lado largo. La desviación respecto de la vertical es el ángulo entre el eje X y el
 * vector de la gravedad. Acostado boca arriba (gx ≈ 0) y en vertical (gy ≈ 9,8) la desviación
 * da cerca de 90°, así que no hacen falta chequeos aparte. El rebote y la histéresis son
 * cosa del TriggerEngine.
 */
object OrientationClassifier {

    /** Con un módulo más chico que esto, el sensor dio basura o el teléfono va en caída libre. */
    private const val MIN_VALID_MAGNITUDE = 3f

    /**
     * Ángulo entre el eje X y la vertical, en grados (0..90), o null si la medición no sirve
     * (por ejemplo, el teléfono se está sacudiendo o cayendo).
     */
    fun tiltFromVerticalDegrees(g: Gravity): Float? {
        val m = g.magnitude
        if (m < MIN_VALID_MAGNITUDE) return null
        val cos = (abs(g.x) / m).coerceIn(0f, 1f)
        return Math.toDegrees(acos(cos).toDouble()).toFloat()
    }

    /** Si el teléfono está de costado para la desviación máxima que se le permita. */
    fun isOnSide(g: Gravity, maxTiltDegrees: Float): Boolean {
        val tilt = tiltFromVerticalDegrees(g) ?: return false
        return tilt < maxTiltDegrees
    }
}
