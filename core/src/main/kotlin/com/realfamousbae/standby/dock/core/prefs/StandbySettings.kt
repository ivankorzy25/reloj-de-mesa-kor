package com.realfamousbae.standby.dock.core.prefs

import com.realfamousbae.standby.dock.core.night.NightMode

/**
 * Ajustes de Iván, de los que depende la lógica. Se guardan en DataStore (capa :app);
 * acá viven solo el modelo y los valores por defecto.
 */
data class StandbySettings(
    /** Interruptor principal: en false el servicio no corre ni se levanta solo. */
    val enabled: Boolean = true,
    /** Apagado no por el interruptor, sino porque pararon la app desde afuera. */
    val stoppedExternally: Boolean = false,
    /** Inclinación máxima del lado largo respecto de la vertical para considerarlo "acostado". */
    val maxTiltDegrees: Float = 55f,
    /** Mostrar el modo solo cuando está cargando. */
    val requireCharging: Boolean = true,
    /** Sin cargador, el modo no arranca y se cierra si la batería baja de esto. */
    val minBatteryPercentOffCharger: Int = 20,
    /** Límites del brillo automático de la ventana del modo (parte del máximo, 0..1). */
    val brightnessMin: Float = 0.01f,
    val brightnessMax: Float = 0.6f,
    /** Distribución de la pantalla del modo. */
    val layout: ScreenLayout = ScreenLayout.ClockOnly,
    /** id del widget del sistema (AppWidgetHost); null = no hay. */
    val widgetId: Int? = null,
    /** Mostrar avisos de notificaciones nuevas sobre el modo. */
    val notificationBannersEnabled: Boolean = true,
    /** Apps de las que NO se muestran avisos (por defecto se muestran todas). */
    val notificationBlockedPackages: Set<String> = emptySet(),
    /**
     * Mientras el modo está en pantalla, ocultar las notificaciones emergentes del sistema
     * (con nuestro propio "No molestar": llamadas y alarmas pasan, el resto sin sonido).
     */
    val hideSystemHeadsUp: Boolean = true,

    // --- Cara del reloj (se pasa con el dedo, se recuerda la última) ---
    val clockFace: ClockFace = ClockFace.Digital,
    /** Tarjeta de la derecha (o de la primera columna) que quedó elegida. */
    val sideCard: SideCard = SideCard.Clima,
    /** Tarjeta de la segunda columna del layout "Dos columnas". */
    val sideCard2: SideCard = SideCard.AlarmaBateria,

    // --- Personalización del reloj ---
    val clockColor: Long = ClockColors.GRIS,
    val clockFont: ClockFont = ClockFont.CondensadaFina,
    /** Tamaño de la hora, en porcentaje del tamaño base. */
    val clockSizePercent: Int = 100,
    val showSeconds: Boolean = false,
    val hourFormat: HourFormat = HourFormat.Sistema,

    // --- Modo noche ---
    val nightEnabled: Boolean = true,
    /** Minutos desde las 00:00. Por defecto, de 22:00 a 07:00. */
    val nightStartMinutes: Int = 22 * 60,
    val nightEndMinutes: Int = 7 * 60,
    /** Además del horario, prender el modo noche si el sensor ve el cuarto a oscuras. */
    val nightUseLightSensor: Boolean = true,

    // --- Clima (Open-Meteo, sin clave) ---
    val weatherLatitude: Float = QUILMES_LAT,
    val weatherLongitude: Float = QUILMES_LON,
    val weatherPlace: String = "Quilmes",

    // --- Fotos (Immich) ---
    val immichUrl: String = "https://fotos.generadores.ar",
    val immichApiKey: String = "",
    val immichOnlyFavorites: Boolean = false,

    /** Ya pasó por el asistente de permisos de la primera vez. */
    val onboardingDone: Boolean = false,
) {
    /** Hay con qué dibujar la cara de fotos. */
    val immichReady: Boolean get() = immichApiKey.isNotBlank() && immichUrl.isNotBlank()

    /** Modo noche prendido ahora mismo. */
    fun nightActive(nowMinutes: Int, lux: Float?): Boolean = NightMode.isActive(
        enabled = nightEnabled,
        nowMinutes = nowMinutes,
        startMinutes = nightStartMinutes,
        endMinutes = nightEndMinutes,
        useLightSensor = nightUseLightSensor,
        lux = lux,
    )

    companion object {
        const val TILT_RANGE_MIN = 15f
        const val TILT_RANGE_MAX = 75f
        const val BATTERY_RANGE_MIN = 5
        const val BATTERY_RANGE_MAX = 80
        const val CLOCK_SIZE_MIN = 60
        const val CLOCK_SIZE_MAX = 130

        /** Quilmes, provincia de Buenos Aires. */
        const val QUILMES_LAT = -34.7203f
        const val QUILMES_LON = -58.2545f
    }
}
