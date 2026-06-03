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
import com.gpsanywhere.app.service.SpoofService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SavedRoutesViewModel(application: Application) : AndroidViewModel(application) {

    private val routeDao = AppDatabase.getInstance(application).routeDao()

    val routes: LiveData<List<SavedRoute>> = routeDao.observeAll()
    val isSpoofing: LiveData<Boolean> = SpoofService.isRunning

    private val _defaultRoutes = MutableStateFlow<List<DefaultRouteAsset>>(emptyList())
    val defaultRoutes: StateFlow<List<DefaultRouteAsset>> = _defaultRoutes

    init {
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

    fun startRoute(route: SavedRoute) {
        val points = WaypointJson.fromJson(route.waypointsJson)
        if (points.size >= 2) {
            val lats = points.map { it.latitude }.toDoubleArray()
            val lngs = points.map { it.longitude }.toDoubleArray()
            SpoofService.startWalk(getApplication(), lats, lngs, route.speedKmh.toFloat())
        } else if (points.size == 1) {
            SpoofService.startFixed(getApplication(), points[0].latitude, points[0].longitude)
        }
    }

    fun startDefaultRoute(asset: DefaultRouteAsset, speedKmh: Float = 4f) {
        val points = asset.toLocationPoints()
        if (points.size >= 2) {
            val lats = points.map { it.latitude }.toDoubleArray()
            val lngs = points.map { it.longitude }.toDoubleArray()
            SpoofService.startWalk(getApplication(), lats, lngs, speedKmh)
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
                        speedKmh = 4.0,
                        routeMethod = "MANUAL_MAP",
                        distanceMeters = estimateDistance(points)
                    )
                )
            }
        }
    }

    fun waypointCount(route: SavedRoute): Int =
        WaypointJson.fromJson(route.waypointsJson).size

    fun distanceKm(route: SavedRoute): String =
        String.format("%.1f km", route.distanceMeters / 1000.0)

    fun defaultRouteDistanceKm(asset: DefaultRouteAsset): String {
        val points = asset.toLocationPoints()
        val dist = estimateDistance(points)
        return String.format("%.1f km", dist / 1000.0)
    }

    private fun estimateDistance(points: List<com.gpsanywhere.app.routes.LocationPoint>): Double {
        if (points.size < 2) return 0.0
        var total = 0.0
        for (i in 0 until points.size - 1) {
            val a = points[i]; val b = points[i + 1]
            val results = FloatArray(1)
            android.location.Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, results)
            total += results[0]
        }
        return total.toDouble()
    }
}
