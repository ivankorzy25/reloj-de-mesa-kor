package com.realfamousbae.standby.dock.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.realfamousbae.standby.dock.core.trigger.TriggerEvent
import com.realfamousbae.standby.dock.graph

/**
 * Órdenes por adb para probar el modo sin tener que andar tocando el teléfono:
 *
 * ```
 * adb shell am broadcast -a com.realfamousbae.standby.dock.SHOW_NOW -p com.realfamousbae.standby.dock
 * adb shell am broadcast -a com.realfamousbae.standby.dock.HIDE_NOW -p com.realfamousbae.standby.dock
 * ```
 *
 * Está protegido con el permiso DUMP, que solo tienen adb y el sistema: ninguna app común
 * puede usarlo.
 */
class CommandReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_SHOW -> ServiceStarter.start(
                context,
                reason = "adb",
                action = StandbyService.ACTION_SHOW_NOW,
                userInitiated = true,
            )
            ACTION_HIDE -> {
                context.graph.controller.setVisible(false)
                context.graph.controller.send(TriggerEvent.UserExit())
            }
        }
    }

    companion object {
        const val ACTION_SHOW = "com.realfamousbae.standby.dock.SHOW_NOW"
        const val ACTION_HIDE = "com.realfamousbae.standby.dock.HIDE_NOW"
    }
}
