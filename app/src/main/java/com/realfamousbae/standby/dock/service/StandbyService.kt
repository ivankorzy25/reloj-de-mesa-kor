package com.realfamousbae.standby.dock.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.hardware.SensorManager
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.realfamousbae.standby.dock.core.trigger.Phase
import com.realfamousbae.standby.dock.core.trigger.TriggerEffect
import com.realfamousbae.standby.dock.core.trigger.TriggerEngine
import com.realfamousbae.standby.dock.core.trigger.TriggerEvent
import com.realfamousbae.standby.dock.core.trigger.TriggerState
import com.realfamousbae.standby.dock.graph
import com.realfamousbae.standby.dock.sensors.GravitySensor
import com.realfamousbae.standby.dock.sensors.MotionWakeSensor
import com.realfamousbae.standby.dock.standby.StandbyActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Servicio en primer plano: junta todo lo que entra (pantalla, energía, sensores, acciones
 * de Iván), se lo pasa al [TriggerEngine] de :core y ejecuta sus efectos. Acá no hay nada
 * de lógica propia sobre "cuándo mostrar": eso está todo en TriggerEngine, con unit tests.
 *
 * Todos los eventos pasan por un solo canal y se atienden de a uno en el hilo principal,
 * así que el estado no necesita sincronización.
 */
class StandbyService : LifecycleService() {

    private val log get() = graph.diagLog
    private lateinit var powerManager: PowerManager
    private lateinit var gravitySensor: GravitySensor
    private lateinit var motionWakeSensor: MotionWakeSensor

    private val events = Channel<TriggerEvent>(Channel.UNLIMITED)
    private var state = TriggerState()

    private var gravityJob: Job? = null
    private var tickJob: Job? = null
    private var wakeLockRefreshJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var testLaunchJob: Job? = null
    private var lastLoggedTilt: Int? = null

    private val systemEvents = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> events.trySend(TriggerEvent.ScreenOff)
                Intent.ACTION_SCREEN_ON -> events.trySend(TriggerEvent.ScreenOn)
                Intent.ACTION_USER_PRESENT -> events.trySend(TriggerEvent.UserPresent)
                Intent.ACTION_BATTERY_CHANGED -> events.trySend(powerEvent(intent))
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        powerManager = getSystemService(PowerManager::class.java)
        val sensorManager = getSystemService(SensorManager::class.java)
        gravitySensor = GravitySensor(sensorManager)
        motionWakeSensor = MotionWakeSensor(sensorManager)
        goForeground()

        // La pantalla y la energía no se pueden escuchar desde el manifiesto: las escuchamos
        // mientras viva el servicio. BATTERY_CHANGED es "pegajoso": al registrarse llega el estado.
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }
        ContextCompat.registerReceiver(this, systemEvents, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        log.log(
            "Servicio creado. Acelerómetro: ${gravitySensor.description}; " +
                "sensor de movimiento: ${motionWakeSensor.description}",
        )

        lifecycleScope.launch { for (event in events) handle(event) }
        lifecycleScope.launch {
            graph.controller.userEvents.collect { event ->
                // Si la pantalla está prendida lo sabe solo el sistema: al apretar el botón de
                // encendido, la Activity puede irse antes de que llegue el SCREEN_OFF.
                events.send(
                    if (event is TriggerEvent.UserExit) event.copy(screenOn = powerManager.isInteractive) else event,
                )
            }
        }
        // Las emergentes del sistema las tapamos solo mientras el modo está en pantalla y en su
        // lugar están nuestros avisos. El primer valor (false) además apaga la regla que
        // pudo quedar colgada del proceso anterior.
        lifecycleScope.launch {
            combine(graph.controller.visible, graph.settings) { visible, s ->
                visible && s.hideSystemHeadsUp && s.notificationBannersEnabled
            }
                .distinctUntilChanged()
                .collect(graph.quietMode::setActive)
        }
        lifecycleScope.launch {
            motionWakeSensor.events().collect { events.send(TriggerEvent.MotionWake) }
        }
        // Interruptor principal: en "Apagado" (interruptor, mosaico o parada externa) el servicio
        // se va solo. Eso cubre también el reinicio del sistema por START_STICKY estando apagado.
        lifecycleScope.launch {
            graph.settingsRepository.settings.first { !it.enabled }
            log.log("Servicio: el reloj está apagado, nos detenemos")
            stopSelf()
        }
        // Si el servicio se levantó con la pantalla apagada (arranque, worker), probamos ya.
        if (!powerManager.isInteractive) events.trySend(TriggerEvent.ScreenOff)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        // En cada startForegroundService el sistema espera un startForeground: lo llamamos de nuevo.
        goForeground()
        val reason = intent?.getStringExtra(EXTRA_REASON) ?: "reinicio del sistema"
        log.log("Servicio: inicio ($reason)")
        if (intent?.action == ACTION_SHOW_NOW) {
            // Mostrar ya (orden por adb, ver CommandReceiver).
            events.trySend(TriggerEvent.ManualShow)
        }
        if (intent?.action == ACTION_TEST_LAUNCH) {
            testLaunchJob?.cancel()
            testLaunchJob = lifecycleScope.launch {
                log.log("Prueba: modo en ${TEST_LAUNCH_DELAY_MS / 1000} s — bloqueá la pantalla")
                delay(TEST_LAUNCH_DELAY_MS)
                events.send(TriggerEvent.ManualShow)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        unregisterReceiver(systemEvents)
        releaseWakeLock()
        graph.controller.setVisible(false)
        graph.quietMode.setActive(false)
        log.log("Servicio detenido")
        super.onDestroy()
    }

    private fun handle(event: TriggerEvent) {
        val before = state
        val result = TriggerEngine.reduce(before, event, SystemClock.elapsedRealtime(), graph.settings.value)
        state = result.state
        logTransition(before, state, event)
        result.effects.forEach(::apply)
    }

    private fun apply(effect: TriggerEffect) {
        when (effect) {
            is TriggerEffect.SetSensorActive -> setGravityActive(effect.active)
            is TriggerEffect.HoldWakeLock -> if (effect.hold) acquireWakeLock() else releaseWakeLock()
            is TriggerEffect.ScheduleTick -> {
                tickJob?.cancel()
                tickJob = lifecycleScope.launch {
                    delay((effect.atMs - SystemClock.elapsedRealtime()).coerceAtLeast(0))
                    events.send(TriggerEvent.Tick)
                }
            }
            TriggerEffect.Show -> {
                graph.controller.setVisible(true)
                log.log("Modo: mostrar")
                // El arranque desde segundo plano lo permite "Sobre otras apps" (BAL_ALLOW_SAW_PERMISSION).
                startActivity(StandbyActivity.intent(this))
            }
            is TriggerEffect.Hide -> {
                log.log("Modo: ocultar (${effect.reason})")
                graph.controller.setVisible(false)
            }
        }
    }

    private fun setGravityActive(active: Boolean) {
        gravityJob?.cancel()
        gravityJob = null
        lastLoggedTilt = null
        if (!active) return
        if (!gravitySensor.isAvailable) {
            log.log("No hay acelerómetro: no se puede arrancar solo por posición")
            return
        }
        gravityJob = lifecycleScope.launch {
            gravitySensor.gravity().collect { events.send(TriggerEvent.GravityReading(it)) }
        }
    }

    /**
     * El S24 Ultra no tiene acelerómetro con wake-up, así que mientras esperamos la posición
     * con la pantalla apagada mantenemos un PARTIAL_WAKE_LOCK. No se cuenta por referencias y
     * se renueva con tiempo límite mientras haga falta: si el servicio muere, el sistema lo suelta.
     */
    private fun acquireWakeLock() {
        val lock = wakeLock ?: powerManager
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "StandBy:trigger")
            .apply { setReferenceCounted(false) }
            .also { wakeLock = it }
        lock.acquire(WAKE_LOCK_TIMEOUT_MS)
        wakeLockRefreshJob?.cancel()
        wakeLockRefreshJob = lifecycleScope.launch {
            while (isActive) {
                delay(WAKE_LOCK_REFRESH_MS)
                lock.acquire(WAKE_LOCK_TIMEOUT_MS)
            }
        }
    }

    private fun releaseWakeLock() {
        wakeLockRefreshJob?.cancel()
        wakeLockRefreshJob = null
        wakeLock?.takeIf { it.isHeld }?.release()
    }

    private fun goForeground() {
        // El tipo specialUse apareció en Android 14; en 12 y 13 no se manda tipo.
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, Notifications.ID_SERVICE, Notifications.service(this), type)
    }

    private fun powerEvent(intent: Intent): TriggerEvent.Power {
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val percent = if (level >= 0 && scale > 0) (level * 100f / scale).roundToInt() else 100
        return TriggerEvent.Power(charging = plugged, batteryPercent = percent)
    }

    /** Al registro solo van el cambio de fase, la energía y los cambios de ángulo que importan. */
    private fun logTransition(before: TriggerState, after: TriggerState, event: TriggerEvent) {
        if (before.charging != after.charging) {
            log.log("Energía: ${if (after.charging) "cargando" else "batería"}, ${after.batteryPercent}%")
        }
        if (before.phase != after.phase) {
            val cause = event::class.simpleName
            log.log("Fase: ${before.phase} → ${after.phase} ($cause)${phaseNote(after)}")
        }
        if (before.suppressedUntilScreenOn != after.suppressedUntilScreenOn) {
            log.log(if (after.suppressedUntilScreenOn) "Bloqueado hasta que se prenda la pantalla" else "Bloqueo quitado")
        }
        val tilt = after.lastTiltDegrees?.roundToInt()
        if (after.sensorActive && tilt != null && after.phase == Phase.Armed) {
            val last = lastLoggedTilt
            if (last == null || kotlin.math.abs(tilt - last) >= TILT_LOG_STEP) {
                lastLoggedTilt = tilt
                log.log(String.format(Locale.ROOT, "Inclinación respecto a vertical: %d°", tilt))
            }
        }
    }

    private fun phaseNote(s: TriggerState): String = when (s.phase) {
        Phase.Armed -> if (s.armedUntilMs == null) ", esperando sin límite" else ", prueba 10 s"
        else -> ""
    }

    companion object {
        const val EXTRA_REASON = "reason"
        const val ACTION_TEST_LAUNCH = "com.realfamousbae.standby.dock.TEST_LAUNCH"
        const val ACTION_SHOW_NOW = "com.realfamousbae.standby.dock.SHOW_NOW_SERVICE"

        private const val TEST_LAUNCH_DELAY_MS = 10_000L
        private const val WAKE_LOCK_TIMEOUT_MS = 10 * 60 * 1000L
        private const val WAKE_LOCK_REFRESH_MS = 5 * 60 * 1000L
        private const val TILT_LOG_STEP = 10
    }
}
