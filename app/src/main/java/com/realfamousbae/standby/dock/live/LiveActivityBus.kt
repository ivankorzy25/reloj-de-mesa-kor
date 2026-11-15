package com.realfamousbae.standby.dock.live

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Las actividades en vivo que hay ahora, puestas por el oyente de notificaciones y leídas por
 * la pantalla del modo. Vive en AppGraph (lo mismo que el proceso) y no guarda nada en disco:
 * cuando la llamada o el temporizador se terminan, desaparecen solos.
 *
 * Una llamada siempre le gana a un temporizador: es lo más urgente que puede pasar.
 */
class LiveActivityBus(scope: CoroutineScope) {

    private val items = MutableStateFlow<Map<String, LiveActivity>>(emptyMap())

    /** Forzada por adb para sacar capturas y grabar el video; no se guarda en ningún lado. */
    private val demo = MutableStateFlow<LiveActivity?>(null)

    val current: StateFlow<LiveActivity?> = combine(demo, items) { forced, map ->
        forced
            ?: map.values.firstOrNull { it is LiveActivity.Call }
            ?: map.values.firstOrNull()
    }.stateIn(scope, SharingStarted.Eagerly, null)

    fun put(key: String, activity: LiveActivity) {
        items.value = items.value + (key to activity)
    }

    fun remove(key: String) {
        if (items.value.containsKey(key)) items.value = items.value - key
    }

    fun setDemo(activity: LiveActivity?) {
        demo.value = activity
    }
}
