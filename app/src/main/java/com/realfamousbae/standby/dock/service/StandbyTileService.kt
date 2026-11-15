package com.realfamousbae.standby.dock.service

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.realfamousbae.standby.dock.graph
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Mosaico de la cortina: prende y apaga el modo igual que el interruptor de Ajustes. */
class StandbyTileService : TileService() {

    private var job: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        job?.cancel()
        job = graph.appScope.launch {
            graph.settingsRepository.settings.collect { render(it.enabled) }
        }
    }

    override fun onStopListening() {
        job?.cancel()
        job = null
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        graph.appScope.launch {
            val on = !graph.settingsRepository.settings.first().enabled
            // Si el sistema no deja arrancar el servicio desde segundo plano, ServiceStarter lo
            // anota; el interruptor ya quedó guardado y el servicio sube por el próximo camino.
            graph.runState.setEnabled(on, reason = "mosaico")
        }
    }

    private fun render(enabled: Boolean) {
        val tile = qsTile ?: return
        tile.state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.subtitle = if (enabled) "Activado" else "Apagado"
        tile.updateTile()
    }
}
