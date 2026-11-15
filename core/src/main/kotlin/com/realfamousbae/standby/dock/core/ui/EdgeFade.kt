package com.realfamousbae.standby.dock.core.ui

import kotlin.math.abs
import kotlin.math.exp

/**
 * Perfil del desvanecido en el borde de la tarjeta: cuánto tapa el "velo" del color de la
 * tarjeta por encima del widget. La forma es la integral de una gaussiana (como un borde
 * desenfocado): no tiene quiebres en las puntas, así que el ojo no ve dónde empieza ni dónde
 * termina. Un degradado lineal deja bandas visibles justo en los quiebres.
 */
object EdgeFade {

    /** Cuántos pasos del degradado: con 16 la curva no se distingue de una continua. */
    const val STOPS = 16

    /** Pendiente: en los bordes el perfil se aparta de 0/1 menos de 1/255 ya normalizado. */
    private const val SHARPNESS = 5.0

    /**
     * @param t posición dentro de la franja: 0 en el borde de la tarjeta, 1 en el borde interno.
     * @return cuánto tapa el velo: 1 en el borde, 0 en el borde interno.
     */
    fun veilAlpha(t: Float): Float {
        val x = t.coerceIn(0f, 1f).toDouble()
        val raw = { v: Double -> 1.0 - gaussCdf((v - 0.5) * SHARPNESS) }
        val lo = raw(1.0)
        val hi = raw(0.0)
        return ((raw(x) - lo) / (hi - lo)).toFloat()
    }

    /** Posiciones de los pasos (0..1) y cuánto tapa el velo en cada uno. */
    fun stops(): List<Pair<Float, Float>> =
        (0 until STOPS).map { i -> val t = i / (STOPS - 1f); t to veilAlpha(t) }

    private fun gaussCdf(z: Double): Double = 0.5 * (1.0 + erf(z / Math.sqrt(2.0)))

    /** Aproximación de Abramowitz y Stegun 7.1.26, con error menor a 1,5e-7. */
    private fun erf(x: Double): Double {
        val t = 1.0 / (1.0 + 0.3275911 * abs(x))
        val poly = t * (0.254829592 + t * (-0.284496736 + t * (1.421413741 + t * (-1.453152027 + t * 1.061405429))))
        val y = 1.0 - poly * exp(-x * x)
        return if (x >= 0) y else -y
    }
}
