package com.realfamousbae.standby.dock.service

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.realfamousbae.standby.dock.core.trigger.TriggerEvent
import com.realfamousbae.standby.dock.AppGraph
import com.realfamousbae.standby.dock.core.place.PlacePrefs
import com.realfamousbae.standby.dock.graph
import com.realfamousbae.standby.dock.live.LiveActivity
import com.realfamousbae.standby.dock.place.PlaceDetector
import com.realfamousbae.standby.dock.core.prefs.ClockFace
import com.realfamousbae.standby.dock.core.prefs.ClockFont
import com.realfamousbae.standby.dock.core.prefs.HourFormat
import com.realfamousbae.standby.dock.core.prefs.ScreenLayout
import com.realfamousbae.standby.dock.core.prefs.SideCard
import com.realfamousbae.standby.dock.prefs.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
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
                        // Lo que fija el DEMO manda: si no, la memoria por cargador volvería a
                        // poner lo de la última vez justo cuando se está sacando la captura.
                        rememberDemoPlace(context, graph, intent)
                        // Prender o apagar el reloj desde adb (después de un "forzar detención"
                        // queda apagado a propósito, y así se lo vuelve a prender sin tocar el
                        // teléfono).
                        intent.getStringExtra("on")?.let { graph.runState.setEnabled(it.isOn(), "adb") }
                        intent.getStringExtra("dimnow")?.let { graph.demoDim.value = it.isOn() }
                        intent.getStringExtra("live")?.let {
                            graph.liveActivities.setDemo(demoActivity(context, it))
                        }
                        // --es dump 1 devuelve el registro de diagnóstico como resultado del
                        // broadcast: en los Samsung, logcat no muestra lo que escriben las
                        // apps de uno, así que esta es la única forma de leerlo por adb.
                        if (intent.getStringExtra("dump") != null) {
                            val vivo = when (graph.liveActivities.current.value) {
                                is LiveActivity.Call -> "llamada"
                                is LiveActivity.Timer -> "temporizador"
                                LiveActivity.Music -> "música"
                                null -> "nada"
                            }
                            val lugar = PlaceDetector(context).current()
                            val guardado = graph.settings.value.places[lugar.key]
                            pending.resultData = buildString {
                                append("en vivo: $vivo\n")
                                append("lugar: ${lugar.label} [${lugar.key}] guardado=$guardado\n")
                                append(graph.diagLog.lines().takeLast(20).joinToString("\n"))
                            }
                        }
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
        intent.getStringExtra("charging")?.let { repo.setRequireCharging(it.isOn()) }
        intent.getStringExtra("memories")?.let { repo.setImmichMemories(it.isOn()) }
        intent.getStringExtra("favorites")?.let { repo.setImmichOnlyFavorites(it.isOn()) }
        intent.getStringExtra("places")?.let { repo.setPlaceMemory(it.isOn()) }
        intent.getStringExtra("dim")?.let { repo.setDimEnabled(it.isOn()) }
        intent.getStringExtra("dimminutes")?.toIntOrNull()?.let { repo.setDimAfterMinutes(it) }
    }

    /** Dejar lo que fijó el DEMO como "lo que había quedado" en este lugar. */
    private suspend fun rememberDemoPlace(context: Context, graph: AppGraph, intent: Intent) {
        val toca = CAMPOS_DE_LUGAR.any { intent.getStringExtra(it) != null }
        if (!toca) return
        val settings = graph.settingsRepository.settings.first()
        if (!settings.placeMemoryEnabled) return
        graph.settingsRepository.rememberPlace(
            key = PlaceDetector(context).current().key,
            prefs = PlacePrefs(
                face = settings.clockFace,
                card = settings.sideCard,
                card2 = settings.sideCard2,
                layout = settings.layout,
            ),
        )
    }

    private fun String.isOn(): Boolean = this == "1" || this == "true"

    /**
     * Actividades en vivo de mentira, para las capturas y el video: no hay forma de hacer que
     * entre una llamada de verdad desde adb. Solo viven en memoria.
     */
    private fun demoActivity(context: Context, kind: String): LiveActivity? = when (kind.lowercase()) {
        "llamada" -> LiveActivity.Call(
            name = "Mamá",
            app = "Teléfono",
            icon = null,
            incoming = true,
            // Botones que no hacen nada: son para la foto, no para atender.
            answer = noop(context),
            hangUp = noop(context),
        )
        "timer" -> LiveActivity.Timer(
            label = "Temporizador",
            app = "Reloj",
            baseMs = System.currentTimeMillis() + 9 * 60_000L + 58_000L,
            countDown = true,
            actions = listOf(LiveActivity.Action("Pausar", noop(context))),
        )
        "musica" -> LiveActivity.Music
        else -> null
    }

    /** Una intención que no hace nada, para los botones de las actividades de mentira. */
    private fun noop(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(ACTION_NOOP).setPackage(context.packageName),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    companion object {
        const val ACTION_SHOW = "com.realfamousbae.standby.dock.SHOW_NOW"
        const val ACTION_HIDE = "com.realfamousbae.standby.dock.HIDE_NOW"
        const val ACTION_DEMO = "com.realfamousbae.standby.dock.DEMO"
        private const val ACTION_NOOP = "com.realfamousbae.standby.dock.NOOP"

        /** Los datos del DEMO que también son parte de la memoria de cada lugar. */
        private val CAMPOS_DE_LUGAR = listOf("face", "card", "card2", "layout")

        /** Fuera del hilo principal y vivo lo que dure el goAsync (unos segundos como mucho). */
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
