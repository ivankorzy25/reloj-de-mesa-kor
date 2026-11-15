package com.realfamousbae.standby.dock.standby

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.realfamousbae.standby.dock.R
import com.realfamousbae.standby.dock.core.prefs.ClockFont

/**
 * Fuentes de la pantalla del modo. Todas con licencia SIL OFL 1.1 (ver `licenses/`):
 * IBM Plex Sans Condensed (fina y condensada, la de por defecto para la hora),
 * Manrope, Space Grotesk, DM Mono, e Inter para el texto chico.
 */
object StandbyFonts {
    val condensada = FontFamily(
        Font(R.font.plex_cond_light, FontWeight.Light),
        Font(R.font.plex_cond_medium, FontWeight.Medium),
    )
    val redonda = FontFamily(Font(R.font.manrope_bold, FontWeight.Bold))
    val geometrica = FontFamily(Font(R.font.space_grotesk_light, FontWeight.Light))
    val mono = FontFamily(Font(R.font.dm_mono_light, FontWeight.Light))

    /** Texto chico (fecha, tarjetas, avisos). */
    val text = FontFamily(
        Font(R.font.inter_regular, FontWeight.Normal),
        Font(R.font.inter_medium, FontWeight.Medium),
    )

    fun family(font: ClockFont): FontFamily = when (font) {
        ClockFont.CondensadaFina -> condensada
        ClockFont.Redonda -> redonda
        ClockFont.Geometrica -> geometrica
        ClockFont.Mono -> mono
    }

    /** Cada fuente se ve bien con un grosor distinto. */
    fun weight(font: ClockFont): FontWeight = when (font) {
        ClockFont.CondensadaFina -> FontWeight.Light
        ClockFont.Redonda -> FontWeight.Bold
        ClockFont.Geometrica -> FontWeight.Light
        ClockFont.Mono -> FontWeight.Light
    }

    /** Dígitos de un solo ancho: la hora no "salta" al cambiar de minuto. */
    const val TABULAR_NUMBERS = "tnum"
}

/** Paleta del modo: grises apagados sobre negro puro (OLED) y la versión de noche en rojo. */
object StandbyColors {
    val background = Color.Black
    val time = Color(0xFFB8B8B8)
    val secondary = Color(0xFF777777)
    val card = Color(0xFF0D0D0D)
    /** Ficha del flip clock: un escalón más clara que la tarjeta, si no no se ve. */
    val flipCard = Color(0xFF1C1C1C)
    val cardBorder = Color(0xFF242424)
    val banner = Color(0xFF1C1C1E)
    val bannerTitle = Color(0xFFE6E6E6)
    val bannerText = Color(0xFF9A9A9A)

    /** Modo noche: rojo tenue, el color que menos molesta a la vista a oscuras. */
    val nightPrimary = Color(0xFFA83B22)
    val nightSecondary = Color(0xFF6B2514)
    val nightCard = Color(0xFF0A0303)
    val nightFlipCard = Color(0xFF1A0705)
    val nightCardBorder = Color(0xFF2A100A)
}
