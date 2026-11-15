package com.realfamousbae.standby.dock.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.realfamousbae.standby.dock.core.prefs.ClockColors
import com.realfamousbae.standby.dock.core.prefs.ClockFace
import com.realfamousbae.standby.dock.core.prefs.ClockFont
import com.realfamousbae.standby.dock.core.prefs.HourFormat
import com.realfamousbae.standby.dock.core.prefs.StandbySettings
import com.realfamousbae.standby.dock.core.prefs.WorldCities
import com.realfamousbae.standby.dock.graph
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** El reloj: qué cara, de qué color, con qué letra y de qué tamaño. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClockSection() {
    val context = LocalContext.current
    val graph = context.graph
    val repository = graph.settingsRepository
    val settings by graph.settings.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var sizeDraft by remember { mutableFloatStateOf(settings.clockSizePercent.toFloat()) }

    SectionTitle("El reloj")
    Text(
        "Así se va a ver. En el modo, las caras se pasan con el dedo (de izquierda a derecha).",
        style = MaterialTheme.typography.bodySmall,
    )
    LivePreview(settings)

    Text("Cara")
    ChipRow(
        options = ClockFace.entries,
        selected = settings.clockFace,
        label = { faceLabel(it) },
    ) { face -> scope.launch { repository.setClockFace(face) } }

    if (settings.clockFace == ClockFace.Mundial) {
        Text("Ciudades del reloj mundial (hasta ${WorldCities.MAX})")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WorldCities.catalog.forEach { city ->
                val chosen = settings.worldCities.any { it.name == city.name }
                FilterChip(
                    selected = chosen,
                    onClick = {
                        scope.launch { repository.setWorldCities(WorldCities.toggle(settings.worldCities, city)) }
                    },
                    label = { Text(city.name) },
                )
            }
        }
    }

    Text("Color")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ClockColors.palette.forEach { (name, value) ->
            val chosen = settings.clockColor == value
            Box(
                Modifier
                    .size(44.dp)
                    .background(Color(value), CircleShape)
                    .border(
                        width = if (chosen) 3.dp else 1.dp,
                        color = if (chosen) MaterialTheme.colorScheme.primary else Color(0x33FFFFFF),
                        shape = CircleShape,
                    )
                    .clickable { scope.launch { repository.setClockColor(value) } },
            )
            // El nombre no se dibuja: el círculo ya dice de qué color es.
            @Suppress("UNUSED_EXPRESSION") name
        }
    }

    Text("Letra")
    ChipRow(
        options = ClockFont.entries,
        selected = settings.clockFont,
        label = { it.label },
    ) { font -> scope.launch { repository.setClockFont(font) } }

    SliderRow(
        title = "Tamaño: ${sizeDraft.roundToInt()}%",
        value = sizeDraft,
        range = StandbySettings.CLOCK_SIZE_MIN.toFloat()..StandbySettings.CLOCK_SIZE_MAX.toFloat(),
        steps = (StandbySettings.CLOCK_SIZE_MAX - StandbySettings.CLOCK_SIZE_MIN) / 5 - 1,
        onChange = { sizeDraft = it },
        onCommit = { scope.launch { repository.setClockSize(it.roundToInt()) } },
    )

    SwitchRow(
        title = "Mostrar segundos",
        subtitle = "Con segundos el reloj se refresca a cada segundo.",
        checked = settings.showSeconds,
    ) { on -> scope.launch { repository.setShowSeconds(on) } }

    Text("Hora")
    ChipRow(
        options = HourFormat.entries,
        selected = settings.hourFormat,
        label = { it.label },
    ) { format -> scope.launch { repository.setHourFormat(format) } }
}

internal fun faceLabel(face: ClockFace): String = when (face) {
    ClockFace.Digital -> "Digital"
    ClockFace.Cuadricula -> "Cuadrícula"
    ClockFace.Flotante -> "Flotante"
    ClockFace.Solar -> "Solar"
    ClockFace.Analogico -> "Analógica"
    ClockFace.Flip -> "Flip"
    ClockFace.Mundial -> "Mundial"
    ClockFace.Fotos -> "Fotos"
}
