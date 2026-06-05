package com.gpsanywhere.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.gpsanywhere.app.data.AppDatabase
import com.gpsanywhere.app.data.SavedRoute
import com.gpsanywhere.app.data.WaypointJson
import com.gpsanywhere.app.directions.NominatimClient
import com.gpsanywhere.app.directions.NominatimResult
import com.gpsanywhere.app.directions.OsrmClient
import com.gpsanywhere.app.directions.OsrmRouteResult
import com.gpsanywhere.app.location.CurrentLocationProvider
import com.gpsanywhere.app.routes.LocationPoint
import com.gpsanywhere.app.service.SpoofService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class RouteTab {
    MANUAL,
    OSRM
}

class RouteViewModel(application: Application) : AndroidViewModel(application) {

    private val osrmClient = OsrmClient()
    private val nominatimClient = NominatimClient()
    private val routeDao = AppDatabase.getInstance(application).routeDao()

    val isSpoofing: LiveData<Boolean> = SpoofService.isRunning

    private val _selectedTab = MutableStateFlow(RouteTab.MANUAL)
    val selectedTab: StateFlow<RouteTab> = _selectedTab.asStateFlow()

    private val _waypoints = MutableStateFlow<List<LocationPoint>>(emptyList())
    val waypoints: StateFlow<List<LocationPoint>> = _waypoints.asStateFlow()

    private val _speedKmh = MutableStateFlow(4f)
    val speedKmh: StateFlow<Float> = _speedKmh.asStateFlow()

    private val _startLat = MutableStateFlow("")
    val startLat: StateFlow<String> = _startLat.asStateFlow()

    private val _startLng = MutableStateFlow("")
    val startLng: StateFlow<String> = _startLng.asStateFlow()

    private val _endLat = MutableStateFlow("")
    val endLat: StateFlow<String> = _endLat.asStateFlow()

    private val _endLng = MutableStateFlow("")
    val endLng: StateFlow<String> = _endLng.asStateFlow()

    val mapCenterLat = CurrentLocationProvider.latitude
    val mapCenterLng = CurrentLocationProvider.longitude

    init {
        CurrentLocationProvider.ensureStarted(getApplication())
    }

    private val _osrmResult = MutableStateFlow<OsrmRouteResult?>(null)
    val osrmResult: StateFlow<OsrmRouteResult?> = _osrmResult.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Geocoding (name → lat/lng) via Nominatim - for OSRM start/end search by name
    private val _searchResults = MutableStateFlow<List<NominatimResult>>(emptyList())
    val searchResults: StateFlow<List<NominatimResult>> = _searchResults.asStateFlow()

    private val _searchLoading = MutableStateFlow(false)
    val searchLoading: StateFlow<Boolean> = _searchLoading.asStateFlow()

    fun selectTab(tab: RouteTab) {
        _selectedTab.value = tab
        if (tab != RouteTab.OSRM) {
            clearSearchResults()
        }
    }

    fun setSpeed(speed: Float) {
        _speedKmh.value = speed
    }

    fun addWaypoint(point: LocationPoint) {
        _waypoints.value = _waypoints.value + point
    }

    fun removeLastWaypoint() {
        val list = _waypoints.value
        if (list.isNotEmpty()) {
            _waypoints.value = list.dropLast(1)
        }
    }

    fun clearWaypoints() {
        _waypoints.value = emptyList()
        _osrmResult.value = null
    }

    fun setStartLat(v: String) { _startLat.value = v }
    fun setStartLng(v: String) { _startLng.value = v }
    fun setEndLat(v: String) { _endLat.value = v }
    fun setEndLng(v: String) { _endLng.value = v }

    fun fetchOsrmRoute() {
        val start = parsePoint(_startLat.value, _startLng.value) ?: run {
            _errorMessage.value = "Invalid start coordinates"
            return
        }
        val end = parsePoint(_endLat.value, _endLng.value) ?: run {
            _errorMessage.value = "Invalid end coordinates"
            return
        }
        viewModelScope.launch {
            _loading.value = true
            _errorMessage.value = null
            try {
                val result = osrmClient.fetchWalkingRoute(start, end)
                _osrmResult.value = result
                _waypoints.value = result.waypoints
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to fetch route"
            } finally {
                _loading.value = false
            }
        }
    }

    fun startWalk(loop: Boolean = false): Boolean {
        val points = _waypoints.value
        if (points.size < 2) {
            _errorMessage.value = "Need at least 2 waypoints to walk"
            return false
        }
        val lats = points.map { it.latitude }.toDoubleArray()
        val lngs = points.map { it.longitude }.toDoubleArray()
        SpoofService.startWalk(getApplication(), lats, lngs, _speedKmh.value, loop = loop)
        return true
    }

    fun stopWalk() {
        SpoofService.stop(getApplication())
    }

    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Search online by place name / address using Nominatim.
     * Results are exposed via searchResults. Call applySearchResult to use one.
     */
    fun searchByName(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _searchLoading.value = true
            _errorMessage.value = null
            try {
                val results = nominatimClient.search(query)
                _searchResults.value = results
                if (results.isEmpty()) {
                    _errorMessage.value = "No results found for \"$query\""
                }
            } catch (e: Exception) {
                _errorMessage.value = "Search failed: ${e.message ?: "network error"}"
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
     * Apply a search result to either the Start or End coordinates (OSRM tab).
     * Also clears the search results list.
     */
    fun applySearchResult(result: NominatimResult, isStart: Boolean) {
        val latStr = result.latitude.toBigDecimal().stripTrailingZeros().toPlainString()
        val lngStr = result.longitude.toBigDecimal().stripTrailingZeros().toPlainString()

        if (isStart) {
            _startLat.value = latStr
            _startLng.value = lngStr
        } else {
            _endLat.value = latStr
            _endLng.value = lngStr
        }
        clearSearchResults()
    }

    suspend fun saveRoute(name: String, method: String): Boolean {
        val points = _waypoints.value
        if (points.isEmpty() || name.isBlank()) return false
        val distance = _osrmResult.value?.distanceMeters
            ?: estimateDistance(points)
        val route = SavedRoute(
            name = name.trim(),
            waypointsJson = WaypointJson.toJson(points),
            routeMethod = method,
            distanceMeters = distance
        )
        routeDao.insert(route)
        return true
    }

    fun loadRoute(route: SavedRoute) {
        _waypoints.value = WaypointJson.fromJson(route.waypointsJson)
        _selectedTab.value = if (route.routeMethod == "OSRM") RouteTab.OSRM else RouteTab.MANUAL
    }

    /** Accepts "lat, lng", "lat lng", or "lat;lng". */
    fun parsePastedLocation(raw: String): LocationPoint? {
        val match = Regex("""^\s*(-?\d+\.?\d*)\s*[,;\s]\s*(-?\d+\.?\d*)\s*$""").find(raw)
            ?: return null
        val lat = match.groupValues[1].toDoubleOrNull() ?: return null
        val lng = match.groupValues[2].toDoubleOrNull() ?: return null
        if (lat !in -90.0..90.0 || lng !in -180.0..180.0) return null
        return LocationPoint(lat, lng)
    }

    fun addPastedWaypoint(raw: String): Boolean {
        val p = parsePastedLocation(raw) ?: run {
            _errorMessage.value = "Couldn't parse location"
            return false
        }
        addWaypoint(p)
        return true
    }

    fun setStartFromPaste(raw: String): Boolean {
        val p = parsePastedLocation(raw) ?: run {
            _errorMessage.value = "Couldn't parse location"
            return false
        }
        _startLat.value = p.latitude.toString()
        _startLng.value = p.longitude.toString()
        return true
    }

    fun setEndFromPaste(raw: String): Boolean {
        val p = parsePastedLocation(raw) ?: run {
            _errorMessage.value = "Couldn't parse location"
            return false
        }
        _endLat.value = p.latitude.toString()
        _endLng.value = p.longitude.toString()
        return true
    }

    private fun parsePoint(latStr: String, lngStr: String): LocationPoint? {
        val lat = latStr.toDoubleOrNull() ?: return null
        val lng = lngStr.toDoubleOrNull() ?: return null
        if (lat !in -90.0..90.0 || lng !in -180.0..180.0) return null
        return LocationPoint(lat, lng)
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
