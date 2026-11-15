package com.realfamousbae.standby.dock.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.realfamousbae.standby.dock.R
import com.realfamousbae.standby.dock.settings.SystemChecks
import com.realfamousbae.standby.dock.settings.openListenerSettings
import androidx.compose.ui.res.stringResource

/**
 * Guía de la primera vez: un paso por pantalla, en criollo, con un botón grande que abre
 * el ajuste del sistema que hace falta. Pensada para que alguien que no es técnico pueda
 * dejar la app andando sin que nadie lo ayude.
 */
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val context = LocalContext.current
    var step by rememberSaveable { mutableIntStateOf(0) }
    // Al volver de los ajustes del sistema hay que volver a mirar si ya está dado.
    var refresh by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        refresh++
        onPauseOrDispose { }
    }
    val askNotifications = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { refresh++ }

    @Suppress("UNUSED_EXPRESSION") refresh
    val steps = steps(context) { askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS) }
    val current = steps[step.coerceIn(steps.indices)]

    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "Paso ${step + 1} de ${steps.size}",
            style = MaterialTheme.typography.labelLarge,
        )
        Text(current.title, style = MaterialTheme.typography.headlineMedium)
        Text(current.body, style = MaterialTheme.typography.bodyLarge)

        current.state?.let { ok ->
            Text(
                if (ok) "✅  Ya está listo" else "❌  Todavía falta",
                style = MaterialTheme.typography.titleMedium,
            )
        }
        current.action?.let { action ->
            Button(
                onClick = { action.run(context) },
                modifier = Modifier.fillMaxWidth().height(64.dp),
            ) { Text(action.label) }
        }
        current.extra?.let { extra ->
            OutlinedButton(
                onClick = { extra.run(context) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(extra.label) }
        }

        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            if (step > 0) {
                TextButton(onClick = { step-- }) { Text("Atrás") }
            } else {
                TextButton(onClick = onFinish) { Text("Saltar la guía") }
            }
            Button(
                onClick = { if (step == steps.lastIndex) onFinish() else step++ },
                modifier = Modifier.height(56.dp),
            ) { Text(if (step == steps.lastIndex) "Listo" else "Siguiente") }
        }
        Text(
            "Todo esto se puede cambiar después desde Ajustes.",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Start,
        )
    }
}

private class StepAction(val label: String, val run: (Context) -> Unit)

private class Step(
    val title: String,
    val body: String,
    val state: Boolean? = null,
    val action: StepAction? = null,
    val extra: StepAction? = null,
)

@Composable
private fun steps(context: Context, askNotifications: () -> Unit): List<Step> {
    val nombre = stringResource(R.string.app_name)
    return listOf(
        Step(
            title = "Hola, soy $nombre",
            body = "Cuando dejes el teléfono cargando y acostado de costado, se transforma " +
                "en un reloj de mesa: hora grande, clima, agenda y tus fotos. Se apaga solo " +
                "cuando lo levantás.\n\nSon unos pasos cortos. Te voy a ir abriendo los " +
                "ajustes de Android que hacen falta; en cada uno tocás el botón grande, " +
                "prendés lo que te pida y volvés con la flechita de atrás.",
        ),
        Step(
            title = "Dejarme aparecer sobre la pantalla",
            body = "Sin esto el reloj no puede aparecer solo cuando la pantalla está " +
                "bloqueada. Buscá $nombre en la lista y prendé el permiso.",
            state = SystemChecks.canDrawOverlays(context),
            action = StepAction("Abrir «Sobre otras apps»") {
                it.startActivity(SystemChecks.overlaySettings(it))
            },
        ),
        Step(
            title = "Que Android no me apague",
            body = "Android apaga las apps que no usás para ahorrar batería. Como el reloj " +
                "tiene que estar atento todo el tiempo, hay que sacarle esa restricción. " +
                "Tocá «Permitir» cuando te pregunte.\n\nEn Samsung, además, conviene entrar " +
                "en Info de la app → Batería → Sin restricciones.",
            state = SystemChecks.isIgnoringBatteryOptimizations(context),
            action = StepAction("Sacar la restricción de batería") {
                it.startActivity(SystemChecks.requestIgnoreBatteryOptimizations(it))
            },
            extra = StepAction("Abrir Info de la app") { it.startActivity(SystemChecks.appDetails(it)) },
        ),
        Step(
            title = "Avisarte que estoy funcionando",
            body = "Android pide permiso para mostrar la notificación chiquita que dice que " +
                "el reloj está atento. Es una sola, sin sonido.",
            state = SystemChecks.canPostNotifications(context),
            action = StepAction("Dar el permiso") {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) askNotifications()
            },
        ),
        Step(
            title = "Mostrarte los mensajes que llegan",
            body = "Si me dejás leer las notificaciones, mientras el reloj está en pantalla " +
                "te muestro arriba los mensajes y las llamadas, en grande. Tocás el aviso y " +
                "se abre la app.\n\nSi el interruptor aparece gris («Ajuste restringido»): " +
                "Info de la app → los tres puntitos de arriba → «Permitir ajustes " +
                "restringidos», y volvé.",
            state = NotificationManagerCompat.getEnabledListenerPackages(context)
                .contains(context.packageName),
            action = StepAction("Abrir «Acceso a notificaciones»") { openListenerSettings(it) },
            extra = StepAction("Abrir Info de la app") { it.startActivity(SystemChecks.appDetails(it)) },
        ),
        Step(
            title = "Apagar el protector de pantalla de Samsung",
            body = "Si el protector de pantalla de Samsung está prendido, se pone adelante " +
                "del reloj. Entrá y dejalo apagado.",
            state = SystemChecks.isScreensaverEnabled(context)?.not(),
            action = StepAction("Abrir el protector de pantalla") {
                it.startActivity(Intent(SystemChecks.screensaverSettings()))
            },
        ),
        Step(
            title = "Listo",
            body = "Ya está. Poné el teléfono en el cargador, acostado de costado, y apagá " +
                "la pantalla: el reloj aparece solo.\n\nEn el reloj, pasá el dedo de " +
                "izquierda a derecha para cambiar de cara (digital, aguja, flip, fotos) y de " +
                "arriba a abajo en la tarjeta de la derecha para ver clima, agenda, alarma o " +
                "lo que está sonando. Un toque en el reloj lo cierra.",
        ),
    )
}
