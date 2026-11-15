package com.realfamousbae.standby.dock.media

import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.app.NotificationManagerCompat
import com.realfamousbae.standby.dock.notifications.StandbyNotificationListener

/** Lo que está sonando, para la tarjeta. */
data class NowPlaying(
    val title: String,
    val artist: String?,
    val app: String,
    val playing: Boolean,
    val art: ImageBitmap?,
)

/**
 * Lee las sesiones de medios activas (Spotify, YouTube Music, lo que sea) usando el mismo
 * permiso de acceso a notificaciones que ya pide la app. Si no está dado, no hay nada que
 * mostrar y la tarjeta lo dice.
 */
class NowPlayingSource(private val context: Context) {

    private val manager: MediaSessionManager? =
        context.getSystemService(MediaSessionManager::class.java)

    /** El permiso de acceso a notificaciones es el mismo que habilita leer los medios. */
    fun hasAccess(): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    private fun controllers(): List<MediaController> = runCatching {
        manager?.getActiveSessions(StandbyNotificationListener.component(context)).orEmpty()
    }.getOrDefault(emptyList())

    /** La sesión que manda: la que está sonando; si ninguna suena, la última que haya. */
    private fun active(): MediaController? {
        val list = controllers()
        return list.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: list.firstOrNull {
                it.playbackState?.state in setOf(PlaybackState.STATE_PAUSED, PlaybackState.STATE_BUFFERING)
            }
    }

    /** @param previous estado anterior, para no volver a convertir la misma carátula. */
    fun read(previous: NowPlaying?): NowPlaying? {
        val controller = active() ?: return null
        val metadata = controller.metadata ?: return null
        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE)?.takeIf { it.isNotBlank() }
            ?: return null
        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
        val playing = controller.playbackState?.state == PlaybackState.STATE_PLAYING
        val app = appLabel(controller.packageName)
        val art = if (previous?.title == title && previous.app == app) {
            previous.art
        } else {
            val bitmap = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
            bitmap?.asImageBitmap()
        }
        return NowPlaying(title = title, artist = artist, app = app, playing = playing, art = art)
    }

    fun playPause() {
        val controller = active() ?: return
        if (controller.playbackState?.state == PlaybackState.STATE_PLAYING) {
            controller.transportControls.pause()
        } else {
            controller.transportControls.play()
        }
    }

    fun next() {
        active()?.transportControls?.skipToNext()
    }

    fun previous() {
        active()?.transportControls?.skipToPrevious()
    }

    private fun appLabel(packageName: String?): String {
        packageName ?: return "Música"
        val pm = context.packageManager
        return runCatching { pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString() }
            .getOrDefault(packageName)
    }
}
