package com.realfamousbae.standby.dock.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.realfamousbae.standby.dock.graph
import kotlinx.coroutines.launch

/** Levanta el servicio después de reiniciar y después de actualizar el APK, si el modo está prendido. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                // El chequeo del interruptor es asíncrono: mantenemos vivo el receiver hasta que
                // termine, para que el arranque del servicio entre en la ventana de BOOT_COMPLETED.
                val pending = goAsync()
                context.graph.appScope.launch {
                    try {
                        ServiceStarter.startIfAllowed(context, reason = intent.action!!.substringAfterLast('.'))
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }
}
