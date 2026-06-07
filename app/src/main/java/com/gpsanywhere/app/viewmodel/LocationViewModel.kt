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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LocationViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getInstance(application).savedLocationDao()

    val customLocations: LiveData<List<SavedLocation>> = dao.observeCustom()
    val isSpoofing: LiveData<Boolean> = SpoofService.isRunning

    private val _locationPacks = MutableStateFlow<List<DefaultLocationPack>>(emptyList())
    val locationPacks: StateFlow<List<DefaultLocationPack>> = _locationPacks.asStateFlow()

    private val _spiralSpeedKmh = MutableStateFlow(4f)
    val spiralSpeedKmh: StateFlow<Float> = _spiralSpeedKmh.asStateFlow()

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
        _spiralSpeedKmh.value = speed.coerceIn(0f, 20f)
        if (SpoofService.isWalkMode.value == true) {
            SpoofService.updateSpeed(getApplication(), _spiralSpeedKmh.value)
        }
    }

    fun addLocation(name: String, latitude: Double, longitude: Double) {
        viewModelScope.launch {
            dao.insert(
                SavedLocation(
                    sourceId = null,
                    name = name.trim(),
                    latitude = latitude,
                    longitude = longitude
                )
            )
        }
    }

    fun updateLocation(location: SavedLocation, name: String, latitude: Double, longitude: Double) {
        viewModelScope.launch {
            dao.update(location.copy(name = name.trim(), latitude = latitude, longitude = longitude))
        }
    }

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
            maxSpeedKmh = 20f,
            varyKmh = 1f,
            loop = false
        )
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

    private fun startSpoofing(latitude: Double, longitude: Double) {
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
