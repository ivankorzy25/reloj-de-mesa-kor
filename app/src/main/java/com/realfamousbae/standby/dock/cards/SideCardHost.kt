package com.realfamousbae.standby.dock.cards

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import com.realfamousbae.standby.dock.core.prefs.SideCard
import com.realfamousbae.standby.dock.core.prefs.StandbySettings
import com.realfamousbae.standby.dock.media.NowPlayingSource
import com.realfamousbae.standby.dock.standby.ClockStyle
import com.realfamousbae.standby.dock.weather.WeatherRepository

/**
 * Las tarjetas de la derecha, una arriba de la otra: se pasan con el dedo (vertical) y la
 * última queda guardada.
 */
@Composable
fun SideCardHost(
    settings: StandbySettings,
    style: ClockStyle,
    weather: WeatherRepository,
    nowPlaying: NowPlayingSource,
    widgetAvailable: Boolean,
    widget: @Composable () -> Unit,
    onCardChange: (SideCard) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cards = SideCard.available(widgetAvailable)
    val pager = rememberPagerState(
        initialPage = SideCard.pageOf(settings.sideCard, widgetAvailable),
    ) { cards.size }

    LaunchedEffect(pager, cards.size) {
        snapshotFlow { pager.settledPage }.collect { page ->
            cards.getOrNull(page)?.let(onCardChange)
        }
    }
    LaunchedEffect(settings.sideCard, cards.size) {
        val target = SideCard.pageOf(settings.sideCard, widgetAvailable)
        if (!pager.isScrollInProgress && pager.currentPage != target) pager.scrollToPage(target)
    }

    VerticalPager(state = pager, modifier = modifier) { page ->
        Box(Modifier.fillMaxSize()) {
            when (cards.getOrElse(page) { SideCard.Clima }) {
                SideCard.Clima -> WeatherCard(settings, style, weather)
                SideCard.Calendario -> CalendarCard(style)
                SideCard.AlarmaBateria -> AlarmBatteryCard(style)
                SideCard.Musica -> NowPlayingCard(style, nowPlaying)
                SideCard.Widget -> widget()
            }
        }
    }
}
