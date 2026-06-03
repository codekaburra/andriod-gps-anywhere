package com.gpsanywhere.app.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.gpsanywhere.app.service.SpoofService
import org.osmdroid.util.GeoPoint

/**
 * Process-wide current position: device GPS when idle, spoof coords when active
 * (and last spoof coords when the service still holds them).
 */
object CurrentLocationProvider {

    private val _deviceLat = MutableLiveData<Double?>(null)
    private val _deviceLng = MutableLiveData<Double?>(null)
    private val _latitude = MediatorLiveData<Double?>()
    private val _longitude = MediatorLiveData<Double?>()

    val latitude: LiveData<Double?> = _latitude
    val longitude: LiveData<Double?> = _longitude

    private var started = false
    private var locationListener: LocationListener? = null
    private var locationManager: LocationManager? = null

    fun ensureStarted(context: Context) {
        val appContext = context.applicationContext
        if (!started) {
            started = true
            wireMediator()
        }
        startDeviceLocationUpdates(appContext)
    }

    fun geoPointOrNull(): GeoPoint? {
        val lat = _latitude.value ?: return null
        val lng = _longitude.value ?: return null
        return GeoPoint(lat, lng)
    }

    fun formatLatitude(lat: Double?): String? =
        lat?.toBigDecimal()?.stripTrailingZeros()?.toPlainString()

    fun formatLongitude(lng: Double?): String? =
        lng?.toBigDecimal()?.stripTrailingZeros()?.toPlainString()

    private fun wireMediator() {
        val recompute = {
            _latitude.value = resolveCoordinate(isLat = true)
            _longitude.value = resolveCoordinate(isLat = false)
        }
        _latitude.addSource(SpoofService.currentLat) { recompute() }
        _latitude.addSource(SpoofService.currentLng) { recompute() }
        _latitude.addSource(SpoofService.isRunning) { recompute() }
        _latitude.addSource(SpoofService.isPaused) { recompute() }
        _latitude.addSource(_deviceLat) { recompute() }
        _latitude.addSource(_deviceLng) { recompute() }
    }

    private fun resolveCoordinate(isLat: Boolean): Double? {
        val running = SpoofService.isRunning.value == true
        val paused = SpoofService.isPaused.value == true
        val spoofLat = SpoofService.currentLat.value ?: 0.0
        val spoofLng = SpoofService.currentLng.value ?: 0.0
        val hasSpoof = spoofLat != 0.0 || spoofLng != 0.0
        val spoofCoord = if (isLat) spoofLat else spoofLng
        val deviceCoord = if (isLat) _deviceLat.value else _deviceLng.value
        return when {
            running && !paused && hasSpoof -> spoofCoord
            deviceCoord != null -> deviceCoord
            hasSpoof -> spoofCoord
            else -> null
        }
    }

    private fun startDeviceLocationUpdates(context: Context) {
        if (locationListener != null) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationManager = lm
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        for (provider in providers) {
            if (!lm.isProviderEnabled(provider)) continue
            try {
                lm.getLastKnownLocation(provider)?.let { postDeviceLocation(it) }
            } catch (_: SecurityException) {
            }
        }
        val listener = LocationListener { postDeviceLocation(it) }
        locationListener = listener
        try {
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                lm.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    2_000L,
                    5f,
                    listener,
                    android.os.Looper.getMainLooper()
                )
            }
            if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                lm.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    2_000L,
                    5f,
                    listener,
                    android.os.Looper.getMainLooper()
                )
            }
        } catch (_: SecurityException) {
        }
    }

    private fun postDeviceLocation(location: Location) {
        _deviceLat.postValue(location.latitude)
        _deviceLng.postValue(location.longitude)
    }
}
