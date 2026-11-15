package com.realfamousbae.standby.dock.core.run

/** Cómo terminó el proceso anterior de la app, en lo que importa para el arranque automático. */
enum class ExitKind {
    /** Lo paró Iván a propósito (Forzar detención, "Detener" en el administrador de tareas). */
    UserStopped,

    /** Todo lo demás: falta de memoria, una falla, sacarlo de "Recientes", una actualización. */
    Other,
}

/** Último dato de cierre del proceso (ApplicationExitInfo, en la capa :app). */
data class LastExit(val kind: ExitKind, val timestampMs: Long)

/** Qué hacer al arrancar el proceso. [markHandledMs] = guardar como salida "ya contada". */
data class ExitDecision(val disable: Boolean, val markHandledMs: Long?)

/**
 * Reglas de "si hay que trabajar en segundo plano". Que paren la app desde afuera cuenta como
 * apagado normal: el interruptor queda en "Apagado" y ningún camino automático (el worker, el
 * arranque, el servicio reiniciado por el sistema, abrir la app) la levanta hasta que Iván la prenda.
 */
object RunPolicy {

    /**
     * Para Android, `REASON_USER_REQUESTED` significa tanto parar la app (Forzar detención:
     * "stop <pkg> due to from pid N"; "Detener" en el administrador de tareas: "fully stop
     * <pkg>/0 by user request"; limpiar memoria: "kill background") como sacarla de "Recientes"
     * ("remove task"), y las subrazones (`SUBREASON_*`) no están en la API pública. Por eso
     * cualquier `USER_REQUESTED` se toma como una parada, salvo los casos reconocidos de
     * "Recientes" y de actualización del paquete (en API 31–32 la actualización también llega
     * como USER_REQUESTED, con "due to update pkg" o "replace pkg").
     * Los textos salen de ActivityManagerService (AOSP).
     */
    fun classify(reasonIsUserRequested: Boolean, description: String?): ExitKind {
        if (!reasonIsUserRequested) return ExitKind.Other
        val d = description?.lowercase().orEmpty()
        return if (NOT_A_STOP_MARKERS.any { it in d }) ExitKind.Other else ExitKind.UserStopped
    }

    /**
     * La decisión al arrancar el proceso. Cada salida se cuenta una sola vez: después de que
     * Iván vuelve a prender el modo, el Forzar detención viejo no lo apaga de nuevo.
     */
    fun onProcessStart(lastExit: LastExit?, lastHandledExitMs: Long): ExitDecision {
        if (lastExit == null || lastExit.timestampMs <= lastHandledExitMs) return ExitDecision(false, null)
        return ExitDecision(disable = lastExit.kind == ExitKind.UserStopped, markHandledMs = lastExit.timestampMs)
    }

    /** Si se puede levantar el servicio solo (no por una acción directa de Iván). */
    fun autoStartAllowed(enabled: Boolean): Boolean = enabled

    private val NOT_A_STOP_MARKERS = listOf("remove task", "update pkg", "replace pkg")
}
