package com.realfamousbae.standby.dock.photos

import android.content.Context
import com.realfamousbae.standby.dock.diag.DiagLog
import com.realfamousbae.standby.dock.prefs.SettingsRepository
import kotlinx.coroutines.flow.first
import java.io.File

/**
 * Carga la dirección y la clave de Immich desde un archivo, para no tener que tipearlas en
 * el teléfono: se empuja con `adb push immich.txt
 * /sdcard/Android/data/<paquete>/files/immich.txt` y en el próximo arranque la app la toma
 * y borra el archivo (la clave queda solo en los ajustes de la app).
 *
 * Formato del archivo, una cosa por línea:
 * ```
 * url=https://fotos.generadores.ar
 * key=<clave de API>
 * ```
 */
object ImmichSeed {

    const val FILE_NAME = "immich.txt"

    suspend fun importIfPresent(context: Context, repository: SettingsRepository, log: DiagLog) {
        val file = File(context.getExternalFilesDir(null), FILE_NAME)
        if (!file.exists()) return
        val values = runCatching {
            file.readLines()
                .mapNotNull { line ->
                    val clean = line.trim()
                    if (clean.isEmpty() || clean.startsWith("#")) return@mapNotNull null
                    val index = clean.indexOf('=').takeIf { it > 0 } ?: return@mapNotNull null
                    clean.substring(0, index).trim().lowercase() to clean.substring(index + 1).trim()
                }
                .toMap()
        }.getOrElse {
            log.log("Fotos: no se pudo leer $FILE_NAME")
            return
        }
        val key = values["key"].orEmpty()
        if (key.isBlank()) {
            log.log("Fotos: $FILE_NAME sin clave, no se tocó nada")
            return
        }
        val url = values["url"]?.takeIf { it.isNotBlank() } ?: repository.settings.first().immichUrl
        repository.setImmich(url, key)
        // La clave ya está en los ajustes: el archivo en la tarjeta no tiene por qué quedar.
        file.delete()
        log.log("Fotos: dirección y clave de Immich importadas desde el archivo")
    }
}
