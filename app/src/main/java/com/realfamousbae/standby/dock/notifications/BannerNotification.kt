package com.realfamousbae.standby.dock.notifications

import android.app.PendingIntent
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Qué mostrar en el aviso. El texto ya viene recortado según las reglas de privacidad de la
 * pantalla de bloqueo. [contentIntent] es lo que abre la app al tocarlo, si la notificación
 * trae algo para abrir.
 */
data class BannerNotification(
    val key: String,
    val appName: String,
    val icon: ImageBitmap?,
    val title: String?,
    val text: String?,
    val contentIntent: PendingIntent? = null,
)

/**
 * El canal entre el oyente de notificaciones y la pantalla del modo. Buffer chico: si las
 * notificaciones llegan en manojo, las viejas se tiran (mostrar diez avisos seguidos no sirve).
 */
class NotificationBus {
    private val _banners = MutableSharedFlow<BannerNotification>(
        extraBufferCapacity = 3,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val banners: SharedFlow<BannerNotification> = _banners.asSharedFlow()

    fun post(banner: BannerNotification) {
        _banners.tryEmit(banner)
    }
}
