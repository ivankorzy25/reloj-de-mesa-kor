package com.realfamousbae.standby.dock.faces

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.realfamousbae.standby.dock.core.prefs.ClockFace
import com.realfamousbae.standby.dock.core.prefs.StandbySettings
import com.realfamousbae.standby.dock.photos.ImmichClient
import com.realfamousbae.standby.dock.standby.ClockStyle

/**
 * Las caras del reloj, una al lado de la otra: se pasan con el dedo (horizontal) y la
 * última queda guardada, así mañana aparece la misma.
 */
@Composable
fun ClockFaceHost(
    settings: StandbySettings,
    style: ClockStyle,
    immich: ImmichClient,
    maxTimeSize: TextUnit,
    dateSize: TextUnit,
    onFaceChange: (ClockFace) -> Unit,
    modifier: Modifier = Modifier,
) {
    val faces = ClockFace.entries
    val pager = rememberPagerState(initialPage = faces.indexOf(settings.clockFace)) { faces.size }

    // Guardar la cara en la que quedó el dedo.
    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.collect { onFaceChange(ClockFace.byIndex(it)) }
    }
    // Si los ajustes guardados llegan después del primer cuadro (o Iván cambia la cara en
    // Ajustes), el carrusel se acomoda solo.
    LaunchedEffect(settings.clockFace) {
        val target = faces.indexOf(settings.clockFace)
        if (!pager.isScrollInProgress && pager.currentPage != target) pager.scrollToPage(target)
    }

    HorizontalPager(state = pager, modifier = modifier, pageSpacing = 0.dp) { page ->
        when (ClockFace.byIndex(page)) {
            ClockFace.Digital -> DigitalFace(
                style = style,
                maxTimeSize = maxTimeSize,
                dateSize = dateSize,
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            )
            ClockFace.Analogico -> AnalogFace(style, Modifier.fillMaxSize().padding(16.dp))
            ClockFace.Flip -> FlipFace(style, Modifier.fillMaxSize().padding(16.dp))
            ClockFace.Fotos -> PhotosFace(settings, style, immich, Modifier.fillMaxSize())
        }
    }
}
