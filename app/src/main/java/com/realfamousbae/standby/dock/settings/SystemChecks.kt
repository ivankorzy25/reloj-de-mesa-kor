package com.realfamousbae.standby.dock.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

/** Chequeos de los permisos y ajustes del sistema. Es la base de la guía de la primera vez. */
object SystemChecks {

    fun canDrawOverlays(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun isIgnoringBatteryOptimizations(context: Context): Boolean =
        context.getSystemService(PowerManager::class.java)
            .isIgnoringBatteryOptimizations(context.packageName)

    /** Antes de Android 13 no existía el permiso de notificaciones: lo damos por dado. */
    fun canPostNotifications(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Si está prendido el "Protector de pantalla" del sistema (Settings.Secure `screensaver_enabled`).
     * La clave no es pública; si el firmware no deja leerla, devuelve null ("desconocido").
     */
    fun isScreensaverEnabled(context: Context): Boolean? = try {
        Settings.Secure.getInt(context.contentResolver, "screensaver_enabled") == 1
    } catch (_: Exception) {
        null
    }

    private fun packageUri(context: Context) = "package:${context.packageName}".toUri()

    fun overlaySettings(context: Context) =
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, packageUri(context))

    /**
     * Diálogo del sistema: "¿Permitir que funcione en segundo plano sin restricciones?". Para
     * Google Play eso no se puede pedir, pero esta app se instala a mano (ver docs/PLAN.md).
     */
    @android.annotation.SuppressLint("BatteryLife")
    fun requestIgnoreBatteryOptimizations(context: Context) =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, packageUri(context))

    /** Info de la app: en One UI, desde ahí se llega a "Batería → Sin restricciones". */
    fun appDetails(context: Context) =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri(context))

    fun screensaverSettings() = Intent(Settings.ACTION_DREAM_SETTINGS)
}
