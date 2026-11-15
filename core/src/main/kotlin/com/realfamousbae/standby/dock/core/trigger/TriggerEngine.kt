package com.realfamousbae.standby.dock.core.trigger

import com.realfamousbae.standby.dock.core.prefs.StandbySettings

/**
 * Lógica pura de "cuándo mostrar el modo". Sin Android, sin reloj y sin hilos:
 * `reduce(state, event, nowMs, settings)` devuelve el estado nuevo y la lista de efectos
 * que ejecuta el servicio. Por eso todo el comportamiento se prueba con unit tests.
 *
 * Reglas (en detalle, docs/PLAN.md, punto 2.3):
 * - Se entra solo por flancos: se apagó la pantalla, se enchufó el cargador con la pantalla
 *   apagada, o el sensor de movimiento despertó al teléfono con la pantalla apagada.
 *   Mientras la pantalla está prendida, el modo no aparece.
 * - Después del flanco viene la fase Armed: se escucha el acelerómetro. Cargando, sin
 *   límite de tiempo; sin cargador, una ventana de prueba [PROBE_WINDOW_MS], para no tener
 *   el sensor y el wake lock despiertos toda la noche.
 * - Se muestra: de costado sin moverse [ENTER_STABLE_MS], con energía permitida y sin veto.
 * - Se esconde: lo sacaron del soporte ([EXIT_STABLE_MS] pasado el umbral más
 *   [EXIT_HYSTERESIS_DEGREES]), la batería bajó del mínimo sin cargador, desbloquearon el
 *   teléfono o lo cerraron con un gesto.
 * - Si apagaron el modo con el botón de encendido queda vetado hasta que la pantalla se
 *   vuelva a prender: mientras tanto no se arma la espera (ni sensor ni wake lock). Si
 *   prendieron la pantalla y dejaron el teléfono, cuando se apague el modo vuelve. Si lo
 *   cerraron con un toque no hay veto: la pantalla sigue prendida y, al apagarse, vuelve.
 */
object TriggerEngine {

    const val ENTER_STABLE_MS = 1_500L
    const val EXIT_STABLE_MS = 2_000L
    const val PROBE_WINDOW_MS = 10_000L
    const val EXIT_HYSTERESIS_DEGREES = 10f

    data class Result(val state: TriggerState, val effects: List<TriggerEffect>)

    fun reduce(
        state: TriggerState,
        event: TriggerEvent,
        nowMs: Long,
        settings: StandbySettings,
    ): Result {
        val effects = mutableListOf<TriggerEffect>()
        var s = state

        when (event) {
            TriggerEvent.ScreenOff -> {
                s = s.copy(screenInteractive = false)
                s = if (s.phase == Phase.Showing) {
                    // Pantalla del modo apagado con el botón de encendido: eso es un "apagalo".
                    hide(s, effects, "apagado con el botón de encendido", suppress = true)
                } else if (s.suppressedUntilScreenOn) {
                    s
                } else {
                    arm(s, nowMs, effects)
                }
            }

            TriggerEvent.ScreenOn -> {
                s = s.copy(screenInteractive = true, suppressedUntilScreenOn = false)
                // La pantalla la prendió Iván: dejamos de esperar. Si la prendimos nosotros
                // (turnScreenOn al mostrar), la fase ya es Showing y no cambia nada.
                if (s.phase == Phase.Armed) s = s.copy(phase = Phase.Idle, armedUntilMs = null)
            }

            TriggerEvent.UserPresent -> {
                s = s.copy(screenInteractive = true, suppressedUntilScreenOn = false)
                s = when (s.phase) {
                    Phase.Showing -> hide(s, effects, "teléfono desbloqueado", suppress = false)
                    Phase.Armed -> s.copy(phase = Phase.Idle, armedUntilMs = null)
                    Phase.Idle -> s
                }
            }

            is TriggerEvent.Power -> {
                val plugged = event.charging && !s.charging
                val chargingChanged = event.charging != s.charging
                s = s.copy(charging = event.charging, batteryPercent = event.batteryPercent)
                if (chargingChanged) s = s.copy(suppressedUntilScreenOn = false)
                if (chargingChanged && s.phase == Phase.Armed) {
                    // Cargando esperamos sin límite; sin cargador abrimos otra ventana de prueba.
                    s = s.copy(armedUntilMs = if (s.charging) null else nowMs + PROBE_WINDOW_MS)
                    s.armedUntilMs?.let { effects += TriggerEffect.ScheduleTick(it) }
                }
                if (plugged && !s.screenInteractive && s.phase == Phase.Idle) {
                    s = arm(s, nowMs, effects)
                }
            }

            is TriggerEvent.GravityReading -> {
                if (s.sensorActive) s = trackOrientation(s, event.gravity, nowMs, settings)
            }

            TriggerEvent.MotionWake -> {
                if (!s.screenInteractive && s.phase != Phase.Showing && !s.suppressedUntilScreenOn) {
                    s = arm(s, nowMs, effects)
                }
            }

            TriggerEvent.Tick -> Unit

            is TriggerEvent.UserExit -> {
                if (s.phase == Phase.Showing) {
                    // La Activity se fue con la pantalla ya apagada: fue el botón de encendido,
                    // el SCREEN_OFF todavía no llegó. Si no, fue un toque: sin veto, la pantalla sigue.
                    s = hide(s, effects, "cerrado por el usuario", suppress = !event.screenOn, notify = false)
                }
            }

            TriggerEvent.ManualShow -> {
                s = s.copy(phase = Phase.Showing, manual = true, suppressedUntilScreenOn = false, armedUntilMs = null)
                effects += TriggerEffect.Show
            }
        }

        s = evaluate(s, nowMs, settings, effects)
        s = reconcileResources(s, effects)
        return Result(s, effects)
    }

    /** Pasar a esperar la posición (o estirar la espera). */
    private fun arm(s: TriggerState, nowMs: Long, effects: MutableList<TriggerEffect>): TriggerState {
        val until = if (s.charging) null else nowMs + PROBE_WINDOW_MS
        until?.let { effects += TriggerEffect.ScheduleTick(it) }
        return s.copy(phase = Phase.Armed, armedUntilMs = until)
    }

    private fun hide(
        s: TriggerState,
        effects: MutableList<TriggerEffect>,
        reason: String,
        suppress: Boolean,
        notify: Boolean = true,
    ): TriggerState {
        if (notify) effects += TriggerEffect.Hide(reason)
        return s.copy(
            phase = Phase.Idle,
            armedUntilMs = null,
            manual = false,
            suppressedUntilScreenOn = s.suppressedUntilScreenOn || suppress,
        )
    }

    private fun trackOrientation(
        s: TriggerState,
        gravity: Gravity,
        nowMs: Long,
        settings: StandbySettings,
    ): TriggerState {
        val tilt = OrientationClassifier.tiltFromVerticalDegrees(gravity) ?: return s
        val onSide = tilt < settings.maxTiltDegrees
        val clearlyOff = tilt > settings.maxTiltDegrees + EXIT_HYSTERESIS_DEGREES
        val next = when {
            onSide -> s.copy(onSideSinceMs = s.onSideSinceMs ?: nowMs, offSideSinceMs = null)
            clearlyOff -> s.copy(onSideSinceMs = null, offSideSinceMs = s.offSideSinceMs ?: nowMs)
            // Zona de histéresis: no es estable ni para entrar ni para salir.
            else -> s.copy(onSideSinceMs = null, offSideSinceMs = null)
        }
        return next.copy(lastTiltDegrees = tilt)
    }

    private fun evaluate(
        s: TriggerState,
        nowMs: Long,
        settings: StandbySettings,
        effects: MutableList<TriggerEffect>,
    ): TriggerState = when (s.phase) {
        Phase.Idle -> s

        Phase.Armed -> {
            val stableOnSide = s.onSideSinceMs?.let { nowMs - it >= ENTER_STABLE_MS } == true
            when {
                stableOnSide && canShow(s, settings) -> {
                    effects += TriggerEffect.Show
                    s.copy(phase = Phase.Showing, armedUntilMs = null)
                }
                s.armedUntilMs != null && nowMs >= s.armedUntilMs ->
                    s.copy(phase = Phase.Idle, armedUntilMs = null)
                else -> s
            }
        }

        Phase.Showing -> when {
            s.manual -> s
            s.offSideSinceMs?.let { nowMs - it >= EXIT_STABLE_MS } == true ->
                hide(s, effects, "sacado del soporte", suppress = false)
            !powerAllowed(s, settings) ->
                hide(s, effects, "sin carga o poca batería", suppress = false)
            else -> s
        }
    }

    private fun canShow(s: TriggerState, settings: StandbySettings): Boolean =
        !s.suppressedUntilScreenOn && powerAllowed(s, settings)

    private fun powerAllowed(s: TriggerState, settings: StandbySettings): Boolean = when {
        s.charging -> true
        settings.requireCharging -> false
        else -> s.batteryPercent >= settings.minBatteryPercentOffCharger
    }

    /**
     * El sensor hace falta mientras esperamos la posición o mientras el modo está en
     * pantalla (para darse cuenta de que lo levantaron). El wake lock, solo mientras
     * esperamos con la pantalla apagada: con el modo en pantalla el teléfono ya está despierto.
     */
    private fun reconcileResources(s: TriggerState, effects: MutableList<TriggerEffect>): TriggerState {
        val wantSensor = s.phase == Phase.Armed || (s.phase == Phase.Showing && !s.manual)
        val wantWakeLock = s.phase == Phase.Armed
        var next = s
        if (wantSensor != s.sensorActive) {
            effects += TriggerEffect.SetSensorActive(wantSensor)
            // Marcas de tiempo viejas sin lecturas frescas no sirven.
            next = next.copy(sensorActive = wantSensor, onSideSinceMs = null, offSideSinceMs = null)
        }
        if (wantWakeLock != s.wakeLockHeld) {
            effects += TriggerEffect.HoldWakeLock(wantWakeLock)
            next = next.copy(wakeLockHeld = wantWakeLock)
        }
        return next
    }
}
