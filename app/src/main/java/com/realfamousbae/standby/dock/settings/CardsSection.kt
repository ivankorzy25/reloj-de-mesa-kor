package com.realfamousbae.standby.dock.settings

import android.Manifest
import android.appwidget.AppWidgetProviderInfo
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.realfamousbae.standby.dock.calendar.CalendarEvents
import com.realfamousbae.standby.dock.core.prefs.ScreenLayout
import com.realfamousbae.standby.dock.core.prefs.SideCard
import com.realfamousbae.standby.dock.graph
import kotlinx.coroutines.launch

/**
 * La segunda tarjeta: qué se ve al lado del reloj (clima, agenda, alarma, música o un
 * widget del sistema) y los datos que cada una necesita.
 */
@Composable
fun CardsSection(onPickWidget: (AppWidgetProviderInfo) -> Unit) {
    val context = LocalContext.current
    val graph = context.graph
    val repository = graph.settingsRepository
    val settings by graph.settings.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var pickerOpen by remember { mutableStateOf(false) }
    var weatherOpen by remember { mutableStateOf(false) }
    var immichOpen by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        refresh++
        onPauseOrDispose { }
    }
    @Suppress("UNUSED_EXPRESSION") refresh
    val widget = graph.widgetHost.info(settings.widgetId)

    SectionTitle("Pantalla del modo")
    ChipRow(
        options = ScreenLayout.entries,
        selected = settings.layout,
        label = { it.label },
    ) { layout -> scope.launch { repository.setLayout(layout) } }

    if (settings.layout != ScreenLayout.ClockOnly) {
        Text(
            "En el modo, las tarjetas se pasan con el dedo de arriba a abajo.",
            style = MaterialTheme.typography.bodySmall,
        )
        if (settings.layout == ScreenLayout.Widgets) Text("Primera columna")
        ChipRow(
            options = SideCard.available(widgetAvailable = widget != null),
            selected = settings.sideCard,
            label = { it.label },
        ) { card -> scope.launch { repository.setSideCard(card) } }
    }
    if (settings.layout == ScreenLayout.Widgets) {
        Text("Segunda columna")
        ChipRow(
            // El widget del sistema solo se puede dibujar una vez: va en la primera columna.
            options = SideCard.available(widgetAvailable = false),
            selected = settings.sideCard2,
            label = { it.label },
        ) { card -> scope.launch { repository.setSideCard2(card) } }
    }

    // --- Clima ---
    Text("Clima: ${settings.weatherPlace} (${"%.3f".format(settings.weatherLatitude)}, " +
        "${"%.3f".format(settings.weatherLongitude)})")
    OutlinedButton(onClick = { weatherOpen = true }, modifier = Modifier.fillMaxWidth()) {
        Text("Cambiar el lugar del clima")
    }

    // --- Agenda ---
    val calendarGranted = CalendarEvents.hasPermission(context)
    val askCalendar = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { refresh++ }
    StatusRow("Permiso de calendario", calendarGranted) {
        askCalendar.launch(Manifest.permission.READ_CALENDAR)
    }

    // --- Fotos ---
    Text(
        if (settings.immichReady) "Fotos: Immich configurado (${settings.immichUrl})"
        else "Fotos: falta la clave de Immich",
    )
    OutlinedButton(onClick = { immichOpen = true }, modifier = Modifier.fillMaxWidth()) {
        Text("Fotos de Immich")
    }

    // --- Widget del sistema ---
    if (widget != null) {
        Text("Widget: ${graph.widgetHost.label(widget)} (${graph.widgetHost.appLabel(widget)})")
    } else if (settings.widgetId != null) {
        Text(
            "El widget elegido ya no está disponible (¿app desinstalada?). Elegí otro.",
            color = MaterialTheme.colorScheme.error,
        )
    }
    OutlinedButton(onClick = { pickerOpen = true }, modifier = Modifier.fillMaxWidth()) {
        Text(if (widget == null) "Elegir widget del sistema" else "Cambiar widget del sistema")
    }

    if (pickerOpen) {
        WidgetPickerDialog(
            onDismiss = { pickerOpen = false },
            onPick = {
                pickerOpen = false
                onPickWidget(it)
            },
        )
    }
    if (weatherOpen) WeatherPlaceDialog { weatherOpen = false }
    if (immichOpen) ImmichDialog { immichOpen = false }
}

/** Lugar del clima: nombre y coordenadas (se buscan una vez en un mapa y listo). */
@Composable
private fun WeatherPlaceDialog(onDismiss: () -> Unit) {
    val graph = LocalContext.current.graph
    val settings by graph.settings.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var place by remember { mutableStateOf(settings.weatherPlace) }
    var lat by remember { mutableStateOf(settings.weatherLatitude.toString()) }
    var lon by remember { mutableStateOf(settings.weatherLongitude.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lugar del clima") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Las coordenadas se sacan de cualquier mapa: tocá un lugar y copiá los " +
                        "dos números.",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(place, { place = it }, label = { Text("Nombre") }, singleLine = true)
                OutlinedTextField(lat, { lat = it }, label = { Text("Latitud") }, singleLine = true)
                OutlinedTextField(lon, { lon = it }, label = { Text("Longitud") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val latitude = lat.trim().replace(',', '.').toFloatOrNull()
                val longitude = lon.trim().replace(',', '.').toFloatOrNull()
                if (latitude != null && longitude != null) {
                    scope.launch {
                        graph.settingsRepository.setWeatherPlace(
                            latitude = latitude.coerceIn(-90f, 90f),
                            longitude = longitude.coerceIn(-180f, 180f),
                            place = place.trim().ifBlank { "Mi lugar" },
                        )
                    }
                }
                onDismiss()
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

/** Dirección y clave de Immich para la cara de fotos. */
@Composable
private fun ImmichDialog(onDismiss: () -> Unit) {
    val graph = LocalContext.current.graph
    val settings by graph.settings.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf(settings.immichUrl) }
    var key by remember { mutableStateOf(settings.immichApiKey) }
    var result by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Fotos de Immich") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "La clave se saca en Immich: Cuenta → Claves de API. Queda guardada " +
                        "solo en este teléfono.",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(url, { url = it }, label = { Text("Dirección") }, singleLine = true)
                OutlinedTextField(key, { key = it }, label = { Text("Clave de API") }, singleLine = true)
                SwitchRow(
                    title = "Solo las favoritas",
                    checked = settings.immichOnlyFavorites,
                ) { on -> scope.launch { graph.settingsRepository.setImmichOnlyFavorites(on) } }
                result?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            graph.settingsRepository.setImmich(url, key)
                            result = "Probando…"
                            result = graph.immich.test(graph.settings.value.copy(immichUrl = url.trim().trimEnd('/'), immichApiKey = key.trim()))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Probar conexión") }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                scope.launch { graph.settingsRepository.setImmich(url, key) }
                onDismiss()
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
