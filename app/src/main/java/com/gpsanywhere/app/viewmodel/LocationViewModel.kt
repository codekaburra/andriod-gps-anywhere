package com.gpsanywhere.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.gpsanywhere.app.data.AppDatabase
import com.gpsanywhere.app.data.DefaultLocationSeeder
import com.gpsanywhere.app.data.DefaultSavedRouteSeeder
import com.gpsanywhere.app.data.SavedLocation
import com.gpsanywhere.app.location.CurrentLocationProvider
import com.gpsanywhere.app.routes.SpiralWalkGenerator
import com.gpsanywhere.app.service.SpoofService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LocationViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        const val MAX_SPEED_KMH = 5000f
        const val SPIRAL_RESET_INTERVAL_MS = 10L * 60L * 1000L

        /** Preset cruising speeds for the Fly buttons. */
        const val FLY_HELI_KMH = 200f
        const val FLY_FLIGHT_KMH = 1000f
        const val FLY_ROCKET_KMH = 5000f

        /** Speed of the spiral walk that starts automatically once a fly reaches its target. */
        const val FLY_SPIRAL_KMH = 16f

        const val MOVE_STEP_DEG = 0.0005
    }

    private val dao = AppDatabase.getInstance(application).savedLocationDao()

    /** All saved locations (prebuilt + custom), all editable/deletable. */
    val allLocations: LiveData<List<SavedLocation>> = dao.observeAll()
    val isSpoofing: LiveData<Boolean> = SpoofService.isRunning

    private val _spiralSpeedKmh = MutableStateFlow(16f)
    val spiralSpeedKmh: StateFlow<Float> = _spiralSpeedKmh.asStateFlow()

    private val _routeHints = MutableStateFlow<Map<String, String>>(emptyMap())
    val routeHints: StateFlow<Map<String, String>> = _routeHints.asStateFlow()

    init {
        CurrentLocationProvider.ensureStarted(getApplication())
        // Prebuilt data is no longer auto-imported; the user imports it from Settings.
        viewModelScope.launch(Dispatchers.IO) {
            _routeHints.value = buildRouteHints()
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
                    tags = tags
                )
            )
        }
    }

    fun updateLocation(location: SavedLocation, name: String, latitude: Double, longitude: Double, tags: String = location.tags) {
        viewModelScope.launch {
            dao.update(location.copy(name = name.trim(), latitude = latitude, longitude = longitude, tags = tags))
        }
    }

    fun deleteLocation(location: SavedLocation) {
        viewModelScope.launch {
            dao.delete(location)
        }
    }

    fun startSpoofing(location: SavedLocation) {
        startSpoofing(location.latitude, location.longitude)
    }

    fun startSpiralWalk(location: SavedLocation) =
        startSpiralWalk(location.latitude, location.longitude)

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
            resetIntervalMs = SPIRAL_RESET_INTERVAL_MS
        )
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
            resetIntervalMs = SPIRAL_RESET_INTERVAL_MS,
            spiralAfterKmh = FLY_SPIRAL_KMH
        )
    }

    fun flyTo(location: SavedLocation, speedKmh: Float = MAX_SPEED_KMH) =
        flyTo(location.latitude, location.longitude, speedKmh)

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
