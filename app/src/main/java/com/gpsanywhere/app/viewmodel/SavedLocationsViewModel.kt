package com.gpsanywhere.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.gpsanywhere.app.data.AppDatabase
import com.gpsanywhere.app.data.DefaultLocationSeeder
import com.gpsanywhere.app.data.DefaultSavedRouteSeeder
import com.gpsanywhere.app.data.SavedLocation
import com.gpsanywhere.app.service.SpoofService
import com.gpsanywhere.app.settings.HistoryEntry
import com.gpsanywhere.app.settings.LocationHistoryStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SavedLocationsViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getInstance(application).savedLocationDao()
    private val historyStore = LocationHistoryStore(application)

    val locations: LiveData<List<SavedLocation>> = dao.observeAll()
    val isSpoofing: LiveData<Boolean> = SpoofService.isRunning

    private val _history = MutableStateFlow<List<HistoryEntry>>(historyStore.load())
    val history: StateFlow<List<HistoryEntry>> = _history.asStateFlow()

    private val _routeHints = MutableStateFlow<Map<String, String>>(emptyMap())
    val routeHints: StateFlow<Map<String, String>> = _routeHints.asStateFlow()

    init {
        viewModelScope.launch {
            DefaultLocationSeeder.seedIfNeeded(getApplication(), dao)
            _routeHints.value = buildRouteHints()
        }
    }

    fun addLocation(name: String, latitude: Double, longitude: Double, category: String? = null) {
        viewModelScope.launch {
            dao.insert(
                SavedLocation(
                    sourceId = null,
                    name = name.trim(),
                    latitude = latitude,
                    longitude = longitude,
                    category = category
                )
            )
        }
    }

    fun deleteLocation(location: SavedLocation) {
        if (location.isPreinstalled) return // safety guard
        viewModelScope.launch {
            dao.delete(location)
        }
    }

    fun startSpoofing(location: SavedLocation) {
        SpoofService.startFixed(getApplication(), location.latitude, location.longitude)
        historyStore.push(location.latitude, location.longitude, location.name)
        _history.value = historyStore.load()
    }

    fun startSpoofing(entry: HistoryEntry) {
        SpoofService.startFixed(getApplication(), entry.lat, entry.lng)
        historyStore.push(entry.lat, entry.lng, entry.label)
        _history.value = historyStore.load()
    }

    fun deleteHistoryEntry(entry: HistoryEntry) {
        historyStore.remove(entry)
        _history.value = historyStore.load()
    }

    fun clearHistory() {
        historyStore.clear()
        _history.value = emptyList()
    }

    fun stopSpoofing() {
        SpoofService.stop(getApplication())
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

    fun routeHintFor(location: SavedLocation, hints: Map<String, String> = _routeHints.value): String? =
        hints[locationKey(location.name, location.latitude, location.longitude)]

    private fun locationKey(name: String, latitude: Double, longitude: Double): String =
        "${name.trim()}|${"%.6f".format(latitude)}|${"%.6f".format(longitude)}"
}
