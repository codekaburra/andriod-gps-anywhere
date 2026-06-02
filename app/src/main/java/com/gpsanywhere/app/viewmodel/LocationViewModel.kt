package com.gpsanywhere.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.gpsanywhere.app.service.SpoofService
import com.gpsanywhere.app.settings.HistoryEntry
import com.gpsanywhere.app.settings.LocationHistoryStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LocationViewModel(application: Application) : AndroidViewModel(application) {

    val isSpoofing: LiveData<Boolean> = SpoofService.isRunning
    val isPaused: LiveData<Boolean> = SpoofService.isPaused

    private val historyStore = LocationHistoryStore(application)

    private val _latitude = MutableStateFlow(
        SpoofService.currentLat.value?.takeIf { it != 0.0 }
            ?.toBigDecimal()?.stripTrailingZeros()?.toPlainString() ?: "25.0330"
    )
    val latitude: StateFlow<String> = _latitude.asStateFlow()

    private val _longitude = MutableStateFlow(
        SpoofService.currentLng.value?.takeIf { it != 0.0 }
            ?.toBigDecimal()?.stripTrailingZeros()?.toPlainString() ?: "121.5654"
    )
    val longitude: StateFlow<String> = _longitude.asStateFlow()

    private val _inputError = MutableStateFlow<String?>(null)
    val inputError: StateFlow<String?> = _inputError.asStateFlow()

    private val _locationHistory = MutableStateFlow<List<HistoryEntry>>(historyStore.load())
    val locationHistory: StateFlow<List<HistoryEntry>> = _locationHistory.asStateFlow()

    fun setLatitude(value: String) {
        _latitude.value = value
        _inputError.value = null
    }

    fun setLongitude(value: String) {
        _longitude.value = value
        _inputError.value = null
    }

    fun setCoordinates(lat: Double, lng: Double) {
        _latitude.value = lat.toBigDecimal().stripTrailingZeros().toPlainString()
        _longitude.value = lng.toBigDecimal().stripTrailingZeros().toPlainString()
        _inputError.value = null
    }

    fun clearInputError() {
        _inputError.value = null
    }

    fun startSpoofing(): Boolean {
        val lat = _latitude.value.toDoubleOrNull()
        val lng = _longitude.value.toDoubleOrNull()

        if (lat == null || lng == null) {
            _inputError.value = "Please enter valid numbers for latitude and longitude."
            return false
        }
        if (lat !in -90.0..90.0) {
            _inputError.value = "Latitude must be between –90 and 90."
            return false
        }
        if (lng !in -180.0..180.0) {
            _inputError.value = "Longitude must be between –180 and 180."
            return false
        }

        if (SpoofService.isPaused.value == true) {
            SpoofService.resume(getApplication())
        }

        SpoofService.startFixed(getApplication(), lat, lng)

        historyStore.push(lat, lng)
        _locationHistory.value = historyStore.load()

        return true
    }

    fun deleteHistoryEntry(entry: HistoryEntry) {
        historyStore.remove(entry)
        _locationHistory.value = historyStore.load()
    }

    fun renameHistoryEntry(entry: HistoryEntry, newLabel: String) {
        historyStore.rename(entry, newLabel)
        _locationHistory.value = historyStore.load()
    }

    fun clearHistory() {
        historyStore.clear()
        _locationHistory.value = emptyList()
    }

    fun pauseSpoofing() {
        SpoofService.pause(getApplication())
    }

    fun resumeSpoofing() {
        SpoofService.resume(getApplication())
    }

    fun stopSpoofing() {
        SpoofService.stop(getApplication())
    }
}
