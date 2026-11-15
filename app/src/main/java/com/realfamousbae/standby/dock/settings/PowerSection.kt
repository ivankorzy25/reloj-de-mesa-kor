package com.realfamousbae.standby.dock.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.realfamousbae.standby.dock.graph
import kotlinx.coroutines.launch

/** Interruptor principal del modo (lo mismo que hace el mosaico de la cortina). */
@Composable
fun PowerSection() {
    val graph = LocalContext.current.graph
    val settings by graph.settings.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Funcionando", style = MaterialTheme.typography.titleMedium)
            Text(
                when {
                    settings.enabled -> "El modo arranca solo"
                    settings.stoppedExternally ->
                        "Apagado tras forzar el cierre de la app. El servicio y las " +
                            "chequeos detenidos"
                    else -> "Apagado. Servicio y chequeos en segundo plano detenidos"
                },
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Switch(
            checked = settings.enabled,
            onCheckedChange = { on -> scope.launch { graph.runState.setEnabled(on, reason = "interruptor") } },
        )
    }
}
