package com.realfamousbae.standby.dock.core.trigger

/** Lo que entra al TriggerEngine. El servicio traduce acá los eventos del sistema y los sensores. */
sealed interface TriggerEvent {
    data object ScreenOff : TriggerEvent
    data object ScreenOn : TriggerEvent

    /** Iván desbloqueó el teléfono. */
    data object UserPresent : TriggerEvent

    /** Estado de la energía (llega seguido, los repetidos no molestan). */
    data class Power(val charging: Boolean, val batteryPercent: Int) : TriggerEvent

    data class GravityReading(val gravity: Gravity) : TriggerEvent

    /** Saltó el sensor de movimiento con la pantalla apagada: conviene revisar la posición. */
    data object MotionWake : TriggerEvent

    /** Sonó el temporizador que pidió el efecto ScheduleTick. */
    data object Tick : TriggerEvent

    /**
     * El modo se cerró sin que lo decidiera el servicio (un toque, "atrás", la Activity se fue).
     * [screenOn] = si la pantalla estaba prendida en ese momento (el servicio pasa `isInteractive`):
     * false quiere decir que lo apagaron con el botón de encendido antes de que llegara SCREEN_OFF.
     */
    data class UserExit(val screenOn: Boolean = true) : TriggerEvent

    /** Mostrar el modo ya, sin revisar condiciones (botón de prueba, mosaico de la cortina). */
    data object ManualShow : TriggerEvent
}

/** Lo que el servicio tiene que hacer. El TriggerEngine no llama a nada por su cuenta. */
sealed interface TriggerEffect {
    data class SetSensorActive(val active: Boolean) : TriggerEffect
    data class HoldWakeLock(val hold: Boolean) : TriggerEffect
    data class ScheduleTick(val atMs: Long) : TriggerEffect
    data object Show : TriggerEffect
    data class Hide(val reason: String) : TriggerEffect
}

enum class Phase {
    /** El teléfono se está usando (pantalla prendida) o no hay nada que esperar. */
    Idle,

    /** La pantalla se apagó: escuchamos el acelerómetro y esperamos que quede quieto de costado. */
    Armed,

    /** El modo está en pantalla. */
    Showing,
}

data class TriggerState(
    val phase: Phase = Phase.Idle,
    val charging: Boolean = false,
    val batteryPercent: Int = 100,
    val screenInteractive: Boolean = true,
    /** Fin de la ventana de prueba en la fase Armed; null = sin límite (cargando). */
    val armedUntilMs: Long? = null,
    /** Desde cuándo el teléfono está de costado sin interrupciones (para el rebote de entrada). */
    val onSideSinceMs: Long? = null,
    /** Desde cuándo está fuera de posición contando la histéresis (para el rebote de salida). */
    val offSideSinceMs: Long? = null,
    val lastTiltDegrees: Float? = null,
    /**
     * El modo se apagó con el botón de encendido: no esperar posición ni mostrar nada hasta
     * que la pantalla se vuelva a prender (o lo desbloqueen, o cambie la carga).
     */
    val suppressedUntilScreenOn: Boolean = false,
    /** El modo se mostró a mano: lo cierra solo Iván. */
    val manual: Boolean = false,
    // Qué tiene prendido el servicio ahora mismo, para emitir efectos solo cuando algo cambia.
    val sensorActive: Boolean = false,
    val wakeLockHeld: Boolean = false,
)
