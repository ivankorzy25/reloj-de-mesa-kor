package com.realfamousbae.standby.dock.standby

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.hardware.SensorManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.realfamousbae.standby.dock.core.trigger.TriggerEvent
import com.realfamousbae.standby.dock.graph
import com.realfamousbae.standby.dock.notifications.BannerNotification
import com.realfamousbae.standby.dock.sensors.LightSensor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * El modo a pantalla completa, por encima de la pantalla de bloqueo.
 *
 * La Activity no decide nada por su cuenta: si se muestra o no lo decide el servicio
 * (TriggerEngine), y se cierra sola apenas `controller.visible` pasa a false. Irse de la
 * pantalla por cualquier motivo (un toque, el botón "atrás") se le avisa al servicio como
 * [TriggerEvent.UserExit].
 *
 * Las banderas showWhenLocked/turnScreenOn están en el manifiesto y también en el código:
 * las del manifiesto actúan al crear la ventana, y las del código son la garantía en
 * cualquier firmware.
 */
class StandbyActivity : ComponentActivity() {

    private val controller get() = graph.controller

    /** Modo noche prendido: lo decide la pantalla y lo usa el control de brillo. */
    private val nightActive = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemBars()
        // Brillo mínimo de entrada: si no, hasta la primera lectura del sensor de luz (~0,25 s)
        // la pantalla destella con el brillo del sistema, y en un cuarto oscuro encandila.
        window.attributes = window.attributes.apply { screenBrightness = graph.settings.value.brightnessMin }

        // Brillo de la ventana: independiente del sistema, según el sensor de luz.
        val brightness = BrightnessController(
            window = window,
            lightSensor = LightSensor(getSystemService(SensorManager::class.java)),
            settings = graph.settings,
            night = nightActive,
        )
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) { brightness.run() }
        }
        // El servicio decidió esconder el modo: nos cerramos.
        lifecycleScope.launch {
            controller.visible.first { !it }
            finish()
        }
        graph.diagLog.log("Modo: mostrado")

        setContent {
            val settings by graph.settings.collectAsStateWithLifecycle()
            val widgetId = settings.widgetId?.takeIf { graph.widgetHost.info(it) != null }
            val lux by brightness.lux.collectAsStateWithLifecycle()
            val night = rememberNightActive(settings, lux)
            // El controlador de brillo necesita saberlo para bajar el tope de noche.
            LaunchedEffect(night) { nightActive.value = night }
            val style = rememberClockStyle(settings, night = night)
            val live by graph.liveActivities.current.collectAsStateWithLifecycle()
            StandbyScreen(
                settings = settings,
                style = style,
                widgetId = widgetId,
                widgetHost = graph.widgetHost,
                immich = graph.immich,
                weather = graph.weather,
                nowPlaying = graph.nowPlaying,
                banners = graph.notificationBus.banners,
                live = live,
                onOpenNotification = ::openNotification,
                onFaceChange = { face ->
                    if (face != graph.settings.value.clockFace) {
                        lifecycleScope.launch { graph.settingsRepository.setClockFace(face) }
                    }
                },
                onCardChange = { card ->
                    if (card != graph.settings.value.sideCard) {
                        lifecycleScope.launch { graph.settingsRepository.setSideCard(card) }
                    }
                },
                onCard2Change = { card ->
                    if (card != graph.settings.value.sideCard2) {
                        lifecycleScope.launch { graph.settingsRepository.setSideCard2(card) }
                    }
                },
                onExit = { finish() },
            )
        }
    }

    /**
     * Tocar un aviso abre la app donde llegó. Si el teléfono está bloqueado, se le pide al
     * sistema que lo desbloquee primero (es el sistema el que pide el patrón o la huella).
     */
    private fun openNotification(banner: BannerNotification) {
        val intent = banner.contentIntent
        if (intent == null) {
            graph.diagLog.log("Aviso: la notificación no traía nada para abrir")
            return
        }
        val abrir = {
            runCatching { intent.send() }
                .onFailure { graph.diagLog.log("Aviso: no se pudo abrir (${it.javaClass.simpleName})") }
            finish()
        }
        val keyguard = getSystemService(KeyguardManager::class.java)
        if (keyguard?.isKeyguardLocked == true) {
            keyguard.requestDismissKeyguard(
                this,
                object : KeyguardManager.KeyguardDismissCallback() {
                    override fun onDismissSucceeded() = abrir()
                },
            )
        } else {
            abrir()
        }
    }

    override fun onStart() {
        super.onStart()
        // Los widgets se actualizan solo mientras el host escucha.
        graph.widgetHost.startListening()
    }

    override fun onStop() {
        graph.widgetHost.stopListening()
        super.onStop()
    }

    override fun onDestroy() {
        // Se cerró sin que lo decidiera el servicio (un toque, "atrás", el sistema): avisamos.
        // Si la pantalla estaba prendida o no (toque o botón de encendido) lo decide el servicio.
        if (!isChangingConfigurations && controller.visible.value) {
            controller.setVisible(false)
            controller.send(TriggerEvent.UserExit())
        }
        graph.diagLog.log("Modo: cerrado")
        super.onDestroy()
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    companion object {
        fun intent(context: Context): Intent =
            Intent(context, StandbyActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
