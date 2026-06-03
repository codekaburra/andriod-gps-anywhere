package com.gpsanywhere.app.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.location.provider.ProviderProperties
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.gpsanywhere.app.MainActivity
import com.gpsanywhere.app.R
import kotlinx.coroutines.*
import kotlin.random.Random

class SpoofService : Service() {

    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "spoof_channel"

        const val ACTION_START_FIXED = "com.gpsanywhere.app.START_FIXED"
        const val ACTION_START_WALK = "com.gpsanywhere.app.START_WALK"
        const val ACTION_STOP = "com.gpsanywhere.app.STOP"
        const val ACTION_PAUSE = "com.gpsanywhere.app.PAUSE"
        const val ACTION_RESUME = "com.gpsanywhere.app.RESUME"

        const val EXTRA_LATITUDE = "extra_latitude"
        const val EXTRA_LONGITUDE = "extra_longitude"
        const val EXTRA_LATS = "extra_lats"
        const val EXTRA_LNGS = "extra_lngs"
        const val EXTRA_SPEED_KMH = "extra_speed_kmh"
        const val EXTRA_LOOP = "extra_loop"

        private val _isRunning = MutableLiveData(false)
        val isRunning: LiveData<Boolean> = _isRunning

        private val _isPaused = MutableLiveData(false)
        val isPaused: LiveData<Boolean> = _isPaused

        private val _currentLat = MutableLiveData(0.0)
        val currentLat: LiveData<Double> = _currentLat

        private val _currentLng = MutableLiveData(0.0)
        val currentLng: LiveData<Double> = _currentLng

        fun startFixed(context: Context, lat: Double, lng: Double) {
            val intent = Intent(context, SpoofService::class.java).apply {
                action = ACTION_START_FIXED
                putExtra(EXTRA_LATITUDE, lat)
                putExtra(EXTRA_LONGITUDE, lng)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun pause(context: Context) {
            context.startService(Intent(context, SpoofService::class.java).apply {
                action = ACTION_PAUSE
            })
        }

        fun resume(context: Context) {
            context.startService(Intent(context, SpoofService::class.java).apply {
                action = ACTION_RESUME
            })
        }

        fun startWalk(
            context: Context,
            lats: DoubleArray,
            lngs: DoubleArray,
            speedKmh: Float,
            loop: Boolean = false
        ) {
            val intent = Intent(context, SpoofService::class.java).apply {
                action = ACTION_START_WALK
                putExtra(EXTRA_LATS, lats)
                putExtra(EXTRA_LNGS, lngs)
                putExtra(EXTRA_SPEED_KMH, speedKmh)
                putExtra(EXTRA_LOOP, loop)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, SpoofService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private var locationManager: LocationManager? = null
    private val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
    private var lastLat = 0.0
    private var lastLng = 0.0
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var pushJob: Job? = null
    private var walkJob: Job? = null
    private var currentBearing: Float = 0f
    private var currentSpeedMps: Float = 0f

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_FIXED -> {
                val lat = intent.getDoubleExtra(EXTRA_LATITUDE, 0.0)
                val lng = intent.getDoubleExtra(EXTRA_LONGITUDE, 0.0)
                walkJob?.cancel()
                lastLat = lat
                lastLng = lng
                currentBearing = 0f
                currentSpeedMps = 0f
                startForeground(NOTIFICATION_ID, buildNotification("Custom Location: $lat, $lng"))
                setupTestProvider()
                startPushLoop()
                _isRunning.postValue(true)
                _isPaused.postValue(false)
                _currentLat.postValue(lat)
                _currentLng.postValue(lng)
            }
            ACTION_START_WALK -> {
                val lats = intent.getDoubleArrayExtra(EXTRA_LATS) ?: DoubleArray(0)
                val lngs = intent.getDoubleArrayExtra(EXTRA_LNGS) ?: DoubleArray(0)
                val speedKmh = intent.getFloatExtra(EXTRA_SPEED_KMH, 4f)
                val loop = intent.getBooleanExtra(EXTRA_LOOP, false)
                if (lats.size < 2 || lats.size != lngs.size) return START_NOT_STICKY
                lastLat = lats[0]
                lastLng = lngs[0]
                currentSpeedMps = speedKmh * 1000f / 3600f
                startForeground(NOTIFICATION_ID, buildNotification("Walking @ ${speedKmh} km/h"))
                setupTestProvider()
                startPushLoop()
                startWalkJob(lats, lngs, speedKmh, loop)
                _isRunning.postValue(true)
                _isPaused.postValue(false)
                _currentLat.postValue(lastLat)
                _currentLng.postValue(lastLng)
            }
            ACTION_PAUSE -> {
                if (_isRunning.value == true && _isPaused.value == false) {
                    pushJob?.cancel()
                    walkJob?.cancel()
                    cleanupTestProvider()
                    _isPaused.postValue(true)
                    val nm = getSystemService(NotificationManager::class.java)
                    nm.notify(NOTIFICATION_ID, buildNotification("Paused — using real location"))
                }
            }
            ACTION_RESUME -> {
                if (_isRunning.value == true && _isPaused.value == true) {
                    setupTestProvider()
                    startPushLoop()
                    _isPaused.postValue(false)
                    val nm = getSystemService(NotificationManager::class.java)
                    nm.notify(NOTIFICATION_ID, buildNotification("Custom Location: $lastLat, $lastLng"))
                }
            }
            ACTION_STOP -> {
                stopSpoofing()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        pushJob?.cancel()
        walkJob?.cancel()
        serviceScope.cancel()
        cleanupTestProvider()
        _isRunning.postValue(false)
        _isPaused.postValue(false)
        super.onDestroy()
    }

    private fun setupTestProvider() {
        for (provider in providers) {
            try {
                locationManager?.removeTestProvider(provider)
            } catch (_: Exception) {}

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    locationManager?.addTestProvider(
                        provider,
                        false, false, false, false, true,
                        true, true,
                        ProviderProperties.POWER_USAGE_LOW,
                        ProviderProperties.ACCURACY_FINE
                    )
                } else {
                    @Suppress("DEPRECATION")
                    locationManager?.addTestProvider(
                        provider,
                        false, false, false, false, true,
                        true, true, 1, 1
                    )
                }
                locationManager?.setTestProviderEnabled(provider, true)
            } catch (e: SecurityException) {
                stopSpoofing()
                return
            } catch (_: Exception) {}
        }
    }

    private fun startWalkJob(lats: DoubleArray, lngs: DoubleArray, speedKmh: Float, loop: Boolean) {
        walkJob?.cancel()
        val metersPerSec = (speedKmh * 1000.0 / 3600.0).coerceAtLeast(0.1)
        val tickMs = 500L
        val metersPerTick = metersPerSec * (tickMs / 1000.0)
        walkJob = serviceScope.launch {
            do {
                var segIdx = 0
                while (isActive && segIdx < lats.size - 1) {
                    val aLat = lats[segIdx]; val aLng = lngs[segIdx]
                    val bLat = lats[segIdx + 1]; val bLng = lngs[segIdx + 1]
                    val segLen = haversine(aLat, aLng, bLat, bLng)
                    if (segLen < 0.01) { segIdx++; continue }
                    currentBearing = bearing(aLat, aLng, bLat, bLng).toFloat()
                    currentSpeedMps = metersPerSec.toFloat()
                    var traveled = 0.0
                    while (isActive && traveled < segLen) {
                        val frac = (traveled / segLen).coerceIn(0.0, 1.0)
                        lastLat = aLat + (bLat - aLat) * frac
                        lastLng = aLng + (bLng - aLng) * frac
                        _currentLat.postValue(lastLat)
                        _currentLng.postValue(lastLng)
                        delay(tickMs)
                        traveled += metersPerTick
                    }
                    segIdx++
                }
                if (loop) {
                    lastLat = lats[0]; lastLng = lngs[0]
                }
            } while (isActive && loop)
            currentSpeedMps = 0f
        }
    }

    private fun haversine(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lng1, lat2, lng2, results)
        return results[0].toDouble()
    }

    private fun bearing(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLng = Math.toRadians(lng2 - lng1)
        val y = Math.sin(dLng) * Math.cos(Math.toRadians(lat2))
        val x = Math.cos(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2)) -
                Math.sin(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(dLng)
        return (Math.toDegrees(Math.atan2(y, x)) + 360) % 360
    }

    private fun startPushLoop() {
        pushJob?.cancel()
        pushJob = serviceScope.launch {
            while (isActive) {
                pushMockLocation(lastLat, lastLng)
                delay(1000)
            }
        }
    }

    private fun pushMockLocation(lat: Double, lng: Double) {
        for (provider in providers) {
            try {
                val location = Location(provider).apply {
                    latitude = lat
                    longitude = lng
                    accuracy = 1.0f
                    altitude = 0.0
                    bearing = currentBearing
                    speed = currentSpeedMps
                    time = System.currentTimeMillis()
                    elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        bearingAccuracyDegrees = 0.1f
                        speedAccuracyMetersPerSecond = 0.01f
                        verticalAccuracyMeters = 0.1f
                    }
                }
                locationManager?.setTestProviderLocation(provider, location)
            } catch (_: Exception) {}
        }
    }

    private fun cleanupTestProvider() {
        for (provider in providers) {
            try {
                locationManager?.setTestProviderEnabled(provider, false)
                locationManager?.removeTestProvider(provider)
            } catch (_: Exception) {}
        }
    }

    private fun stopSpoofing() {
        pushJob?.cancel()
        cleanupTestProvider()
        _isRunning.postValue(false)
        _isPaused.postValue(false)
        _currentLat.postValue(0.0)
        _currentLng.postValue(0.0)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Custom Location",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when using a custom location"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(contentText: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, SpoofService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GPS Anywhere — Active")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .addAction(0, "Stop", stopPendingIntent)
            .setOngoing(true)
            .build()
    }
}
