package com.realfamousbae.standby.dock.sensors

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.realfamousbae.standby.dock.core.trigger.Gravity
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Adaptador del acelerómetro a Flow<Gravity>. La suscripción al sensor vive exactamente lo
 * que vive quien recolecta el Flow, así el servicio no se puede olvidar de desuscribirse.
 *
 * Preferimos la variante wake-up: despierta al procesador, así que los eventos llegan
 * también con la pantalla apagada. Si no existe, quien llame tiene que sostener un PARTIAL_WAKE_LOCK.
 */
class GravitySensor(private val sensorManager: SensorManager) {

    private val wakeUpSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER, true)
    private val sensor: Sensor? =
        wakeUpSensor ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    val isAvailable: Boolean get() = sensor != null
    val isWakeUp: Boolean get() = wakeUpSensor != null
    val description: String
        get() = sensor?.let { "${it.name} (${it.vendor}), wakeUp=${it.isWakeUpSensor}" } ?: "no"

    fun gravity(): Flow<Gravity> = callbackFlow {
        val s = sensor ?: run {
            close(IllegalStateException("El equipo no tiene acelerómetro"))
            return@callbackFlow
        }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                trySend(Gravity(event.values[0], event.values[1], event.values[2]))
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
        }
        // ~5 Hz, aceptando entrega en lotes con hasta 1 s de atraso: ahorra despertadas.
        sensorManager.registerListener(
            listener, s, SensorManager.SENSOR_DELAY_NORMAL, MAX_REPORT_LATENCY_US,
        )
        awaitClose { sensorManager.unregisterListener(listener) }
    }

    private companion object {
        const val MAX_REPORT_LATENCY_US = 1_000_000
    }
}
