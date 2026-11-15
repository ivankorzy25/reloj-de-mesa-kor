package com.realfamousbae.standby.dock.settings

import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.realfamousbae.standby.dock.R
import androidx.lifecycle.lifecycleScope
import com.realfamousbae.standby.dock.core.prefs.ScreenLayout
import com.realfamousbae.standby.dock.core.prefs.SideCard
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.realfamousbae.standby.dock.graph
import com.realfamousbae.standby.dock.onboarding.OnboardingScreen
import com.realfamousbae.standby.dock.service.ServiceStarter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Ajustes: arriba lo que se usa todos los días (el reloj, las tarjetas, las notificaciones) y
 * abajo los permisos, las pruebas y el registro de diagnóstico.
 */
class SettingsActivity : ComponentActivity() {

    private val widgetHost get() = graph.widgetHost

    /** id del widget que se está enganchando o configurando en este momento. */
    private var pendingWidgetId: Int? = null

    private val bindWidget = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val id = pendingWidgetId ?: return@registerForActivityResult
        if (it.resultCode == RESULT_OK) configureOrSave(id) else cancelPending()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Desde una Activity visible siempre se puede arrancar el servicio. Si el modo está
        // apagado (por el interruptor o por una parada externa), abrir la app no lo levanta.
        ServiceStarter.start(this, reason = "app abierta")
        pendingWidgetId = savedInstanceState?.getInt(KEY_PENDING, NO_ID)?.takeIf { it != NO_ID }
        if (pendingWidgetId == null) {
            // Sacar los id que quedaron de una elección cortada junto con el proceso.
            lifecycleScope.launch {
                val saved = graph.settingsRepository.settings.first().widgetId
                // Iván pudo haber empezado a elegir mientras se leían los ajustes.
                widgetHost.deleteAllExcept(setOfNotNull(saved, pendingWidgetId))
            }
        }
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                val settings by graph.settings.collectAsStateWithLifecycle()
                var guiaAbierta by rememberSaveable { mutableStateOf<Boolean?>(null) }
                // La primera vez arranca la guía; después se puede volver a abrir a mano.
                val mostrarGuia = guiaAbierta ?: !settings.onboardingDone
                Surface(Modifier.fillMaxSize()) {
                    if (mostrarGuia) {
                        OnboardingScreen(
                            onFinish = {
                                guiaAbierta = false
                                lifecycleScope.launch { graph.settingsRepository.setOnboardingDone(true) }
                            },
                        )
                    } else {
                        SettingsScreen(
                            onPickWidget = ::pickWidget,
                            onOpenGuide = { guiaAbierta = true },
                        )
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        pendingWidgetId?.let { outState.putInt(KEY_PENDING, it) }
    }

    /** Paso 1: reservar un id y engancharlo (con el diálogo del sistema, si hace falta). */
    private fun pickWidget(provider: AppWidgetProviderInfo) {
        cancelPending()
        val id = widgetHost.allocate()
        pendingWidgetId = id
        if (widgetHost.bindIfAllowed(id, provider.provider)) {
            configureOrSave(id)
        } else {
            bindWidget.launch(widgetHost.bindIntent(id, provider.provider))
        }
    }

    /** Paso 2: la pantalla de configuración del widget, si tiene; si no, guardar directo. */
    private fun configureOrSave(id: Int) {
        val started = runCatching { widgetHost.startConfigureIfNeeded(this, id, REQUEST_CONFIGURE) }
            .getOrElse {
                graph.diagLog.log("Widget: no se abrió la pantalla de configuración (${it.javaClass.simpleName})")
                false
            }
        if (!started) save(id)
    }

    @Deprecated("startAppWidgetConfigureActivityForResult devuelve el resultado solo acá")
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_CONFIGURE) return
        val id = pendingWidgetId ?: return
        if (resultCode == RESULT_OK) save(id) else cancelPending()
    }

    private fun save(id: Int) {
        pendingWidgetId = null
        lifecycleScope.launch {
            val previous = graph.settingsRepository.settings.first().widgetId
            graph.settingsRepository.setWidgetId(id)
            graph.settingsRepository.setLayout(ScreenLayout.ClockAndWidget)
            graph.settingsRepository.setSideCard(SideCard.Widget)
            if (previous != null && previous != id) widgetHost.delete(previous)
            graph.diagLog.log("Widget: elegido")
        }
    }

    private fun cancelPending() {
        pendingWidgetId?.let(widgetHost::delete)
        pendingWidgetId = null
    }

    private companion object {
        const val REQUEST_CONFIGURE = 4101
        const val KEY_PENDING = "pending_widget_id"
        const val NO_ID = -1
    }
}

@Composable
private fun SettingsScreen(onPickWidget: (AppWidgetProviderInfo) -> Unit, onOpenGuide: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall)
        PowerSection()
        SectionDivider()
        ClockSection()
        SectionDivider()
        CardsSection(onPickWidget)
        SectionDivider()
        LiveSection()
        SectionDivider()
        NightSection()
        SectionDivider()
        NotificationsSection()
        SectionDivider()
        TriggerSettingsSection()
        SectionDivider()
        OutlinedButton(onClick = onOpenGuide, modifier = Modifier.fillMaxWidth()) {
            Text("Volver a ver la guía del principio")
        }
        SectionDivider()
        DiagnosticsSection()
    }
}

@Composable
private fun SectionDivider() = HorizontalDivider(Modifier.padding(vertical = 8.dp))

@Composable
internal fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium)
}

@Composable
internal fun StatusRow(
    title: String,
    ok: Boolean?,
    unknownText: String = "desconocido",
    onFix: () -> Unit,
) {
    val mark = when (ok) {
        true -> "✅"
        false -> "❌"
        null -> "❔ $unknownText"
    }
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("$mark  $title", modifier = Modifier.weight(1f).padding(top = 10.dp))
        if (ok != true) OutlinedButton(onClick = onFix) { Text("Abrir") }
    }
}
