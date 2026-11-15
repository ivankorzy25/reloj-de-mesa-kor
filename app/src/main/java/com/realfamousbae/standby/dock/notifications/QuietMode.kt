package com.realfamousbae.standby.dock.notifications

import android.app.AutomaticZenRule
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Build
import android.service.notification.Condition
import android.service.notification.ZenPolicy
import androidx.core.content.edit
import com.realfamousbae.standby.dock.diag.DiagLog
import com.realfamousbae.standby.dock.settings.SettingsActivity

/**
 * Nuestro propio "No molestar" (Modes de Android) mientras el modo está en pantalla: esconde
 * las notificaciones emergentes del sistema para que arriba de la pantalla estén solo
 * nuestros avisos.
 *
 * El sistema deja esconder la emergente solo de las notificaciones que intercepta el "No
 * molestar", así que mientras tanto las notificaciones comunes llegan sin sonido. Las
 * llamadas, las repetidas, las alarmas y los medios pasan igual. En la cortina, en los
 * globitos y en la barra de estado queda todo, y el oyente de notificaciones las sigue
 * recibiendo todas.
 *
 * Hace falta el acceso a "No molestar"; una app con acceso a notificaciones suele tenerlo
 * de entrada. Cualquier negativa del sistema se anota en el registro y no rompe el modo.
 */
class QuietMode(context: Context, private val log: DiagLog) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("quiet_mode", Context.MODE_PRIVATE)
    private val manager = appContext.getSystemService(NotificationManager::class.java)
    private val conditionId: Uri = Uri.Builder()
        .scheme(Condition.SCHEME)
        .authority(appContext.packageName)
        .appendPath("standby")
        .build()

    /** Nuestro modo está prendido ahora (según nuestra última llamada). */
    @Volatile
    var active: Boolean = false
        private set

    /** El "No molestar" ya lo tenía prendido Iván antes de que prendiéramos el nuestro. */
    @Volatile
    var userDndActive: Boolean = false
        private set

    fun hasAccess(): Boolean = manager.isNotificationPolicyAccessGranted

    /**
     * Prender o apagar el modo. Apagarlo no crea la regla si no existe, y se hace siempre
     * (también después de que el proceso se caiga y la regla haya quedado colgada).
     */
    fun setActive(value: Boolean) {
        runCatching {
            if (value) {
                if (!hasAccess()) {
                    log.log("Modo silencio: sin acceso a «No molestar»")
                    return
                }
                userDndActive = manager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
                val id = findRuleId() ?: manager.addAutomaticZenRule(newRule())
                manager.setAutomaticZenRuleState(id, condition(Condition.STATE_TRUE))
                active = true
                suppressPeekGlobally()
                log.log("Modo silencio: activado (${stateSummary(id)})")
            } else {
                active = false
                userDndActive = false
                if (!hasAccess()) return
                restoreGlobalPeek()
                val id = findRuleId() ?: return
                manager.setAutomaticZenRuleState(id, condition(Condition.STATE_FALSE))
                log.log("Modo silencio: apagado")
            }
        }.onFailure {
            log.log("Modo silencio: error (${it.javaClass.simpleName})")
        }
    }

    /**
     * One UI con la interfaz vieja de "No molestar" saca los efectos visuales de la política
     * general ("Ocultar notificaciones") y no del `ZenPolicy` de la regla. Por eso, mientras
     * dura el modo, agregamos a la política general solo "ocultar emergentes" y lo anotamos
     * en disco, para poder devolverlo aunque el proceso se muera.
     */
    private fun suppressPeekGlobally() {
        val policy = manager.notificationPolicy
        if (policy.suppressedVisualEffects and PEEK != 0) return
        manager.notificationPolicy = policy.withVisualEffects(policy.suppressedVisualEffects or PEEK)
        prefs.edit(commit = true) { putBoolean(KEY_PEEK_ADDED, true) }
    }

    /** Sacar el "ocultar emergentes" solo si lo pusimos nosotros; el resto no se toca. */
    private fun restoreGlobalPeek() {
        if (!prefs.getBoolean(KEY_PEEK_ADDED, false)) return
        val policy = manager.notificationPolicy
        if (policy.suppressedVisualEffects and PEEK != 0) {
            manager.notificationPolicy = policy.withVisualEffects(policy.suppressedVisualEffects and PEEK.inv())
        }
        prefs.edit(commit = true) { putBoolean(KEY_PEEK_ADDED, false) }
        log.log("Modo silencio: ajuste de emergentes restaurado")
    }

    private fun NotificationManager.Policy.withVisualEffects(effects: Int) = NotificationManager.Policy(
        priorityCategories,
        priorityCallSenders,
        priorityMessageSenders,
        effects,
        priorityConversationSenders,
    )

    /** Para el registro: qué considera prendido el sistema después de nuestra llamada. */
    private fun stateSummary(id: String): String {
        val filter = manager.currentInterruptionFilter
        val ruleState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            ", regla=${manager.getAutomaticZenRuleState(id)}"
        } else {
            ""
        }
        val peek = manager.notificationPolicy.suppressedVisualEffects and PEEK != 0
        return "filtro=$filter$ruleState, peek general=${if (peek) "oculta" else "visible"}"
    }

    private fun findRuleId(): String? =
        manager.automaticZenRules.entries.firstOrNull { it.value.conditionId == conditionId }?.key

    private fun condition(state: Int) = Condition(conditionId, RULE_NAME, state)

    private fun newRule(): AutomaticZenRule {
        val policy = ZenPolicy.Builder()
            .disallowAllSounds()
            .allowAlarms(true)
            .allowMedia(true)
            .allowCalls(ZenPolicy.PEOPLE_TYPE_ANYONE)
            .allowRepeatCallers(true)
            // Lo importante: sin notificaciones emergentes. Los demás efectos quedan como están,
            // para que las notificaciones no se pierdan de la cortina.
            .showPeeking(false)
            .showFullScreenIntent(true)
            .showInNotificationList(true)
            .showBadges(true)
            .showStatusBarIcons(true)
            .showLights(true)
            .showInAmbientDisplay(true)
            .build()
        val settingsActivity = ComponentName(appContext, SettingsActivity::class.java)
        val filter = NotificationManager.INTERRUPTION_FILTER_PRIORITY
        // El tipo y la descripción de la regla en Modes se ponen solo por el Builder (Android 15+).
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            AutomaticZenRule.Builder(RULE_NAME, conditionId)
                .setType(AutomaticZenRule.TYPE_OTHER)
                .setTriggerDescription("Mientras el reloj de mesa está en pantalla")
                .setConfigurationActivity(settingsActivity)
                .setZenPolicy(policy)
                .setInterruptionFilter(filter)
                .setEnabled(true)
                .build()
        } else {
            AutomaticZenRule(RULE_NAME, null, settingsActivity, conditionId, policy, filter, true)
        }
    }

    private companion object {
        const val RULE_NAME = "Reloj de Mesa KOR"
        const val PEEK = NotificationManager.Policy.SUPPRESSED_EFFECT_PEEK
        const val KEY_PEEK_ADDED = "global_peek_added"
    }
}
