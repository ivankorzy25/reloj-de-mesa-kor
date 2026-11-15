package com.realfamousbae.standby.dock.settings

import android.hardware.SensorManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.realfamousbae.standby.dock.core.prefs.StandbySettings
import com.realfamousbae.standby.dock.core.trigger.OrientationClassifier
import com.realfamousbae.standby.dock.graph
import com.realfamousbae.standby.dock.sensors.GravitySensor
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Cuándo aparece el modo, con el ángulo del sensor en vivo para calibrarlo con el soporte. */
@Composable
fun TriggerSettingsSection() {
    val context = LocalContext.current
    val graph = context.graph
    val settings by graph.settings.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    // Mientras esta pantalla está abierta escuchamos el acelerómetro acá (el servicio no lo tiene).
    val gravitySensor = remember { GravitySensor(context.getSystemService(SensorManager::class.java)) }
    val tilt by remember {
        gravitySensor.gravity().map { OrientationClassifier.tiltFromVerticalDegrees(it) }
    }.collectAsStateWithLifecycle(initialValue = null)

    // Los deslizadores se mueven en pantalla y se guardan al soltar, para no escribir 60 veces por segundo.
    var tiltDraft by remember(settings.maxTiltDegrees) { mutableFloatStateOf(settings.maxTiltDegrees) }
    var batteryDraft by remember(settings.minBatteryPercentOffCharger) {
        mutableFloatStateOf(settings.minBatteryPercentOffCharger.toFloat())
    }

    SectionTitle("Encendido")
    val current = tilt
    val onSide = current != null && current < tiltDraft
    Text(
        if (current == null) "Inclinación: —" else "Inclinación actual: ${current.roundToInt()}° " +
            if (onSide) "— de costado ✅" else "— no está de costado",
    )
    Text("Umbral de inclinación: ${tiltDraft.roundToInt()}°")
    Slider(
        value = tiltDraft,
        onValueChange = { tiltDraft = it },
        onValueChangeFinished = { scope.launch { graph.settingsRepository.setMaxTilt(tiltDraft) } },
        valueRange = StandbySettings.TILT_RANGE_MIN..StandbySettings.TILT_RANGE_MAX,
    )

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Solo cargando", modifier = Modifier.weight(1f).padding(top = 12.dp))
        Switch(
            checked = settings.requireCharging,
            onCheckedChange = { scope.launch { graph.settingsRepository.setRequireCharging(it) } },
        )
    }
    if (!settings.requireCharging) {
        Text("Sin cargar: solo si la batería no baja de ${batteryDraft.roundToInt()}%")
        Slider(
            value = batteryDraft,
            onValueChange = { batteryDraft = it },
            onValueChangeFinished = {
                scope.launch { graph.settingsRepository.setMinBattery(batteryDraft.roundToInt()) }
            },
            valueRange = StandbySettings.BATTERY_RANGE_MIN.toFloat()..StandbySettings.BATTERY_RANGE_MAX.toFloat(),
        )
    }
}
