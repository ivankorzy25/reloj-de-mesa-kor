package com.realfamousbae.standby.dock.place

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.realfamousbae.standby.dock.core.place.PlaceMemory
import kotlin.math.roundToInt

/**
 * Dónde está apoyado el teléfono ahora, visto desde el cargador: si carga sin cable, con un
 * cargador de pared o por USB, y con cuánto voltaje. Con eso alcanza para distinguir la mesa
 * de luz del escritorio sin pedir un solo permiso (ver [PlaceMemory]).
 */
class PlaceDetector(private val context: Context) {

    data class Place(val key: String, val label: String)

    fun current(): Place {
        val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val plugged = battery?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val charger = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> PlaceMemory.Charger.Inalambrico
            BatteryManager.BATTERY_PLUGGED_AC -> PlaceMemory.Charger.Pared
            BatteryManager.BATTERY_PLUGGED_USB -> PlaceMemory.Charger.Usb
            else -> PlaceMemory.Charger.Ninguno
        }
        // Los voltios de carga vienen en el mismo aviso de la batería, en microvoltios. La
        // constante no es parte de la API pública (sí el aviso), así que se usa el nombre del
        // dato tal cual; si el firmware no lo manda, el lugar queda nombrado solo por el tipo
        // de cargador y listo.
        val microVolts = battery?.getIntExtra(EXTRA_MAX_CHARGING_VOLTAGE, -1) ?: -1
        val volts = if (microVolts > 0) (microVolts / 1_000_000f).roundToInt() else null
        return Place(
            key = PlaceMemory.key(charger, volts),
            label = PlaceMemory.label(charger, volts),
        )
    }

    private companion object {
        const val EXTRA_MAX_CHARGING_VOLTAGE = "max_charging_voltage"
    }
}
