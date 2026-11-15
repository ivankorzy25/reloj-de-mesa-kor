package com.realfamousbae.standby.dock.prefs

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.realfamousbae.standby.dock.core.place.PlaceMemory
import com.realfamousbae.standby.dock.core.place.PlacePrefs
import com.realfamousbae.standby.dock.core.prefs.ClockFace
import com.realfamousbae.standby.dock.core.prefs.ClockFont
import com.realfamousbae.standby.dock.core.prefs.HourFormat
import com.realfamousbae.standby.dock.core.prefs.ScreenLayout
import com.realfamousbae.standby.dock.core.prefs.SideCard
import com.realfamousbae.standby.dock.core.prefs.StandbySettings
import com.realfamousbae.standby.dock.core.prefs.WorldCities
import com.realfamousbae.standby.dock.core.prefs.WorldCity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "standby_settings")

/**
 * Guarda [StandbySettings] en DataStore. Una sola instancia por proceso (vive en AppGraph):
 * DataStore no permite dos instancias sobre el mismo archivo.
 */
class SettingsRepository(context: Context) {

    private val store = context.applicationContext.dataStore
    private val defaults = StandbySettings()

    val settings: Flow<StandbySettings> = store.data
        .map { p ->
            StandbySettings(
                enabled = p[ENABLED] ?: defaults.enabled,
                stoppedExternally = p[STOPPED_EXTERNALLY] ?: defaults.stoppedExternally,
                maxTiltDegrees = p[MAX_TILT] ?: defaults.maxTiltDegrees,
                requireCharging = p[REQUIRE_CHARGING] ?: defaults.requireCharging,
                minBatteryPercentOffCharger = p[MIN_BATTERY] ?: defaults.minBatteryPercentOffCharger,
                brightnessMin = p[BRIGHTNESS_MIN] ?: defaults.brightnessMin,
                brightnessMax = p[BRIGHTNESS_MAX] ?: defaults.brightnessMax,
                layout = ScreenLayout.parse(p[LAYOUT]),
                widgetId = p[WIDGET_ID],
                notificationBannersEnabled = p[BANNERS_ENABLED] ?: defaults.notificationBannersEnabled,
                notificationBlockedPackages = p[BLOCKED_PACKAGES] ?: defaults.notificationBlockedPackages,
                hideSystemHeadsUp = p[HIDE_SYSTEM_HEADS_UP] ?: defaults.hideSystemHeadsUp,
                liveActivitiesEnabled = p[LIVE_ENABLED] ?: defaults.liveActivitiesEnabled,
                liveMusicFullScreen = p[LIVE_MUSIC] ?: defaults.liveMusicFullScreen,
                clockFace = ClockFace.parse(p[CLOCK_FACE]),
                sideCard = SideCard.parse(p[SIDE_CARD]),
                sideCard2 = SideCard.parseSecond(p[SIDE_CARD_2]),
                clockColor = p[CLOCK_COLOR] ?: defaults.clockColor,
                clockFont = ClockFont.parse(p[CLOCK_FONT]),
                clockSizePercent = p[CLOCK_SIZE] ?: defaults.clockSizePercent,
                showSeconds = p[SHOW_SECONDS] ?: defaults.showSeconds,
                hourFormat = HourFormat.parse(p[HOUR_FORMAT]),
                worldCities = WorldCities.decode(p[WORLD_CITIES]),
                placeMemoryEnabled = p[PLACE_MEMORY] ?: defaults.placeMemoryEnabled,
                places = PlaceMemory.decode(p[PLACES]),
                nightEnabled = p[NIGHT_ENABLED] ?: defaults.nightEnabled,
                nightStartMinutes = p[NIGHT_START] ?: defaults.nightStartMinutes,
                nightEndMinutes = p[NIGHT_END] ?: defaults.nightEndMinutes,
                nightUseLightSensor = p[NIGHT_LIGHT_SENSOR] ?: defaults.nightUseLightSensor,
                weatherLatitude = p[WEATHER_LAT] ?: defaults.weatherLatitude,
                weatherLongitude = p[WEATHER_LON] ?: defaults.weatherLongitude,
                weatherPlace = p[WEATHER_PLACE] ?: defaults.weatherPlace,
                immichUrl = p[IMMICH_URL] ?: defaults.immichUrl,
                immichApiKey = p[IMMICH_KEY] ?: defaults.immichApiKey,
                immichOnlyFavorites = p[IMMICH_FAVORITES] ?: defaults.immichOnlyFavorites,
                onboardingDone = p[ONBOARDING_DONE] ?: defaults.onboardingDone,
            )
        }
        .distinctUntilChanged()

    /** Interruptor principal. [external] = lo apagó una parada externa, no Iván. */
    suspend fun setEnabled(value: Boolean, external: Boolean = false) {
        store.edit {
            it[ENABLED] = value
            it[STOPPED_EXTERNALLY] = !value && external
        }
    }

    /** Marca de tiempo del último cierre de proceso ya contabilizado (ApplicationExitInfo). */
    suspend fun lastHandledExitMs(): Long = store.data.first()[LAST_HANDLED_EXIT_MS] ?: 0L
    suspend fun setLastHandledExitMs(value: Long) = set(LAST_HANDLED_EXIT_MS, value)

    suspend fun setMaxTilt(value: Float) = set(MAX_TILT, value)
    suspend fun setRequireCharging(value: Boolean) = set(REQUIRE_CHARGING, value)
    suspend fun setMinBattery(value: Int) = set(MIN_BATTERY, value)
    suspend fun setLayout(value: ScreenLayout) = set(LAYOUT, value.name)
    suspend fun setBannersEnabled(value: Boolean) = set(BANNERS_ENABLED, value)
    suspend fun setHideSystemHeadsUp(value: Boolean) = set(HIDE_SYSTEM_HEADS_UP, value)
    suspend fun setLiveActivities(value: Boolean) = set(LIVE_ENABLED, value)
    suspend fun setLiveMusicFullScreen(value: Boolean) = set(LIVE_MUSIC, value)

    suspend fun setClockFace(value: ClockFace) = set(CLOCK_FACE, value.name)
    suspend fun setSideCard(value: SideCard) = set(SIDE_CARD, value.name)
    suspend fun setSideCard2(value: SideCard) = set(SIDE_CARD_2, value.name)
    suspend fun setClockColor(value: Long) = set(CLOCK_COLOR, value)
    suspend fun setClockFont(value: ClockFont) = set(CLOCK_FONT, value.name)
    suspend fun setClockSize(value: Int) = set(CLOCK_SIZE, value)
    suspend fun setShowSeconds(value: Boolean) = set(SHOW_SECONDS, value)
    suspend fun setHourFormat(value: HourFormat) = set(HOUR_FORMAT, value.name)
    suspend fun setWorldCities(value: List<WorldCity>) = set(WORLD_CITIES, WorldCities.encode(value))

    suspend fun setPlaceMemory(value: Boolean) = set(PLACE_MEMORY, value)

    /** Guardar cómo quedó un lugar (la cara y las tarjetas que están puestas ahora). */
    suspend fun rememberPlace(key: String, prefs: PlacePrefs) {
        store.edit {
            val places = PlaceMemory.decode(it[PLACES])
            if (places[key] == prefs) return@edit
            it[PLACES] = PlaceMemory.encode(PlaceMemory.remember(places, key, prefs))
        }
    }

    /** Olvidar todos los lugares (el botón de Ajustes). */
    suspend fun forgetPlaces() {
        store.edit { it.remove(PLACES) }
    }

    suspend fun setNightEnabled(value: Boolean) = set(NIGHT_ENABLED, value)
    suspend fun setNightStart(value: Int) = set(NIGHT_START, value)
    suspend fun setNightEnd(value: Int) = set(NIGHT_END, value)
    suspend fun setNightUseLightSensor(value: Boolean) = set(NIGHT_LIGHT_SENSOR, value)

    suspend fun setWeatherPlace(latitude: Float, longitude: Float, place: String) {
        store.edit {
            it[WEATHER_LAT] = latitude
            it[WEATHER_LON] = longitude
            it[WEATHER_PLACE] = place
        }
    }

    suspend fun setImmich(url: String, apiKey: String) {
        store.edit {
            it[IMMICH_URL] = url.trim().trimEnd('/')
            it[IMMICH_KEY] = apiKey.trim()
        }
    }

    suspend fun setImmichOnlyFavorites(value: Boolean) = set(IMMICH_FAVORITES, value)
    suspend fun setOnboardingDone(value: Boolean) = set(ONBOARDING_DONE, value)

    suspend fun setWidgetId(value: Int?) {
        store.edit { if (value == null) it.remove(WIDGET_ID) else it[WIDGET_ID] = value }
    }

    /** Permitir o tapar los avisos de una app. */
    suspend fun setPackageBlocked(packageName: String, blocked: Boolean) {
        store.edit {
            val current = it[BLOCKED_PACKAGES].orEmpty()
            it[BLOCKED_PACKAGES] = if (blocked) current + packageName else current - packageName
        }
    }

    private suspend fun <T> set(key: Preferences.Key<T>, value: T) {
        store.edit { it[key] = value }
    }

    private companion object {
        val ENABLED = booleanPreferencesKey("enabled")
        val STOPPED_EXTERNALLY = booleanPreferencesKey("stopped_externally")
        val LAST_HANDLED_EXIT_MS = longPreferencesKey("last_handled_exit_ms")
        val MAX_TILT = floatPreferencesKey("max_tilt_degrees")
        val REQUIRE_CHARGING = booleanPreferencesKey("require_charging")
        val MIN_BATTERY = intPreferencesKey("min_battery_percent_off_charger")
        val BRIGHTNESS_MIN = floatPreferencesKey("brightness_min")
        val BRIGHTNESS_MAX = floatPreferencesKey("brightness_max")
        val LAYOUT = stringPreferencesKey("screen_layout")
        val WIDGET_ID = intPreferencesKey("widget_id")
        val BANNERS_ENABLED = booleanPreferencesKey("notification_banners_enabled")
        val BLOCKED_PACKAGES = stringSetPreferencesKey("notification_blocked_packages")
        val HIDE_SYSTEM_HEADS_UP = booleanPreferencesKey("hide_system_heads_up")
        val LIVE_ENABLED = booleanPreferencesKey("live_activities_enabled")
        val LIVE_MUSIC = booleanPreferencesKey("live_music_full_screen")
        val CLOCK_FACE = stringPreferencesKey("clock_face")
        val SIDE_CARD = stringPreferencesKey("side_card")
        val SIDE_CARD_2 = stringPreferencesKey("side_card_2")
        val CLOCK_COLOR = longPreferencesKey("clock_color")
        val CLOCK_FONT = stringPreferencesKey("clock_font")
        val CLOCK_SIZE = intPreferencesKey("clock_size_percent")
        val SHOW_SECONDS = booleanPreferencesKey("show_seconds")
        val HOUR_FORMAT = stringPreferencesKey("hour_format")
        val WORLD_CITIES = stringPreferencesKey("world_cities")
        val PLACE_MEMORY = booleanPreferencesKey("place_memory_enabled")
        val PLACES = stringPreferencesKey("places")
        val NIGHT_ENABLED = booleanPreferencesKey("night_enabled")
        val NIGHT_START = intPreferencesKey("night_start_minutes")
        val NIGHT_END = intPreferencesKey("night_end_minutes")
        val NIGHT_LIGHT_SENSOR = booleanPreferencesKey("night_use_light_sensor")
        val WEATHER_LAT = floatPreferencesKey("weather_latitude")
        val WEATHER_LON = floatPreferencesKey("weather_longitude")
        val WEATHER_PLACE = stringPreferencesKey("weather_place")
        val IMMICH_URL = stringPreferencesKey("immich_url")
        val IMMICH_KEY = stringPreferencesKey("immich_api_key")
        val IMMICH_FAVORITES = booleanPreferencesKey("immich_only_favorites")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
    }
}
