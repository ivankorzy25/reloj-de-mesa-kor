package com.realfamousbae.standby.dock.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.realfamousbae.standby.dock.graph
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Modo noche: la pantalla se pone en rojo tenue y baja el brillo. */
@Composable
fun NightSection() {
    val graph = LocalContext.current.graph
    val repository = graph.settingsRepository
    val settings by graph.settings.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var startDraft by remember { mutableFloatStateOf(settings.nightStartMinutes.toFloat()) }
    var endDraft by remember { mutableFloatStateOf(settings.nightEndMinutes.toFloat()) }

    SectionTitle("Modo noche")
    SwitchRow(
        title = "Rojo tenue de noche",
        subtitle = "El rojo es el color que menos molesta la vista a oscuras.",
        checked = settings.nightEnabled,
    ) { on -> scope.launch { repository.setNightEnabled(on) } }

    if (!settings.nightEnabled) return

    SliderRow(
        title = "Desde las ${hhmm(startDraft)}",
        value = startDraft,
        range = 0f..(24f * 60f - 15f),
        steps = 24 * 4 - 2,
        onChange = { startDraft = it },
        onCommit = { scope.launch { repository.setNightStart(round15(it)) } },
    )
    SliderRow(
        title = "Hasta las ${hhmm(endDraft)}",
        value = endDraft,
        range = 0f..(24f * 60f - 15f),
        steps = 24 * 4 - 2,
        onChange = { endDraft = it },
        onCommit = { scope.launch { repository.setNightEnd(round15(it)) } },
    )
    SwitchRow(
        title = "También si el cuarto está a oscuras",
        subtitle = "Usa el sensor de luz del teléfono, aunque no sea la hora.",
        checked = settings.nightUseLightSensor,
    ) { on -> scope.launch { repository.setNightUseLightSensor(on) } }
    Text(
        "Fuera de ese horario el reloj vuelve al color elegido.",
        style = MaterialTheme.typography.bodySmall,
    )
}

private fun round15(minutes: Float): Int = (minutes / 15f).roundToInt() * 15

private fun hhmm(minutes: Float): String {
    val total = round15(minutes).coerceIn(0, 24 * 60 - 15)
    return "%02d:%02d".format(total / 60, total % 60)
}
