package com.realfamousbae.standby.dock.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Red de seguridad por si el sistema mata el servicio y no lo reinicia (START_STICKY no está
 * garantizado). El modo funciona también sin cargador, así que la tarea es periódica y sin
 * condiciones: cada ~15 minutos (el sistema puede correrlo un poco) levanta el servicio. Si
 * el servicio está vivo, volver a arrancarlo no hace nada. Arrancar un servicio en primer
 * plano desde segundo plano está permitido gracias a la excepción de optimización de
 * batería. Solo se programa mientras el modo está prendido ([RunState]); con el interruptor
 * apagado, el arranque se saltea igual.
 */
class KeepAliveWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        ServiceStarter.startIfAllowed(applicationContext, reason = "worker")
        return Result.success()
    }

    companion object {
        private const val UNIQUE_NAME = "standby_keep_alive"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<KeepAliveWorker>(15, TimeUnit.MINUTES).build()
            // UPDATE: reemplaza la tarea de versiones anteriores (la que pedía estar cargando).
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(UNIQUE_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        }

        /** Modo apagado: no hacen falta los levantes de fondo. */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_NAME)
        }
    }
}
