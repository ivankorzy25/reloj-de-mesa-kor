package com.realfamousbae.standby.dock

import android.app.Application
import android.content.Context
import com.realfamousbae.standby.dock.core.prefs.StandbySettings
import com.realfamousbae.standby.dock.diag.DiagLog
import com.realfamousbae.standby.dock.notifications.NotificationBus
import com.realfamousbae.standby.dock.media.NowPlayingSource
import com.realfamousbae.standby.dock.notifications.QuietMode
import com.realfamousbae.standby.dock.photos.ImmichClient
import com.realfamousbae.standby.dock.photos.ImmichSeed
import com.realfamousbae.standby.dock.prefs.SettingsRepository
import com.realfamousbae.standby.dock.service.Notifications
import com.realfamousbae.standby.dock.service.RunState
import com.realfamousbae.standby.dock.standby.StandbyController
import com.realfamousbae.standby.dock.weather.WeatherRepository
import com.realfamousbae.standby.dock.widgets.StandbyWidgetHost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class App : Application() {

    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = AppGraph(this)
        Notifications.createChannels(this)
        // El chequeo de la parada externa y el KeepAliveWorker viven adentro de RunState.
        graph.runState.startup(graph.appScope)
        // Clave de Immich dejada por adb, si la hay (ver ImmichSeed).
        graph.appScope.launch {
            ImmichSeed.importIfPresent(this@App, graph.settingsRepository, graph.diagLog)
        }
    }
}

/**
 * Contenedor de dependencias hecho a mano (sin framework de DI). Vive lo mismo que el proceso.
 * Acá se van sumando repositorios, ajustes y demás; los componentes de Android (servicio,
 * Activity, receivers) sacan sus dependencias solo de acá.
 */
class AppGraph(app: Application) {
    /** Ámbito de corutinas del proceso (para los StateFlow compartidos). */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val diagLog = DiagLog(File(app.filesDir, "diag.log"))
    val settingsRepository = SettingsRepository(app)
    val controller = StandbyController()
    val widgetHost = StandbyWidgetHost(app)
    val notificationBus = NotificationBus()
    val quietMode = QuietMode(app, diagLog)
    val immich = ImmichClient(diagLog)
    val weather = WeatherRepository(diagLog)
    val nowPlaying = NowPlayingSource(app)
    val runState = RunState(app, settingsRepository, diagLog)

    /** Los últimos ajustes, siempre disponibles al toque (hasta la primera lectura, los de fábrica). */
    val settings: StateFlow<StandbySettings> =
        settingsRepository.settings.stateIn(appScope, SharingStarted.Eagerly, StandbySettings())
}

val Context.graph: AppGraph get() = (applicationContext as App).graph
