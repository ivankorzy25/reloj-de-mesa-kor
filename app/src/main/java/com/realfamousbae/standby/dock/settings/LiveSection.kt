package com.realfamousbae.standby.dock.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.realfamousbae.standby.dock.graph
import kotlinx.coroutines.launch

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
