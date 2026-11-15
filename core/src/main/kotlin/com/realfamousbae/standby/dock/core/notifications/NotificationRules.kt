package com.realfamousbae.standby.dock.core.notifications

import com.realfamousbae.standby.dock.core.prefs.StandbySettings

/**
 * Lo que hace falta saber de una notificación para decidir si se muestra el aviso. La capa
 * :app lo llena desde StatusBarNotification y Ranking; los valores de importancia y de
 * visibilidad son los de Android
 * (`NotificationManager.IMPORTANCE_*`, `Notification.VISIBILITY_*`).
 */
data class NotificationInfo(
    val packageName: String,
    val isOwnPackage: Boolean,
    /** Fija (ongoing) o de un servicio en primer plano: música, descargas, navegación. */
    val isOngoing: Boolean,
    val isGroupSummary: Boolean,
    val importance: Int,
    /** false = la notificación la tapó el modo "No molestar". */
    val matchesInterruptionFilter: Boolean,
    /**
     * Está activo nuestro propio modo de silencio (el que tapa las notificaciones emergentes
     * del sistema). Como intercepta casi todo, por sí solo no tiene que apagar nuestros avisos.
     */
    val ownQuietModeActive: Boolean = false,
    /** "No molestar" ya estaba prendido por Iván antes de nuestro modo. */
    val userDndActive: Boolean = false,
)

object NotificationFilter {

    /** `NotificationManager.IMPORTANCE_DEFAULT`: más abajo son notificaciones sin sonido. */
    const val IMPORTANCE_DEFAULT = 3

    fun shouldShow(info: NotificationInfo, settings: StandbySettings): Boolean = when {
        !settings.notificationBannersEnabled -> false
        info.isOwnPackage -> false
        info.isOngoing -> false
        info.isGroupSummary -> false
        info.importance < IMPORTANCE_DEFAULT -> false
        // La intercepción cuenta solo si la pudo hacer el "No molestar" de Iván.
        !info.matchesInterruptionFilter && (!info.ownQuietModeActive || info.userDndActive) -> false
        info.packageName in settings.notificationBlockedPackages -> false
        else -> true
    }
}

/** Qué parte de la notificación se puede mostrar sobre la pantalla de bloqueo. */
enum class BannerContent {
    /** Ícono, app, título y texto. */
    Full,

    /** La versión pública que la app preparó para la pantalla de bloqueo. */
    PublicVersion,

    /** Solo el ícono y el nombre de la app. */
    AppOnly,

    /** No mostrar nada. */
    Hidden,
}

/**
 * La decisión "como la toma el sistema": repite las reglas de la pantalla de bloqueo de Android.
 */
object LockscreenPrivacy {

    const val VISIBILITY_SECRET = -1
    const val VISIBILITY_PRIVATE = 0
    const val VISIBILITY_PUBLIC = 1

    /** `Ranking.getLockscreenVisibility()` cuando ni el canal ni Iván cambiaron nada. */
    const val VISIBILITY_NO_OVERRIDE = -1000

    /**
     * @param visibility el campo `Notification.visibility` (PRIVATE por defecto).
     * @param channelOverride lo que cambió el canal, vía Ranking, o [VISIBILITY_NO_OVERRIDE].
     * @param systemAllowsPrivate el ajuste del sistema "mostrar el contenido en la pantalla de
     *   bloqueo"; null = no se pudo leer, y damos por hecho que el contenido va tapado.
     */
    fun decide(
        visibility: Int,
        channelOverride: Int,
        systemAllowsPrivate: Boolean?,
        hasPublicVersion: Boolean,
    ): BannerContent {
        // Lo que diga el canal solo puede tapar más, nunca menos, igual que en SystemUI.
        val effective = if (channelOverride == VISIBILITY_NO_OVERRIDE) visibility else minOf(visibility, channelOverride)
        return when {
            effective <= VISIBILITY_SECRET -> BannerContent.Hidden
            effective >= VISIBILITY_PUBLIC -> BannerContent.Full
            systemAllowsPrivate == true -> BannerContent.Full
            hasPublicVersion -> BannerContent.PublicVersion
            else -> BannerContent.AppOnly
        }
    }
}

/**
 * Corta las publicaciones repetidas de la misma notificación sin cambios (progreso,
 * actualizaciones de medios, apps que la vuelven a poner). Guarda las últimas [capacity] claves.
 */
class BannerDedupe(private val capacity: Int = 64) {

    private val seen = LinkedHashMap<String, Int>()

    /** true = mostrar (es nueva o le cambió el contenido). */
    fun accept(key: String, title: String?, text: String?): Boolean {
        val hash = (title.orEmpty() + '\u0000' + text.orEmpty()).hashCode()
        val previous = seen.remove(key)
        seen[key] = hash
        if (seen.size > capacity) seen.remove(seen.keys.first())
        return previous != hash
    }

    fun forget(key: String) {
        seen.remove(key)
    }
}
