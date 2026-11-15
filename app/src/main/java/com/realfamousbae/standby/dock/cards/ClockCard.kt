package com.realfamousbae.standby.dock.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.realfamousbae.standby.dock.faces.DigitalFace
import com.realfamousbae.standby.dock.standby.ClockStyle

/**
 * El reloj como una tarjeta más, para poder armar las dos columnas con la hora en una de
 * ellas (así queda la vista "Widgets" del StandBy del iPhone, que siempre tiene la hora).
 */
@Composable
fun ClockCard(style: ClockStyle) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DigitalFace(
            style = style,
            maxTimeSize = 140.sp,
            dateSize = 17.sp,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
