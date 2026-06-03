package com.gpsanywhere.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.gpsanywhere.app.data.AppDatabase
import com.gpsanywhere.app.data.DefaultSavedRouteSeeder
import com.gpsanywhere.app.data.SavedRoute
import com.gpsanywhere.app.data.WaypointJson
import com.gpsanywhere.app.location.CurrentLocationProvider
import com.gpsanywhere.app.service.SpoofService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class WalkViewModel(application: Application) : AndroidViewModel(application) {

    private val routeDao = AppDatabase.getInstance(application).routeDao()

    val routes: LiveData<List<SavedRoute>> = routeDao.observeAll()
    val isSpoofing: LiveData<Boolean> = SpoofService.isRunning
    val isPaused: LiveData<Boolean> = SpoofService.isPaused
    val currentLat: LiveData<Double> = SpoofService.currentLat
    val currentLng: LiveData<Double> = SpoofService.currentLng
    val currentSpeedKmh: LiveData<Float> = SpoofService.currentSpeedKmh

    val mapCenterLat: LiveData<Double?> = CurrentLocationProvider.latitude
    val mapCenterLng: LiveData<Double?> = CurrentLocationProvider.longitude

    private val _speedKmh = MutableStateFlow(4f)
    val speedKmh: StateFlow<Float> = _speedKmh

    private val _minSpeedKmh = MutableStateFlow(0f)
    val minSpeedKmh: StateFlow<Float> = _minSpeedKmh

    private val _maxSpeedKmh = MutableStateFlow(20f)
    val maxSpeedKmh: StateFlow<Float> = _maxSpeedKmh

    private val _varyKmh = MutableStateFlow(1f)
    val varyKmh: StateFlow<Float> = _varyKmh

    private val _activeRoute = MutableStateFlow<SavedRoute?>(null)
    val activeRoute: StateFlow<SavedRoute?> = _activeRoute

    init {
        CurrentLocationProvider.ensureStarted(getApplication())
        viewModelScope.launch {
            DefaultSavedRouteSeeder.seedIfNeeded(getApplication(), routeDao)
        }
    }

    fun setSpeed(speed: Float) {
        _speedKmh.value = speed
        if (SpoofService.isRunning.value == true) {
            SpoofService.updateSpeed(getApplication(), speed)
        }
    }
    fun setMinSpeed(v: Float) { _minSpeedKmh.value = v.coerceIn(0f, 20f) }
    fun setMaxSpeed(v: Float) { _maxSpeedKmh.value = v.coerceIn(0f, 20f) }
    fun setVary(v: Float) { _varyKmh.value = v.coerceAtLeast(0f) }

    fun startWalk(route: SavedRoute) {
        val points = WaypointJson.fromJson(route.waypointsJson)
        if (points.size >= 2) {
            val lats = points.map { it.latitude }.toDoubleArray()
            val lngs = points.map { it.longitude }.toDoubleArray()
            SpoofService.startWalk(
                getApplication(), lats, lngs,
                speedKmh = _speedKmh.value,
                minSpeedKmh = _minSpeedKmh.value,
                maxSpeedKmh = _maxSpeedKmh.value,
                varyKmh = _varyKmh.value
            )
            _activeRoute.value = route
        } else if (points.size == 1) {
            SpoofService.startFixed(getApplication(), points[0].latitude, points[0].longitude)
            _activeRoute.value = route
        }
    }

    fun pause() = SpoofService.pause(getApplication())
    fun resume() = SpoofService.resume(getApplication())

    /** End the walk but keep the GPS fixed at whatever position we stopped at. */
    fun stop() {
        val lat = SpoofService.currentLat.value ?: 0.0
        val lng = SpoofService.currentLng.value ?: 0.0
        if (lat != 0.0 || lng != 0.0) {
            SpoofService.startFixed(getApplication(), lat, lng)
        } else {
            SpoofService.stop(getApplication())
        }
        _activeRoute.value = null
    }

    fun distanceKm(route: SavedRoute) = "%.1f km".format(route.distanceMeters / 1000.0)
    fun waypointCount(route: SavedRoute) = WaypointJson.fromJson(route.waypointsJson).size

    fun progressLabel(route: SavedRoute, currentLat: Double, currentLng: Double): String {
        val points = WaypointJson.fromJson(route.waypointsJson)
        if (points.isEmpty()) return ""
        val currentIndex = points
            .mapIndexed { index, point ->
                val results = FloatArray(1)
                android.location.Location.distanceBetween(
                    currentLat,
                    currentLng,
                    point.latitude,
                    point.longitude,
                    results
                )
                index to results[0]
            }
            .minByOrNull { it.second }
            ?.first ?: 0
        val stopName = points[currentIndex].name?.takeIf { it.isNotBlank() }
            ?: "Waypoint ${currentIndex + 1}"
        return "${currentIndex + 1} / ${points.size} · $stopName"
    }
}
