package com.realfamousbae.standby.dock.settings

import android.appwidget.AppWidgetProviderInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.realfamousbae.standby.dock.graph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Lista de widgets del sistema que se pueden poner en la tarjeta. */
internal class WidgetEntry(val info: AppWidgetProviderInfo, val label: String, val app: String, val icon: ImageBitmap?)

@Composable
internal fun WidgetPickerDialog(onDismiss: () -> Unit, onPick: (AppWidgetProviderInfo) -> Unit) {
    val context = LocalContext.current
    val host = context.graph.widgetHost
    val density = LocalResources.current.displayMetrics.densityDpi
    val entries by produceState<List<WidgetEntry>?>(null) {
        value = withContext(Dispatchers.Default) {
            host.providers().map { info ->
                val icon = runCatching { info.loadIcon(context, density)?.toBitmap(96, 96)?.asImageBitmap() }.getOrNull()
                WidgetEntry(info, host.label(info), host.appLabel(info), icon)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        title = { Text("Widget del sistema") },
        text = {
            val list = entries
            when {
                list == null -> Text("Cargando…")
                list.isEmpty() -> Text("No se encontraron widgets.")
                else -> LazyColumn(Modifier.heightIn(max = 480.dp)) {
                    items(list, key = { "${it.info.provider.flattenToString()}@${it.info.profile.hashCode()}" }) { entry ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onPick(entry.info) }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            entry.icon?.let { Image(it, contentDescription = null, modifier = Modifier.size(36.dp)) }
                            Column {
                                Text(entry.label)
                                Text(entry.app, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        },
    )
}
