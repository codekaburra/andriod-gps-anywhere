package com.gpsanywhere.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.gpsanywhere.app.data.AppDatabase
import com.gpsanywhere.app.data.DefaultSavedRouteSeeder
import com.gpsanywhere.app.data.SavedRoute
import com.gpsanywhere.app.data.WaypointJson
import com.gpsanywhere.app.service.SpoofService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class WalkViewModel(application: Application) : AndroidViewModel(application) {

    private val routeDao = AppDatabase.getInstance(application).routeDao()

    val routes: LiveData<List<SavedRoute>> = routeDao.observeAll()
    val isSpoofing: LiveData<Boolean> = SpoofService.isRunning
    val currentLat: LiveData<Double> = SpoofService.currentLat
    val currentLng: LiveData<Double> = SpoofService.currentLng

    private val _speedKmh = MutableStateFlow(4f)
    val speedKmh: StateFlow<Float> = _speedKmh

    private val _activeRoute = MutableStateFlow<SavedRoute?>(null)
    val activeRoute: StateFlow<SavedRoute?> = _activeRoute

    init {
        viewModelScope.launch {
            DefaultSavedRouteSeeder.seedIfNeeded(getApplication(), routeDao)
        }
    }

    fun setSpeed(speed: Float) {
        _speedKmh.value = speed
        if (SpoofService.isRunning.value == true && _activeRoute.value != null) {
            SpoofService.updateSpeed(getApplication(), speed)
        }
    }

    fun startWalk(route: SavedRoute) {
        val points = WaypointJson.fromJson(route.waypointsJson)
        if (points.size >= 2) {
            val lats = points.map { it.latitude }.toDoubleArray()
            val lngs = points.map { it.longitude }.toDoubleArray()
            SpoofService.startWalk(getApplication(), lats, lngs, _speedKmh.value)
            _activeRoute.value = route
        } else if (points.size == 1) {
            SpoofService.startFixed(getApplication(), points[0].latitude, points[0].longitude)
            _activeRoute.value = route
        }
    }

    fun stop() {
        SpoofService.stop(getApplication())
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
