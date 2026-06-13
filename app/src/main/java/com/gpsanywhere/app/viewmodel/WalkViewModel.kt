package com.gpsanywhere.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.gpsanywhere.app.data.AppDatabase
import com.gpsanywhere.app.data.DefaultSavedRouteSeeder
import com.gpsanywhere.app.data.DefaultSavedRouteSeeder.DefaultRouteAsset
import com.gpsanywhere.app.data.SavedRoute
import com.gpsanywhere.app.data.WaypointJson
import com.gpsanywhere.app.location.CurrentLocationProvider
import com.gpsanywhere.app.routes.LocationPoint
import com.gpsanywhere.app.service.SpoofService
import kotlinx.coroutines.Dispatchers
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

    private val _defaultRoutes = MutableStateFlow<List<DefaultRouteAsset>>(emptyList())
    val defaultRoutes: StateFlow<List<DefaultRouteAsset>> = _defaultRoutes

    init {
        CurrentLocationProvider.ensureStarted(getApplication())
        viewModelScope.launch {
            DefaultSavedRouteSeeder.seedIfNeeded(getApplication(), routeDao)
        }
        viewModelScope.launch(Dispatchers.IO) {
            _defaultRoutes.value = DefaultSavedRouteSeeder.loadAllAssets(getApplication())
        }
    }

    fun deleteRoute(route: SavedRoute) {
        viewModelScope.launch {
            routeDao.delete(route)
        }
    }

    fun startDefaultRoute(asset: DefaultRouteAsset) {
        val points = asset.toLocationPoints()
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
        }
    }

    fun saveDefaultRoute(asset: DefaultRouteAsset) {
        viewModelScope.launch {
            val points = asset.toLocationPoints()
            if (points.isNotEmpty() && routeDao.countByName(asset.routeName) == 0) {
                routeDao.insert(
                    SavedRoute(
                        name = asset.routeName,
                        waypointsJson = WaypointJson.toJson(points),
                        routeMethod = "MANUAL_MAP",
                        distanceMeters = estimateDistance(points)
                    )
                )
            }
        }
    }

    fun defaultRouteDistanceKm(asset: DefaultRouteAsset): String {
        val dist = estimateDistance(asset.toLocationPoints())
        return "%.1f km".format(dist / 1000.0)
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

    fun startWalk(route: SavedRoute, reversed: Boolean = false) {
        val points = WaypointJson.fromJson(route.waypointsJson)
            .let { if (reversed) it.reversed() else it }
        if (points.size >= 2) {
            val lats = points.map { it.latitude }.toDoubleArray()
            val lngs = points.map { it.longitude }.toDoubleArray()
            SpoofService.startWalk(
                getApplication(), lats, lngs,
                speedKmh = _speedKmh.value,
                minSpeedKmh = _minSpeedKmh.value,
                maxSpeedKmh = _maxSpeedKmh.value,
                varyKmh = _varyKmh.value,
                loop = true
            )
            _activeRoute.value = route
        } else if (points.size == 1) {
            SpoofService.startFixed(getApplication(), points[0].latitude, points[0].longitude)
            _activeRoute.value = route
        }
    }

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

    fun jumpToWaypoint(index: Int, route: SavedRoute) {
        val points = WaypointJson.fromJson(route.waypointsJson)
        if (index in points.indices) {
            val p = points[index]
            SpoofService.jumpTo(getApplication(), p.latitude, p.longitude)
        }
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

    private fun estimateDistance(points: List<LocationPoint>): Double {
        if (points.size < 2) return 0.0
        var total = 0.0
        for (i in 0 until points.size - 1) {
            val a = points[i]
            val b = points[i + 1]
            val results = FloatArray(1)
            android.location.Location.distanceBetween(
                a.latitude, a.longitude,
                b.latitude, b.longitude,
                results
            )
            total += results[0]
        }
        return total.toDouble()
    }
}
