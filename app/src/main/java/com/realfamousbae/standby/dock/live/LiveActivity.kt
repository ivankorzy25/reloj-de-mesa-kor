package com.realfamousbae.standby.dock.live

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.service.notification.StatusBarNotification
import android.view.View
import android.view.ViewGroup
import android.widget.Chronometer
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.compose.ui.graphics.ImageBitmap

/**
 * Lo que el StandBy del iPhone llama "actividad en vivo": algo que está pasando ahora y que
 * se merece toda la pantalla. Acá salen de las notificaciones del sistema (una llamada, un
 * temporizador) o de la sesión de medios (la música, que la arma la pantalla).
 */
sealed interface LiveActivity {

    /** Una llamada: entrando (con atender y rechazar) o en curso (solo colgar). */
    data class Call(
        val name: String,
        val app: String,
        val icon: ImageBitmap?,
        val incoming: Boolean,
        val answer: PendingIntent?,
        val hangUp: PendingIntent?,
    ) : LiveActivity

    /** Un temporizador o un cronómetro del reloj del sistema. */
    data class Timer(
        val label: String,
        val app: String,
        /** Momento al que cuenta (si va para atrás) o desde el que cuenta (si va para adelante). */
        val baseMs: Long,
        val countDown: Boolean,
        val actions: List<Action>,
    ) : LiveActivity

    /** Lo que está sonando, con la carátula grande. */
    data object Music : LiveActivity

    data class Action(val title: String, val intent: PendingIntent)
}

/**
 * Qué notificaciones son una actividad en vivo. Se mira el `category` y el cronómetro, que
 * son parte de la API pública de [Notification]; las llamadas además traen sus intenciones
 * de atender y colgar en los extras del estilo [Notification.CallStyle].
 */
object LiveActivityParser {

    /** Claves del CallStyle: públicas en la documentación, pero sin constante en el SDK. */
    private const val EXTRA_ANSWER = "android.answerIntent"
    private const val EXTRA_DECLINE = "android.declineIntent"
    private const val EXTRA_HANG_UP = "android.hangUpIntent"

    /** Palabras con las que las apps llaman a esos botones, por si no vienen en los extras. */
    private val ANSWER_WORDS = listOf("atender", "contestar", "responder", "answer", "accept")
    private val HANG_UP_WORDS = listOf("rechazar", "colgar", "finalizar", "decline", "hang", "end")

    fun isCall(n: Notification): Boolean = n.category == Notification.CATEGORY_CALL

    /**
     * El cronómetro de una notificación: a qué momento cuenta y para qué lado.
     *
     * Hay dos formas de que una app lo diga. La de Android ([Notification.EXTRA_SHOW_CHRONOMETER])
     * la usan el reloj de Google y casi todos. Samsung, en cambio, mete un `Chronometer` de
     * verdad adentro de las vistas de la notificación y no prende esa bandera; por eso, si la
     * primera no está, se arman las vistas y se busca el reloj adentro.
     */
    data class Chrono(val baseWallMs: Long, val countDown: Boolean)

    fun chrono(context: Context, n: Notification): Chrono? {
        val extras = n.extras ?: Bundle()
        if (extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER, false) && n.`when` > 0L) {
            return Chrono(n.`when`, extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN, false))
        }
        return fromRemoteViews(context, n)
    }

    /**
     * Temporizador o cronómetro. Si cuenta para atrás alcanza con eso; si cuenta para
     * adelante se pide además que sea el reloj del teléfono, para no agrandar cualquier
     * notificación que tenga un cronómetro adentro (una grabadora, una llamada de otra app).
     */
    fun isTimer(context: Context, sbn: StatusBarNotification): Boolean {
        val n = sbn.notification
        if (isCall(n)) return false
        val chrono = chrono(context, n) ?: return false
        if (chrono.countDown) return true
        return n.category == Notification.CATEGORY_ALARM ||
            n.category == Notification.CATEGORY_STOPWATCH ||
            "clock" in sbn.packageName
    }

    private fun fromRemoteViews(context: Context, n: Notification): Chrono? {
        val candidates = buildList {
            n.bigContentView?.let(::add)
            n.contentView?.let(::add)
            n.headsUpContentView?.let(::add)
            @Suppress("DEPRECATION")
            n.extras?.let { bundle ->
                bundle.keySet().forEach { key -> (bundle.get(key) as? RemoteViews)?.let(::add) }
            }
        }
        for (views in candidates) {
            val chronometer = runCatching {
                // Armar las vistas de otra app puede fallar (recursos que no están, layouts
                // que no se entienden): si falla, se prueba con las siguientes.
                findChronometer(views.apply(context, FrameLayout(context)))
            }.getOrNull() ?: continue
            // El Chronometer cuenta con el reloj de "desde que prendió el teléfono"; lo
            // pasamos al reloj de pared, que es con el que trabaja el resto de la app.
            val base = System.currentTimeMillis() + (chronometer.base - SystemClock.elapsedRealtime())
            return Chrono(base, chronometer.isCountDown)
        }
        return null
    }

    private fun findChronometer(view: View): Chronometer? = when {
        view is Chronometer -> view
        view is ViewGroup -> (0 until view.childCount).firstNotNullOfOrNull { findChronometer(view.getChildAt(it)) }
        else -> null
    }

    fun call(
        sbn: StatusBarNotification,
        appName: String,
        icon: ImageBitmap?,
        fallbackName: String,
    ): LiveActivity.Call {
        val n = sbn.notification
        val extras = n.extras ?: Bundle()
        val answer = extras.pendingIntent(EXTRA_ANSWER) ?: action(n, ANSWER_WORDS)?.actionIntent
        val hangUp = extras.pendingIntent(EXTRA_HANG_UP)
            ?: extras.pendingIntent(EXTRA_DECLINE)
            ?: action(n, HANG_UP_WORDS)?.actionIntent
        val name = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.takeIf { it.isNotBlank() }
            ?: fallbackName
        return LiveActivity.Call(
            name = name,
            app = appName,
            icon = icon,
            // Entrando = todavía se puede atender; en curso solo queda colgar.
            incoming = answer != null,
            answer = answer,
            hangUp = hangUp,
        )
    }

    fun timer(
        context: Context,
        sbn: StatusBarNotification,
        appName: String,
        fallbackLabel: String,
    ): LiveActivity.Timer? {
        val n = sbn.notification
        val extras = n.extras ?: Bundle()
        val chrono = chrono(context, n) ?: return null
        return LiveActivity.Timer(
            label = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.takeIf { it.isNotBlank() }
                ?: fallbackLabel,
            app = appName,
            baseMs = chrono.baseWallMs,
            countDown = chrono.countDown,
            // Como mucho dos botones: en la pantalla del modo no entra más, y con más de dos
            // nadie sabe cuál tocar de lejos.
            actions = n.actions.orEmpty().mapNotNull { a ->
                val title = a.title?.toString() ?: return@mapNotNull null
                a.actionIntent?.let { LiveActivity.Action(title, it) }
            }.take(2),
        )
    }

    private fun action(n: Notification, words: List<String>): Notification.Action? =
        n.actions.orEmpty().firstOrNull { a ->
            val title = a.title?.toString()?.lowercase().orEmpty()
            words.any { it in title } && a.actionIntent != null
        }

    @Suppress("DEPRECATION")
    private fun Bundle.pendingIntent(key: String): PendingIntent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) getParcelable(key, PendingIntent::class.java)
        else getParcelable(key)
}
