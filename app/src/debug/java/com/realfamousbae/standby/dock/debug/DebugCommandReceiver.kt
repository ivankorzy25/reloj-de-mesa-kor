package com.realfamousbae.standby.dock.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.realfamousbae.standby.dock.graph
import com.realfamousbae.standby.dock.service.KeepAliveWorker
import com.realfamousbae.standby.dock.service.ServiceStarter
import com.realfamousbae.standby.dock.service.StandbyService
import java.util.concurrent.TimeUnit

/**
 * Órdenes de depuración por adb (solo en la build debug), por ejemplo:
 * `adb shell am broadcast -a com.realfamousbae.standby.dock.debug.RUN_KEEP_ALIVE -p com.realfamousbae.standby.dock`
 */
class DebugCommandReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val log = context.graph.diagLog
        when (intent.action?.substringAfterLast('.')) {
            "STOP_SERVICE" -> {
                context.stopService(Intent(context, StandbyService::class.java))
                log.log("adb: servicio detenido")
            }
            // Una tarea suelta con el mismo worker: probamos arrancar el servicio en primer plano
            // desde un Worker de fondo, salteando la protección de WorkManager contra arrancar
            // antes de tiempo una tarea periódica. Los 15 s de espera son para que, cuando el
            // worker arranque, el proceso ya esté en segundo plano y no atendiendo este broadcast.
            "RUN_KEEP_ALIVE" -> {
                val request = OneTimeWorkRequestBuilder<KeepAliveWorker>()
                    .setInitialDelay(15, TimeUnit.SECONDS)
                    .build()
                WorkManager.getInstance(context).enqueue(request)
                log.log("adb: KeepAliveWorker suelto en 15 s")
            }
            "SHOW_STANDBY" ->
                ServiceStarter.start(context, "adb", StandbyService.ACTION_TEST_LAUNCH)
        }
    }
}
