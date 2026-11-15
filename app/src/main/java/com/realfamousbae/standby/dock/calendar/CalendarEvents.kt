package com.realfamousbae.standby.dock.calendar

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Un evento de la agenda, con lo poco que entra en la tarjeta. */
data class AgendaEvent(
    val title: String,
    val startMs: Long,
    val endMs: Long,
    val allDay: Boolean,
    val color: Int,
)

/** Próximos eventos del calendario del teléfono (solo lectura). */
object CalendarEvents {

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED

    /** Lo que viene en los próximos [days] días, ordenado por hora de inicio. */
    suspend fun next(context: Context, limit: Int = 3, days: Int = 7): List<AgendaEvent> =
        withContext(Dispatchers.IO) {
            if (!hasPermission(context)) return@withContext emptyList()
            val now = System.currentTimeMillis()
            val until = now + days * 24L * 60 * 60 * 1000
            val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
                .let { ContentUris.appendId(it, now); ContentUris.appendId(it, until); it.build() }
            val columns = arrayOf(
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Instances.ALL_DAY,
                CalendarContract.Instances.DISPLAY_COLOR,
                CalendarContract.Instances.STATUS,
            )
            runCatching {
                context.contentResolver.query(
                    uri,
                    columns,
                    "${CalendarContract.Instances.STATUS} IS NULL OR " +
                        "${CalendarContract.Instances.STATUS} != ${CalendarContract.Instances.STATUS_CANCELED}",
                    null,
                    "${CalendarContract.Instances.BEGIN} ASC",
                )?.use { cursor ->
                    buildList {
                        while (cursor.moveToNext() && size < limit) {
                            val title = cursor.getString(0)?.takeIf { it.isNotBlank() } ?: "(sin título)"
                            add(
                                AgendaEvent(
                                    title = title,
                                    startMs = cursor.getLong(1),
                                    endMs = cursor.getLong(2),
                                    allDay = cursor.getInt(3) != 0,
                                    color = cursor.getInt(4),
                                ),
                            )
                        }
                    }
                }.orEmpty()
            }.getOrDefault(emptyList())
        }
}
