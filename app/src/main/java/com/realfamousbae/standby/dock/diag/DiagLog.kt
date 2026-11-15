package com.realfamousbae.standby.dock.diag

import android.util.Log
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Registro de diagnóstico para probar en el teléfono: repite lo que va a logcat y guarda las
 * últimas líneas en un archivo de la app, para poder mirarlas en la pantalla de
 * diagnóstico sin computadora. El archivo queda solo en el teléfono, no va al repositorio.
 */
class DiagLog(private val file: File, private val maxLines: Int = 300) {

    private val lock = Any()

    fun log(message: String) {
        Log.i(TAG, message)
        val line = "${LocalDateTime.now().format(FORMAT)} $message"
        synchronized(lock) {
            val kept = readUnlocked().takeLast(maxLines - 1)
            file.writeText((kept + line).joinToString("\n"))
        }
    }

    fun lines(): List<String> = synchronized(lock) { readUnlocked() }

    fun clear() = synchronized(lock) { file.delete(); Unit }

    private fun readUnlocked(): List<String> =
        if (file.exists()) file.readLines().filter { it.isNotBlank() } else emptyList()

    companion object {
        const val TAG = "StandBy"
        private val FORMAT = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss")
    }
}
