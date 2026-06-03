package com.gpsanywhere.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.gpsanywhere.app.data.AppDatabase
import com.gpsanywhere.app.data.DefaultLocationSeeder
import com.gpsanywhere.app.data.SavedLocation
import com.gpsanywhere.app.service.SpoofService
import kotlinx.coroutines.launch

class SavedLocationsViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getInstance(application).savedLocationDao()

    val locations: LiveData<List<SavedLocation>> = dao.observeAll()
    val isSpoofing: LiveData<Boolean> = SpoofService.isRunning

    init {
        viewModelScope.launch {
            DefaultLocationSeeder.seedIfNeeded(getApplication(), dao)
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
    }

    fun stopSpoofing() {
        SpoofService.stop(getApplication())
    }
}
