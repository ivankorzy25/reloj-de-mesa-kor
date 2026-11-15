package com.realfamousbae.standby.dock.notifications

import android.app.Notification
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.realfamousbae.standby.dock.core.notifications.BannerContent
import com.realfamousbae.standby.dock.core.notifications.BannerDedupe
import com.realfamousbae.standby.dock.core.notifications.LockscreenPrivacy
import com.realfamousbae.standby.dock.core.notifications.NotificationFilter
import com.realfamousbae.standby.dock.core.notifications.NotificationInfo
import com.realfamousbae.standby.dock.graph
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Oyente de notificaciones: mientras el modo está en pantalla, convierte las notificaciones
 * nuevas en avisos. Las decisiones (qué mostrar y cuánto texto) están en :core
 * ([NotificationFilter], [LockscreenPrivacy]). Las que ya estaban colgadas al conectarse
 * no se vuelven a mostrar.
 *
 * En el registro no se escriben ni los textos ni los nombres de las apps: solo que hubo un aviso.
 */
class StandbyNotificationListener : NotificationListenerService() {

    private val dedupe = BannerDedupe()
    private var enabledWatch: Job? = null

    override fun onListenerConnected() {
        // Modo apagado: nos desvinculamos para que el sistema no sostenga el proceso por el
        // oyente. Al prenderlo de nuevo, RunState llama a requestRebind.
        enabledWatch?.cancel()
        enabledWatch = graph.appScope.launch {
            graph.settingsRepository.settings.first { !it.enabled }
            graph.diagLog.log("Notificaciones: el reloj está apagado, oyente desconectado")
            runCatching { requestUnbind() }
        }
        val privacy = when (systemAllowsPrivateNotifications(this)) {
            true -> "mostrar"
            false -> "ocultar"
            null -> "no se lee, se oculta"
        }
        graph.diagLog.log("Notificaciones: oyente conectado; contenido en bloqueo: $privacy")
    }

    override fun onListenerDisconnected() {
        enabledWatch?.cancel()
        enabledWatch = null
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        dedupe.forget(sbn.key)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification, rankingMap: RankingMap) {
        val graph = graph
        val n = sbn.notification
        val extras = n.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: extras.getCharSequence(Notification.EXTRA_TEXT))
            ?.toString()
        // Lo recordamos siempre, incluso con el modo escondido: si no, actualizar una
        // notificación vieja con el modo en pantalla parecería una nueva.
        val fresh = dedupe.accept(sbn.key, title, text)
        if (!graph.controller.visible.value || !fresh) return

        val ranking = Ranking().takeIf { rankingMap.getRanking(sbn.key, it) }
        val info = NotificationInfo(
            packageName = sbn.packageName,
            isOwnPackage = sbn.packageName == packageName,
            isOngoing = sbn.isOngoing || n.flags and Notification.FLAG_FOREGROUND_SERVICE != 0,
            isGroupSummary = n.flags and Notification.FLAG_GROUP_SUMMARY != 0,
            importance = ranking?.importance ?: NotificationFilter.IMPORTANCE_DEFAULT,
            matchesInterruptionFilter = ranking?.matchesInterruptionFilter() ?: true,
            ownQuietModeActive = graph.quietMode.active,
            userDndActive = graph.quietMode.userDndActive,
        )
        // Diagnóstico de las emergentes del sistema: si las interceptó el No molestar y si se
        // sacó el peek. Sin texto ni nombre de app.
        if (ranking != null) {
            val peekHidden = ranking.suppressedVisualEffects and NotificationManager.Policy.SUPPRESSED_EFFECT_PEEK != 0
            graph.diagLog.log(
                "Notificaciones: dnd=${if (ranking.matchesInterruptionFilter()) "dejó pasar" else "interceptó"}, " +
                    "popup=${if (peekHidden) "oculta" else "permitida"}",
            )
        }
        if (!NotificationFilter.shouldShow(info, graph.settings.value)) return

        val content = LockscreenPrivacy.decide(
            visibility = n.visibility,
            channelOverride = ranking?.lockscreenVisibilityOverride ?: LockscreenPrivacy.VISIBILITY_NO_OVERRIDE,
            systemAllowsPrivate = systemAllowsPrivateNotifications(this),
            hasPublicVersion = n.publicVersion != null,
        )
        val (shownTitle, shownText) = when (content) {
            BannerContent.Hidden -> return
            BannerContent.Full -> title to text
            BannerContent.AppOnly -> null to null
            BannerContent.PublicVersion -> n.publicVersion.extras.let {
                it.getCharSequence(Notification.EXTRA_TITLE)?.toString() to
                    it.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            }
        }

        val pm = packageManager
        val app = runCatching { pm.getApplicationInfo(sbn.packageName, 0) }.getOrNull()
        val icon = app?.let { runCatching { pm.getApplicationIcon(it).toBitmap(96, 96).asImageBitmap() }.getOrNull() }
        graph.notificationBus.post(
            BannerNotification(
                key = sbn.key,
                appName = app?.let { pm.getApplicationLabel(it).toString() } ?: sbn.packageName,
                icon = icon,
                title = shownTitle,
                text = shownText,
                // Tocar el aviso abre la app en la conversación que corresponde.
                contentIntent = n.contentIntent,
            ),
        )
        graph.diagLog.log("Notificaciones: aviso mostrado")
    }

    companion object {
        fun component(context: Context) = ComponentName(context, StandbyNotificationListener::class.java)

        /**
         * El ajuste del sistema "Mostrar el contenido de las notificaciones en la pantalla de
         * bloqueo". La clave no es pública: si no está o el firmware no deja leerla, devuelve
         * null (y damos por hecho que va tapado). Qué pasó en el teléfono se ve en el registro.
         */
        fun systemAllowsPrivateNotifications(context: Context): Boolean? = try {
            Settings.Secure.getInt(context.contentResolver, "lock_screen_allow_private_notifications") != 0
        } catch (_: Exception) {
            null
        }
    }
}
