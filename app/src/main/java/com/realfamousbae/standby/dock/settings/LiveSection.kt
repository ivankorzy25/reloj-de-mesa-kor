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
import com.realfamousbae.standby.dock.core.dim.DimPolicy
import com.realfamousbae.standby.dock.graph
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Actividades en vivo: lo que está pasando ahora se lleva toda la pantalla. */
@Composable
fun LiveSection() {
    val graph = LocalContext.current.graph
    val repository = graph.settingsRepository
    val settings by graph.settings.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    SectionTitle("Actividades en vivo")
    SwitchRow(
        title = "Llamadas y temporizadores a pantalla completa",
        subtitle = "Cuando te llaman, el nombre ocupa toda la pantalla con los botones de " +
            "atender y colgar. Lo mismo con el temporizador del reloj.",
        checked = settings.liveActivitiesEnabled,
    ) { on -> scope.launch { repository.setLiveActivities(on) } }

    if (!settings.liveActivitiesEnabled) return

    SwitchRow(
        title = "La música también",
        subtitle = "Mientras suena algo se ve la carátula grande con los controles. " +
            "Apagalo si preferís que quede siempre el reloj.",
        checked = settings.liveMusicFullScreen,
    ) { on -> scope.launch { repository.setLiveMusicFullScreen(on) } }
    Text(
        "Las llamadas y los temporizadores se leen del acceso a notificaciones, el mismo " +
            "que usan los avisos.",
        style = MaterialTheme.typography.bodySmall,
    )
}

/** Siempre encendido: en vez de apagarse, la pantalla baja al mínimo con un reloj chiquito. */
@Composable
fun DimSection() {
    val graph = LocalContext.current.graph
    val repository = graph.settingsRepository
    val settings by graph.settings.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var minutos by remember { mutableFloatStateOf(settings.dimAfterMinutes.toFloat()) }

    SectionTitle("Siempre encendido")
    SwitchRow(
        title = "Atenuar en vez de apagarse",
        subtitle = "Después de un rato sin tocar nada queda la hora chiquita en el brillo más " +
            "bajo, y se corre de lugar para no quemar la pantalla. Solo cargando: a batería " +
            "la pantalla se apaga como siempre.",
        checked = settings.dimEnabled,
    ) { on -> scope.launch { repository.setDimEnabled(on) } }

    if (!settings.dimEnabled) return

    SliderRow(
        title = "Después de ${minutos.roundToInt()} min sin tocar nada",
        value = minutos,
        range = DimPolicy.MIN_MINUTES.toFloat()..DimPolicy.MAX_MINUTES.toFloat(),
        steps = DimPolicy.MAX_MINUTES - DimPolicy.MIN_MINUTES - 1,
        onChange = { minutos = it },
        onCommit = { scope.launch { repository.setDimAfterMinutes(it.roundToInt()) } },
    )
}
