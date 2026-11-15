package com.realfamousbae.standby.dock.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.realfamousbae.standby.dock.core.trigger.TriggerEvent
import com.realfamousbae.standby.dock.graph
import com.realfamousbae.standby.dock.core.prefs.ClockFace
import com.realfamousbae.standby.dock.core.prefs.ClockFont
import com.realfamousbae.standby.dock.core.prefs.HourFormat
import com.realfamousbae.standby.dock.core.prefs.ScreenLayout
import com.realfamousbae.standby.dock.core.prefs.SideCard
import kotlinx.coroutines.runBlocking

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
            ACTION_DEMO -> runBlocking {
                // Solo por adb (permiso DUMP): fija la configuración para capturas/demos.
                val repo = context.graph.settingsRepository
                intent.getStringExtra("face")?.let { repo.setClockFace(ClockFace.parse(it)) }
                intent.getStringExtra("card")?.let { v -> SideCard.entries.firstOrNull { it.name == v }?.let { repo.setSideCard(it) } }
                intent.getStringExtra("layout")?.let { v -> ScreenLayout.entries.firstOrNull { it.name == v }?.let { repo.setLayout(it) } }
                intent.getStringExtra("font")?.let { repo.setClockFont(ClockFont.parse(it)) }
                intent.getStringExtra("hour")?.let { repo.setHourFormat(HourFormat.parse(it)) }
                intent.getStringExtra("color")?.let { v -> v.removePrefix("0x").toLongOrNull(16)?.let { repo.setClockColor(it) } }
                intent.getStringExtra("size")?.toIntOrNull()?.let { repo.setClockSize(it) }
                intent.getStringExtra("seconds")?.let { repo.setShowSeconds(it == "1" || it == "true") }
                intent.getStringExtra("night")?.let { repo.setNightEnabled(it == "1" || it == "true") }
                intent.getStringExtra("nightsensor")?.let { repo.setNightUseLightSensor(it == "1" || it == "true") }
                intent.getStringExtra("nightstart")?.toIntOrNull()?.let { repo.setNightStart(it) }
                intent.getStringExtra("nightend")?.toIntOrNull()?.let { repo.setNightEnd(it) }
            }
            ACTION_HIDE -> {
                context.graph.controller.setVisible(false)
                context.graph.controller.send(TriggerEvent.UserExit())
            }
        }
    }

    companion object {
        const val ACTION_SHOW = "com.realfamousbae.standby.dock.SHOW_NOW"
        const val ACTION_HIDE = "com.realfamousbae.standby.dock.HIDE_NOW"
        const val ACTION_DEMO = "com.realfamousbae.standby.dock.DEMO"
    }
}
