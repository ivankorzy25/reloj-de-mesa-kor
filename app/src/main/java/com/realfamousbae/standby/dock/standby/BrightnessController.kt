package com.realfamousbae.standby.dock.standby

import android.os.SystemClock
import android.view.Window
import com.realfamousbae.standby.dock.core.brightness.AutoBrightness
import com.realfamousbae.standby.dock.core.prefs.StandbySettings
import com.realfamousbae.standby.dock.sensors.LightSensor
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Aplica el brillo automático a la ventana del modo. La suavidad la pone [AutoBrightness]
 * (:core); acá están el sensor, el reloj y la escritura en `window.attributes`.
 *
 * Lo que lee el sensor también sale por [lux]: el modo noche lo usa para darse cuenta de
 * que el cuarto está a oscuras.
 */
class BrightnessController(
    private val window: Window,
    private val lightSensor: LightSensor,
    private val settings: StateFlow<StandbySettings>,
    /** De noche la pantalla no pasa de este brillo, aunque el sensor pida más. */
    private val night: StateFlow<Boolean>,
    /** Atenuada ("siempre encendido"): el brillo más bajo, pase lo que pase. */
    private val dimmed: StateFlow<Boolean>,
) {
    private val auto = AutoBrightness()

    private val _lux = MutableStateFlow<Float?>(null)
    val lux: StateFlow<Float?> = _lux.asStateFlow()

    /** Funciona mientras viva la corutina (la Activity la lanza mientras está STARTED). */
    suspend fun run() = coroutineScope {
        if (!lightSensor.isAvailable) {
            apply(settings.value.brightnessMin)
            return@coroutineScope
        }
        launch { lightSensor.lux().collect { _lux.value = it } }
            // El sensor se queda callado mientras la luz no cambia: por eso ajustamos por reloj.
        while (true) {
            if (dimmed.value) {
                apply(settings.value.brightnessMin)
            } else {
                _lux.value?.let { lux ->
                    val s = settings.value
                    val max = if (night.value) minOf(s.brightnessMax, NIGHT_MAX_BRIGHTNESS) else s.brightnessMax
                    auto.update(lux, SystemClock.elapsedRealtime(), s.brightnessMin, max)?.let(::apply)
                }
            }
            delay(TICK_MS)
        }
    }

    private fun apply(brightness: Float) {
        window.attributes = window.attributes.apply { screenBrightness = brightness }
    }

    private companion object {
        const val TICK_MS = 200L

        /** Tope de brillo con el modo noche prendido (parte del máximo de la pantalla). */
        const val NIGHT_MAX_BRIGHTNESS = 0.25f
    }
}
