package com.realfamousbae.standby.dock.service

import android.app.ForegroundServiceStartNotAllowedException
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.realfamousbae.standby.dock.graph
import kotlinx.coroutines.launch

/**
 * La única puerta de arranque de StandbyService. Todas las entradas (Activity, BootReceiver,
 * KeepAliveWorker, mosaico) pasan por acá, así que cuando el sistema se niega a arrancar el
 * servicio desde segundo plano siempre queda anotado en el registro de diagnóstico.
 *
 * Los arranques automáticos pasan por el interruptor principal ([RunState.autoStartAllowed]):
 * si el modo está apagado por el interruptor o por una parada externa, el servicio no sube.
 * [userInitiated] = Iván lo mandó prender a mano, no hace falta chequear nada.
 */
object ServiceStarter {

    fun start(context: Context, reason: String, action: String? = null, userInitiated: Boolean = false) {
        if (userInitiated) {
            startNow(context, reason, action)
            return
        }
        val graph = context.graph
        graph.appScope.launch { startIfAllowed(context, reason, action) }
    }

    /** Arrancar si está permitido; espera el chequeo del motivo de la salida anterior. */
    suspend fun startIfAllowed(context: Context, reason: String, action: String? = null): Boolean {
        val graph = context.graph
        if (!graph.runState.autoStartAllowed()) {
            graph.diagLog.log("FGS: omitido ($reason) — el reloj está apagado")
            return false
        }
        startNow(context, reason, action)
        return true
    }

    private fun startNow(context: Context, reason: String, action: String?) {
        val log = context.graph.diagLog
        val intent = Intent(context, StandbyService::class.java)
            .setAction(action)
            .putExtra(StandbyService.EXTRA_REASON, reason)
        try {
            ContextCompat.startForegroundService(context, intent)
        } catch (e: ForegroundServiceStartNotAllowedException) {
            log.log("FGS: inicio PROHIBIDO por el sistema ($reason): ${e.message}")
        } catch (e: Exception) {
            log.log("FGS: error de inicio ($reason): $e")
        }
    }
}
