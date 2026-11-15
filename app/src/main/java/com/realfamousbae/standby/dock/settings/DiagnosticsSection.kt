package com.realfamousbae.standby.dock.settings

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.realfamousbae.standby.dock.core.trigger.TriggerEvent
import com.realfamousbae.standby.dock.graph
import com.realfamousbae.standby.dock.service.ServiceStarter
import com.realfamousbae.standby.dock.service.StandbyService

/**
 * Permisos (los necesita todo el mundo) y el bloque "Para desarrolladores", cerrado por
 * defecto, con las pruebas a mano y el registro.
 */
@Composable
fun DiagnosticsSection() {
    val context = LocalContext.current
    // Los estados se vuelven a mirar cada vez que se vuelve a la pantalla (por ejemplo, del sistema).
    var refresh by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        refresh++
        onPauseOrDispose { }
    }
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { refresh++ }

    // Leer `refresh` hace que este bloque se vuelva a dibujar.
    @Suppress("UNUSED_EXPRESSION") refresh
    val overlay = SystemChecks.canDrawOverlays(context)
    val battery = SystemChecks.isIgnoringBatteryOptimizations(context)
    val notifications = SystemChecks.canPostNotifications(context)
    val screensaver = SystemChecks.isScreensaverEnabled(context)
    var devOpen by rememberSaveable { mutableStateOf(false) }
    // Las pruebas del modo necesitan el servicio andando: con el modo apagado no se pueden tocar.
    val enabled = context.graph.settings.collectAsStateWithLifecycle().value.enabled

    SectionTitle("Permisos")
    StatusRow("Sobre otras apps", overlay) {
        context.startActivity(SystemChecks.overlaySettings(context))
    }
    StatusRow("Sin optimización de batería", battery) {
        context.startActivity(SystemChecks.requestIgnoreBatteryOptimizations(context))
    }
    StatusRow("Notificación del servicio", notifications) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    StatusRow(
        title = "Protector de pantalla Samsung apagado",
        ok = screensaver?.not(),
        unknownText = "no se pudo leer",
    ) { context.startActivity(SystemChecks.screensaverSettings()) }

    OutlinedButton(
        onClick = { context.startActivity(SystemChecks.appDetails(context)) },
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Info de la app (Batería → Sin restricciones)") }

    Row(
        Modifier
            .fillMaxWidth()
            .clickable { devOpen = !devOpen }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("Para desarrolladores", style = MaterialTheme.typography.titleMedium)
        Text(if (devOpen) "▾" else "▸", style = MaterialTheme.typography.titleMedium)
    }
    AnimatedVisibility(devOpen) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Su propia zona de dibujo: leer `refresh` acá actualiza el registro con los botones.
            @Suppress("UNUSED_EXPRESSION") refresh
            SectionTitle("Pruebas")
            Button(
                // Pasa por el servicio: mostrar y cerrar el modo lo decide solo él (TriggerEngine).
                onClick = { context.graph.controller.send(TriggerEvent.ManualShow) },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Mostrar modo ahora") }
            Button(
                onClick = {
                    ServiceStarter.start(context, "botón de prueba", StandbyService.ACTION_TEST_LAUNCH)
                },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Mostrar modo en 10 s (bloqueá la pantalla)") }
            OutlinedButton(
                onClick = {
                    // Parada a propósito: START_STICKY no va a actuar y al servicio solo lo pueden
                    // levantar KeepAliveWorker o BootReceiver. El interruptor no cambia.
                    context.stopService(Intent(context, StandbyService::class.java))
                    context.graph.diagLog.log("Prueba: servicio detenido a mano")
                    refresh++
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Detener servicio (prueba de levantar)") }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle("Registro")
                OutlinedButton(onClick = { refresh++ }) { Text("Actualizar") }
                OutlinedButton(onClick = { context.graph.diagLog.clear(); refresh++ }) { Text("Limpiar") }
            }
            context.graph.diagLog.lines().takeLast(80).reversed().forEach {
                Text(it, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
        }
    }
}
