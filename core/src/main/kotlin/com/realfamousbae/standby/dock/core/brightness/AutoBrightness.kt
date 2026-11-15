package com.realfamousbae.standby.dock.core.brightness

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min

/**
 * Curva "luz del ambiente → brillo de la ventana". El ojo percibe la luz de forma
 * logarítmica, así que el brillo sube lineal con log10(lux): 1 lux o menos → [min],
 * [LUX_FULL] o más → [max].
 */
object BrightnessCurve {
    const val LUX_DARK = 1f
    const val LUX_FULL = 1_000f

    fun map(lux: Float, min: Float, max: Float): Float {
        if (lux <= LUX_DARK) return min
        if (lux >= LUX_FULL) return max
        val t = log10(lux / LUX_DARK) / log10(LUX_FULL / LUX_DARK)
        return min + (max - min) * t
    }
}

/**
 * Brillo automático con suavidad: lo que lee el sensor se promedia de forma exponencial (en
 * escala logarítmica, para que el reflejo de unas luces por la ventana no haga saltar la
 * pantalla) y el resultado no cambia más rápido que [maxStepPerSecond] ni más seguido que lo
 * que marca la zona muerta.
 *
 * El sensor de luz de Android avisa solo cuando el valor cambia, así que quien lo use tiene
 * que llamar a [update] con un reloj y el último valor conocido; si no, el promedio queda
 * clavado a mitad de camino.
 */
class AutoBrightness(
    private val timeConstantMs: Long = 3_000,
    private val maxStepPerSecond: Float = 0.25f,
    private val deadBand: Float = 0.005f,
) {
    private var smoothedLogLux: Float? = null
    private var output: Float? = null
    private var lastUpdateMs: Long = 0

    /**
     * @return el brillo nuevo si conviene aplicarlo a la ventana; si no, null.
     */
    fun update(lux: Float, nowMs: Long, minBrightness: Float, maxBrightness: Float): Float? {
        val logLux = log10(max(lux, 0f) + 1f)
        val prevLog = smoothedLogLux
        val prevOut = output
        if (prevLog == null || prevOut == null) {
            // Primera lectura: directo al brillo que toca, sin animación.
            smoothedLogLux = logLux
            lastUpdateMs = nowMs
            return BrightnessCurve.map(lux, minBrightness, maxBrightness).also { output = it }
        }

        val dt = (nowMs - lastUpdateMs).coerceAtLeast(0)
        lastUpdateMs = nowMs
        val alpha = 1f - exp(-dt.toFloat() / timeConstantMs)
        val smoothed = prevLog + (logLux - prevLog) * alpha
        smoothedLogLux = smoothed

        val smoothedLux = Math.pow(10.0, smoothed.toDouble()).toFloat() - 1f
        val target = BrightnessCurve.map(smoothedLux, minBrightness, maxBrightness)
        val maxStep = maxStepPerSecond * dt / 1_000f
        val next = prevOut + (target - prevOut).coerceIn(-maxStep, maxStep)
        // Los límites pueden haber cambiado en Ajustes: mantenemos el resultado adentro.
        val clamped = min(max(next, minBrightness), maxBrightness)
        if (abs(clamped - prevOut) < deadBand && clamped != target) return null
        if (clamped == prevOut) return null
        output = clamped
        return clamped
    }
}
