package com.realfamousbae.standby.dock.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.realfamousbae.standby.dock.standby.ClockStyle
import com.realfamousbae.standby.dock.standby.StandbyFonts

/** Piezas comunes de las tarjetas, para que todas se vean de la misma familia. */
@Composable
fun CardFrame(style: ClockStyle, title: String, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        CardLabel(title, style)
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = androidx.compose.ui.Alignment.Start,
        ) { content() }
    }
}

@Composable
fun CardLabel(text: String, style: ClockStyle) {
    BasicText(
        text.uppercase(),
        style = TextStyle(
            color = style.secondary,
            fontFamily = StandbyFonts.text,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            letterSpacing = 1.5.sp,
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun CardBig(text: String, style: ClockStyle, size: TextUnit = 72.sp) {
    BasicText(
        text,
        style = TextStyle(
            color = style.color,
            fontFamily = style.family,
            fontWeight = style.weight,
            fontFeatureSettings = StandbyFonts.TABULAR_NUMBERS,
            fontSize = style.size(size),
        ),
        maxLines = 1,
    )
}

@Composable
fun CardText(
    text: String,
    style: ClockStyle,
    size: TextUnit = 18.sp,
    strong: Boolean = false,
    maxLines: Int = 1,
) {
    BasicText(
        text,
        style = TextStyle(
            color = if (strong) style.color else style.secondary,
            fontFamily = StandbyFonts.text,
            fontWeight = if (strong) FontWeight.Medium else FontWeight.Normal,
            fontSize = size,
        ),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}
