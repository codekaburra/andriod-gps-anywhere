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
        val spoofLat = SpoofService.currentLat.value
        val spoofLng = SpoofService.currentLng.value
        // Both present or neither: a position is a pair, and (0, 0) is a place.
        val hasSpoof = spoofLat != null && spoofLng != null
        val spoofCoord = if (isLat) spoofLat else spoofLng
        val deviceCoord = if (isLat) _deviceLat.value else _deviceLng.value
        return when {
            running && !paused && hasSpoof -> spoofCoord
            deviceCoord != null -> deviceCoord
            hasSpoof -> spoofCoord
            else -> null
        }
    }

    /**
     * Starts listening for the device's real position with whatever location
     * permission was actually granted.
     *
     * The permission prompt accepts coarse as well as fine, so requiring fine
     * here meant a coarse-only grant returned immediately and the real-position
     * features stayed dead with nothing to explain why. GPS needs fine; the
     * network provider works on coarse, so a coarse grant now gets network
     * updates rather than nothing.
     */
    private fun startDeviceLocationUpdates(context: Context) {
        if (locationListener != null) return

        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) return

        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationManager = lm

        val providers = buildList {
            if (fine) add(LocationManager.GPS_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
        }

        for (provider in providers) {
            if (!lm.isProviderEnabled(provider)) continue
            try {
                lm.getLastKnownLocation(provider)?.let { postDeviceLocation(it) }
            } catch (_: SecurityException) {
            }
        }

        val listener = LocationListener { postDeviceLocation(it) }
        locationListener = listener
        for (provider in providers) {
            if (!lm.isProviderEnabled(provider)) continue
            try {
                lm.requestLocationUpdates(
                    provider,
                    2_000L,
                    5f,
                    listener,
                    android.os.Looper.getMainLooper()
                )
            } catch (_: SecurityException) {
            }
        }
    }

    private fun postDeviceLocation(location: Location) {
        _deviceLat.postValue(location.latitude)
        _deviceLng.postValue(location.longitude)
    }
}
