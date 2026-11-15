package com.realfamousbae.standby.dock.service

import android.app.ActivityManager
import android.app.Application
import android.app.ApplicationExitInfo
import android.content.ComponentName
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.quicksettings.TileService
import com.realfamousbae.standby.dock.core.run.LastExit
import com.realfamousbae.standby.dock.core.run.RunPolicy
import com.realfamousbae.standby.dock.diag.DiagLog
import com.realfamousbae.standby.dock.notifications.StandbyNotificationListener
import com.realfamousbae.standby.dock.prefs.SettingsRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * El interruptor principal del modo y el respeto por la parada externa de la app.
 *
 * Al arrancar el proceso mira una sola vez cómo murió el proceso anterior
 * (ApplicationExitInfo): si Iván paró la app a propósito, el interruptor pasa a "Apagado"
 * (la decisión está en [RunPolicy], en :core). Todos los caminos automáticos de arranque
 * esperan a que ese chequeo termine ([autoStartAllowed]), así que nadie puede adelantarse.
 */
class RunState(
    private val app: Application,
    private val settings: SettingsRepository,
    private val log: DiagLog,
) {
    private val ready = CompletableDeferred<Unit>()

    /** Se llama una sola vez desde App.onCreate. */
    fun startup(scope: CoroutineScope) {
        scope.launch {
            try {
                checkLastExit()
            } catch (e: Exception) {
                log.log("Inicio: no se pudo leer el motivo de la salida anterior: $e")
            } finally {
                ready.complete(Unit)
            }
            syncBackgroundWork(settings.settings.first().enabled)
        }
    }

    /** Si se puede levantar el servicio solo. Espera el chequeo de la salida anterior. */
    suspend fun autoStartAllowed(): Boolean {
        ready.await()
        return RunPolicy.autoStartAllowed(settings.settings.first().enabled)
    }

    /** Prendido y apagado por Iván (el interruptor de Ajustes, el mosaico de la cortina). */
    suspend fun setEnabled(on: Boolean, reason: String) {
        settings.setEnabled(on)
        log.log(if (on) "El reloj quedó prendido ($reason)" else "El reloj quedó apagado ($reason)")
        syncBackgroundWork(on)
        if (on) {
            ServiceStarter.start(app, reason = reason, userInitiated = true)
            runCatching {
                NotificationListenerService.requestRebind(StandbyNotificationListener.component(app))
            }
        } else {
            app.stopService(Intent(app, StandbyService::class.java))
        }
        refreshTile()
    }

    private suspend fun checkLastExit() {
        val am = app.getSystemService(ActivityManager::class.java)
        val info = am.getHistoricalProcessExitReasons(app.packageName, 0, 1).firstOrNull() ?: return
        val userRequested = info.reason == ApplicationExitInfo.REASON_USER_REQUESTED
        val kind = RunPolicy.classify(userRequested, info.description)
        val decision = RunPolicy.onProcessStart(LastExit(kind, info.timestamp), settings.lastHandledExitMs())
        val handled = decision.markHandledMs ?: return
        log.log("Última salida del proceso: motivo ${info.reason}, «${info.description.orEmpty()}» → $kind")
        if (decision.disable) {
            settings.setEnabled(false, external = true)
            log.log("El reloj quedó apagado: cerraron la app a la fuerza")
            app.stopService(Intent(app, StandbyService::class.java))
            refreshTile()
        }
        settings.setLastHandledExitMs(handled)
    }

    private fun syncBackgroundWork(enabled: Boolean) {
        if (enabled) KeepAliveWorker.schedule(app) else KeepAliveWorker.cancel(app)
    }

    private fun refreshTile() {
        runCatching { TileService.requestListeningState(app, ComponentName(app, StandbyTileService::class.java)) }
    }
}
