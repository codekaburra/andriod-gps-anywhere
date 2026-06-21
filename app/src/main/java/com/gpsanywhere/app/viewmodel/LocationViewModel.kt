package com.gpsanywhere.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.gpsanywhere.app.data.AppDatabase
import com.gpsanywhere.app.data.DefaultLocationSeeder
import com.gpsanywhere.app.data.DefaultLocationSeeder.DefaultLocationAsset
import com.gpsanywhere.app.data.DefaultLocationSeeder.DefaultLocationPack
import com.gpsanywhere.app.data.DefaultSavedRouteSeeder
import com.gpsanywhere.app.data.SavedLocation
import com.gpsanywhere.app.location.CurrentLocationProvider
import com.gpsanywhere.app.routes.SpiralWalkGenerator
import com.gpsanywhere.app.service.SpoofService
import com.gpsanywhere.app.settings.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LocationViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        const val MAX_SPEED_KMH = 5000f

        /** Bounds for the user-configurable spiral reset interval. */
        const val MIN_SPIRAL_RESET_MIN = 1
        const val MAX_SPIRAL_RESET_MIN = 180

        /** Preset cruising speeds for the Fly buttons. */
        const val FLY_HELI_KMH = 200f
        const val FLY_FLIGHT_KMH = 1000f
        const val FLY_ROCKET_KMH = 5000f

        /** Speed of the spiral walk that starts automatically once a fly reaches its target. */
        const val FLY_SPIRAL_KMH = 15f

        /** One D-pad nudge step, in degrees (~55 m at the equator). */
        const val MOVE_STEP_DEG = 0.0005
    }

    private val dao = AppDatabase.getInstance(application).savedLocationDao()

    val customLocations: LiveData<List<SavedLocation>> = dao.observeCustom()
    val isSpoofing: LiveData<Boolean> = SpoofService.isRunning

    private val _locationPacks = MutableStateFlow<List<DefaultLocationPack>>(emptyList())
    val locationPacks: StateFlow<List<DefaultLocationPack>> = _locationPacks.asStateFlow()

    private val _spiralSpeedKmh = MutableStateFlow(16f)
    val spiralSpeedKmh: StateFlow<Float> = _spiralSpeedKmh.asStateFlow()

    private val prefs = AppPreferences(application)

    private val _spiralResetMinutes = MutableStateFlow(prefs.spiralResetMinutes.also {
        SpoofService.liveResetIntervalMs = it * 60L * 1000L
    })
    /** How often (minutes) a spiral walk returns to its origin and restarts. User-configurable. */
    val spiralResetMinutes: StateFlow<Int> = _spiralResetMinutes.asStateFlow()

    private val spiralResetIntervalMs: Long
        get() = _spiralResetMinutes.value * 60L * 1000L

    fun setSpiralResetMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(MIN_SPIRAL_RESET_MIN, MAX_SPIRAL_RESET_MIN)
        _spiralResetMinutes.value = clamped
        prefs.spiralResetMinutes = clamped
        SpoofService.liveResetIntervalMs = clamped * 60L * 1000L
    }

    private val _routeHints = MutableStateFlow<Map<String, String>>(emptyMap())
    val routeHints: StateFlow<Map<String, String>> = _routeHints.asStateFlow()

    init {
        CurrentLocationProvider.ensureStarted(getApplication())
        viewModelScope.launch {
            DefaultLocationSeeder.seedIfNeeded(getApplication(), dao)
            _routeHints.value = buildRouteHints()
        }
        viewModelScope.launch(Dispatchers.IO) {
            _locationPacks.value = DefaultLocationSeeder.loadAllPacks(getApplication())
        }
    }

    fun setSpiralSpeed(speed: Float) {
        _spiralSpeedKmh.value = speed.coerceIn(0f, MAX_SPEED_KMH)
        if (SpoofService.isWalkMode.value == true) {
            SpoofService.updateSpeed(getApplication(), _spiralSpeedKmh.value)
        }
    }

    fun addLocation(name: String, latitude: Double, longitude: Double, tags: String = "") {
        viewModelScope.launch {
            dao.insert(
                SavedLocation(
                    sourceId = null,
                    name = name.trim(),
                    latitude = latitude,
                    longitude = longitude,
                    tags = normalizeTags(tags)
                )
            )
        }
    }

    fun updateLocation(
        location: SavedLocation,
        name: String,
        latitude: Double,
        longitude: Double,
        tags: String = location.tags
    ) {
        viewModelScope.launch {
            dao.update(
                location.copy(
                    name = name.trim(),
                    latitude = latitude,
                    longitude = longitude,
                    tags = normalizeTags(tags)
                )
            )
        }
    }

    /** Accepts comma- or pipe-separated tag input and stores it pipe-separated. */
    private fun normalizeTags(raw: String): String =
        raw.split(",", "|").map { it.trim() }.filter { it.isNotEmpty() }.joinToString("|")

    fun deleteLocation(location: SavedLocation) {
        if (location.isPreinstalled) return
        viewModelScope.launch {
            dao.delete(location)
        }
    }

    fun startSpoofing(location: SavedLocation) {
        startSpoofing(location.latitude, location.longitude)
    }

    fun startSpoofing(asset: DefaultLocationAsset) {
        startSpoofing(asset.latitude, asset.longitude)
    }

    fun startSpiralWalk(location: SavedLocation) =
        startSpiralWalk(location.latitude, location.longitude)

    fun startSpiralWalk(asset: DefaultLocationAsset) =
        startSpiralWalk(asset.latitude, asset.longitude)

    fun startSpiralWalk(lat: Double, lng: Double) {
        val (lats, lngs) = SpiralWalkGenerator.generate(lat, lng)
        SpoofService.startWalk(
            getApplication(),
            lats = lats,
            lngs = lngs,
            speedKmh = _spiralSpeedKmh.value,
            minSpeedKmh = 0f,
            maxSpeedKmh = MAX_SPEED_KMH,
            varyKmh = 1f,
            loop = false,
            resetIntervalMs = spiralResetIntervalMs
        )
    }

    /**
     * Nudge the spiral's centre by [dLatDeg]/[dLngDeg] degrees from the current position
     * (spoofed if active, otherwise the real device location) and restart the spiral there.
     */
    fun nudgeSpiral(dLatDeg: Double, dLngDeg: Double) {
        val spoofLat = SpoofService.currentLat.value ?: 0.0
        val spoofLng = SpoofService.currentLng.value ?: 0.0
        val baseLat: Double
        val baseLng: Double
        if (spoofLat != 0.0 || spoofLng != 0.0) {
            baseLat = spoofLat
            baseLng = spoofLng
        } else {
            baseLat = CurrentLocationProvider.latitude.value ?: return
            baseLng = CurrentLocationProvider.longitude.value ?: return
        }
        startSpiralWalk(baseLat + dLatDeg, baseLng + dLngDeg)
    }

    fun flyTo(lat: Double, lng: Double, speedKmh: Float = MAX_SPEED_KMH) {
        // The slider reflects the sustained speed: the spiral the app settles into after arrival.
        _spiralSpeedKmh.value = FLY_SPIRAL_KMH
        val spoofLat = SpoofService.currentLat.value ?: 0.0
        val spoofLng = SpoofService.currentLng.value ?: 0.0
        val fromLat: Double
        val fromLng: Double
        if (spoofLat != 0.0 || spoofLng != 0.0) {
            fromLat = spoofLat
            fromLng = spoofLng
        } else {
            fromLat = CurrentLocationProvider.latitude.value ?: return
            fromLng = CurrentLocationProvider.longitude.value ?: return
        }
        SpoofService.startWalk(
            getApplication(),
            lats = doubleArrayOf(fromLat, lat),
            lngs = doubleArrayOf(fromLng, lng),
            speedKmh = speedKmh.coerceIn(0f, MAX_SPEED_KMH),
            minSpeedKmh = 0f,
            maxSpeedKmh = MAX_SPEED_KMH,
            varyKmh = 0f,
            loop = false,
            resetIntervalMs = spiralResetIntervalMs,
            spiralAfterKmh = FLY_SPIRAL_KMH
        )
    }

    fun flyTo(location: SavedLocation, speedKmh: Float = MAX_SPEED_KMH) =
        flyTo(location.latitude, location.longitude, speedKmh)

    fun flyTo(asset: DefaultLocationAsset, speedKmh: Float = MAX_SPEED_KMH) =
        flyTo(asset.latitude, asset.longitude, speedKmh)

    fun stopSpoofing() {
        SpoofService.stop(getApplication())
    }

    fun routeHintFor(location: SavedLocation, hints: Map<String, String> = _routeHints.value): String? =
        routeHintFor(location.name, location.latitude, location.longitude, hints)

    fun routeHintFor(
        name: String,
        latitude: Double,
        longitude: Double,
        hints: Map<String, String> = _routeHints.value
    ): String? = hints[locationKey(name, latitude, longitude)]

    fun startSpoofing(latitude: Double, longitude: Double) {
        SpoofService.startFixed(getApplication(), latitude, longitude)
    }

    private fun buildRouteHints(): Map<String, String> {
        val hints = linkedMapOf<String, String>()
        DefaultSavedRouteSeeder.loadAllAssets(getApplication()).forEach { route ->
            route.coordinates.forEach { point ->
                val key = locationKey(point.name, point.latitude, point.longitude)
                hints.putIfAbsent(key, route.routeName)
            }
        }
        return hints
    }

    private fun locationKey(name: String, latitude: Double, longitude: Double): String =
        "${name.trim()}|${"%.6f".format(latitude)}|${"%.6f".format(longitude)}"
}
