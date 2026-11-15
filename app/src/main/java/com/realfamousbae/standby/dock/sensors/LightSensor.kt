package com.realfamousbae.standby.dock.sensors

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow

/** El sensor de luz como Flow<lux>. El evento llega solo cuando el valor cambia. */
class LightSensor(private val sensorManager: SensorManager) {

    private val sensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

    val isAvailable: Boolean get() = sensor != null

    fun lux(): Flow<Float> {
        val s = sensor ?: return emptyFlow()
        return callbackFlow {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    trySend(event.values[0])
                }

                override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
            }
            sensorManager.registerListener(listener, s, SensorManager.SENSOR_DELAY_NORMAL)
            awaitClose { sensorManager.unregisterListener(listener) }
        }
    }
}
