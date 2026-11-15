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
import com.realfamousbae.standby.dock.prefs.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Órdenes por adb para probar el modo sin tener que andar tocando el teléfono:
 *
 * ```
 * adb shell am broadcast -a com.realfamousbae.standby.dock.SHOW_NOW -p ar.kor.reloj
 * adb shell am broadcast -a com.realfamousbae.standby.dock.HIDE_NOW -p ar.kor.reloj
 * adb shell am broadcast -a com.realfamousbae.standby.dock.DEMO -p ar.kor.reloj --es face Solar
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
            // Guardar los ajustes es trabajo de archivo: nunca con el hilo principal
            // bloqueado. Con el proceso recién arrancado, un runBlocking acá se traba con la
            // primera lectura de DataStore (que vuelve justamente por el hilo principal).
            ACTION_DEMO -> {
                val pending = goAsync()
                val graph = context.graph
                scope.launch {
                    try {
                        applyDemo(graph.settingsRepository, intent)
                        // Prender o apagar el reloj desde adb (después de un "forzar detención"
                        // queda apagado a propósito, y así se lo vuelve a prender sin tocar el
                        // teléfono).
                        intent.getStringExtra("on")?.let { graph.runState.setEnabled(it.isOn(), "adb") }
                    } finally {
                        pending.finish()
                    }
                }
            }
            ACTION_HIDE -> {
                context.graph.controller.setVisible(false)
                context.graph.controller.send(TriggerEvent.UserExit())
            }
        }
    }

    /** Solo por adb (permiso DUMP): fija la configuración para capturas y demos. */
    private suspend fun applyDemo(repo: SettingsRepository, intent: Intent) {
        intent.getStringExtra("face")?.let { repo.setClockFace(ClockFace.parse(it)) }
        intent.getStringExtra("card")?.let { v -> SideCard.entries.firstOrNull { it.name == v }?.let { repo.setSideCard(it) } }
        intent.getStringExtra("card2")?.let { v -> SideCard.entries.firstOrNull { it.name == v }?.let { repo.setSideCard2(it) } }
        intent.getStringExtra("layout")?.let { v -> ScreenLayout.entries.firstOrNull { it.name == v }?.let { repo.setLayout(it) } }
        intent.getStringExtra("font")?.let { repo.setClockFont(ClockFont.parse(it)) }
        intent.getStringExtra("hour")?.let { repo.setHourFormat(HourFormat.parse(it)) }
        intent.getStringExtra("color")?.let { v -> v.removePrefix("0x").toLongOrNull(16)?.let { repo.setClockColor(it) } }
        intent.getStringExtra("size")?.toIntOrNull()?.let { repo.setClockSize(it) }
        intent.getStringExtra("seconds")?.let { repo.setShowSeconds(it.isOn()) }
        intent.getStringExtra("night")?.let { repo.setNightEnabled(it.isOn()) }
        intent.getStringExtra("nightsensor")?.let { repo.setNightUseLightSensor(it.isOn()) }
        intent.getStringExtra("nightstart")?.toIntOrNull()?.let { repo.setNightStart(it) }
        intent.getStringExtra("nightend")?.toIntOrNull()?.let { repo.setNightEnd(it) }
    }

    private fun String.isOn(): Boolean = this == "1" || this == "true"

    companion object {
        const val ACTION_SHOW = "com.realfamousbae.standby.dock.SHOW_NOW"
        const val ACTION_HIDE = "com.realfamousbae.standby.dock.HIDE_NOW"
        const val ACTION_DEMO = "com.realfamousbae.standby.dock.DEMO"

        /** Fuera del hilo principal y vivo lo que dure el goAsync (unos segundos como mucho). */
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
