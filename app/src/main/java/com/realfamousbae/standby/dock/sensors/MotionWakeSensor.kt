package com.realfamousbae.standby.dock.sensors

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Sensor wake-up de "inclinación" (Tilt Detector, tipo 22): salta cuando al teléfono lo giran
 * bastante y despierta al procesador incluso con la pantalla apagada. Hace falta para
 * darse cuenta, sin cargador, de que pusieron el teléfono apagado en el soporte, sin tener
 * el acelerómetro prendido todo el tiempo.
 *
 * La constante del tipo está escondida en el SDK, así que buscamos el sensor por número en
 * la lista general. En teléfonos que no lo tienen, el Flow queda vacío y solo funciona el
 * camino de "se apagó la pantalla".
 */
class MotionWakeSensor(private val sensorManager: SensorManager) {

    private val sensor: Sensor? = sensorManager.getSensorList(Sensor.TYPE_ALL)
        .firstOrNull { it.type == TYPE_TILT_DETECTOR && it.isWakeUpSensor }

    val description: String get() = sensor?.let { "${it.name} (${it.vendor})" } ?: "no"

    fun events(): Flow<Unit> {
        val s = sensor ?: return emptyFlow()
        return callbackFlow {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    trySend(Unit)
                }

                override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
            }
            val ok = sensorManager.registerListener(listener, s, SensorManager.SENSOR_DELAY_NORMAL)
            if (!ok) close(IllegalStateException("No se pudo suscribir a ${s.name}"))
            awaitClose { sensorManager.unregisterListener(listener) }
        }
    }

    private companion object {
        /** `Sensor.TYPE_TILT_DETECTOR` (@hide). */
        const val TYPE_TILT_DETECTOR = 22
    }
}
