package com.gpsanywhere.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.gpsanywhere.app.data.AppDatabase
import com.gpsanywhere.app.data.SavedRoute
import com.gpsanywhere.app.data.WaypointJson
import com.gpsanywhere.app.service.SpoofService
import kotlinx.coroutines.launch

class SavedRoutesViewModel(application: Application) : AndroidViewModel(application) {

    private val routeDao = AppDatabase.getInstance(application).routeDao()

    val routes: LiveData<List<SavedRoute>> = routeDao.observeAll()
    val isSpoofing: LiveData<Boolean> = SpoofService.isRunning

    fun deleteRoute(route: SavedRoute) {
        viewModelScope.launch {
            routeDao.delete(route)
        }
    }

    fun startRoute(route: SavedRoute) {
        val points = WaypointJson.fromJson(route.waypointsJson)
        if (points.isNotEmpty()) {
            val first = points.first()
            SpoofService.startFixed(getApplication(), first.latitude, first.longitude)
        }
    }

    fun waypointCount(route: SavedRoute): Int =
        WaypointJson.fromJson(route.waypointsJson).size

    fun distanceKm(route: SavedRoute): String =
        String.format("%.1f km", route.distanceMeters / 1000.0)
}
