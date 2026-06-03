package com.gpsanywhere.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.gpsanywhere.app.directions.NominatimClient
import com.gpsanywhere.app.directions.NominatimResult
import com.gpsanywhere.app.location.CurrentLocationProvider
import com.gpsanywhere.app.service.SpoofService
import androidx.lifecycle.Observer
import com.gpsanywhere.app.settings.HistoryEntry
import com.gpsanywhere.app.settings.LocationHistoryStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LocationViewModel(application: Application) : AndroidViewModel(application) {

    val isSpoofing: LiveData<Boolean> = SpoofService.isRunning
    val isPaused: LiveData<Boolean> = SpoofService.isPaused

    private val historyStore = LocationHistoryStore(application)
    private val nominatimClient = NominatimClient()

    private val _latitude = MutableStateFlow(
        CurrentLocationProvider.formatLatitude(
            SpoofService.currentLat.value?.takeIf { it != 0.0 }
        ) ?: ""
    )
    val latitude: StateFlow<String> = _latitude.asStateFlow()

    private val _longitude = MutableStateFlow(
        CurrentLocationProvider.formatLongitude(
            SpoofService.currentLng.value?.takeIf { it != 0.0 }
        ) ?: ""
    )
    val longitude: StateFlow<String> = _longitude.asStateFlow()

    private val locationObserver = Observer<Double?> {
        seedCoordinatesFromProviderIfEmpty()
    }

    init {
        CurrentLocationProvider.ensureStarted(getApplication())
        seedCoordinatesFromProviderIfEmpty()
        CurrentLocationProvider.latitude.observeForever(locationObserver)
        CurrentLocationProvider.longitude.observeForever(locationObserver)
    }

    override fun onCleared() {
        CurrentLocationProvider.latitude.removeObserver(locationObserver)
        CurrentLocationProvider.longitude.removeObserver(locationObserver)
        super.onCleared()
    }

    private fun seedCoordinatesFromProviderIfEmpty() {
        if (_latitude.value.isNotEmpty() && _longitude.value.isNotEmpty()) return
        val lat = CurrentLocationProvider.latitude.value ?: return
        val lng = CurrentLocationProvider.longitude.value ?: return
        if (_latitude.value.isEmpty()) {
            _latitude.value = CurrentLocationProvider.formatLatitude(lat) ?: return
        }
        if (_longitude.value.isEmpty()) {
            _longitude.value = CurrentLocationProvider.formatLongitude(lng) ?: return
        }
    }

    private val _inputError = MutableStateFlow<String?>(null)
    val inputError: StateFlow<String?> = _inputError.asStateFlow()

    private val _locationHistory = MutableStateFlow<List<HistoryEntry>>(historyStore.load())
    val locationHistory: StateFlow<List<HistoryEntry>> = _locationHistory.asStateFlow()

    // Online name search (Nominatim) for quickly setting a fixed location
    private val _searchResults = MutableStateFlow<List<NominatimResult>>(emptyList())
    val searchResults: StateFlow<List<NominatimResult>> = _searchResults.asStateFlow()

    private val _searchLoading = MutableStateFlow(false)
    val searchLoading: StateFlow<Boolean> = _searchLoading.asStateFlow()

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

    /**
     * Search for a location by name/address online.
     */
    fun searchByName(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _searchLoading.value = true
            _inputError.value = null
            try {
                val results = nominatimClient.search(query)
                _searchResults.value = results
                if (results.isEmpty()) {
                    _inputError.value = "No results for \"$query\""
                }
            } catch (e: Exception) {
                _inputError.value = "Search failed: ${e.message ?: "network error"}"
                _searchResults.value = emptyList()
            } finally {
                _searchLoading.value = false
            }
        }
    }

    fun clearSearchResults() {
        _searchResults.value = emptyList()
    }

    /**
     * Apply a geocoded result and update both the fields + history entry logic.
     */
    fun applySearchResult(result: NominatimResult) {
        setCoordinates(result.latitude, result.longitude)
        // Also push to recent history like a normal start would (user can still tap Start)
        historyStore.push(result.latitude, result.longitude)
        _locationHistory.value = historyStore.load()
        clearSearchResults()
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
