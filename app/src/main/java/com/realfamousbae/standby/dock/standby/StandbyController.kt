package com.realfamousbae.standby.dock.standby

import com.realfamousbae.standby.dock.core.trigger.TriggerEvent
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * El puente entre el servicio (que es dueño del TriggerEngine) y la interfaz (la Activity del
 * modo, la pantalla de ajustes).
 * - El servicio pone [visible]: la Activity se cierra sola apenas pasa a false.
 * - La interfaz manda lo que hace Iván a [userEvents], y el servicio se lo pasa al TriggerEngine.
 * Vive en AppGraph, o sea lo mismo que el proceso.
 */
class StandbyController {

    private val _visible = MutableStateFlow(false)
    val visible: StateFlow<Boolean> = _visible.asStateFlow()

    private val _userEvents = MutableSharedFlow<TriggerEvent>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val userEvents: SharedFlow<TriggerEvent> = _userEvents.asSharedFlow()

    fun setVisible(value: Boolean) {
        _visible.value = value
    }

    fun send(event: TriggerEvent) {
        _userEvents.tryEmit(event)
    }
}
