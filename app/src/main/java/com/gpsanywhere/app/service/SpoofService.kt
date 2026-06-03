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
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.StepsRecord
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

class SpoofService : Service() {

    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "spoof_channel"

        const val ACTION_START_FIXED = "com.gpsanywhere.app.START_FIXED"
        const val ACTION_START_WALK = "com.gpsanywhere.app.START_WALK"
        const val ACTION_STOP = "com.gpsanywhere.app.STOP"
        const val ACTION_PAUSE = "com.gpsanywhere.app.PAUSE"
        const val ACTION_RESUME = "com.gpsanywhere.app.RESUME"
        const val ACTION_UPDATE_SPEED = "com.gpsanywhere.app.UPDATE_SPEED"

        const val EXTRA_LATITUDE = "extra_latitude"
        const val EXTRA_LONGITUDE = "extra_longitude"
        const val EXTRA_LATS = "extra_lats"
        const val EXTRA_LNGS = "extra_lngs"
        const val EXTRA_SPEED_KMH = "extra_speed_kmh"
        const val EXTRA_MIN_SPEED_KMH = "extra_min_speed_kmh"
        const val EXTRA_MAX_SPEED_KMH = "extra_max_speed_kmh"
        const val EXTRA_VARY_KMH = "extra_vary_kmh"
        const val EXTRA_LOOP = "extra_loop"

        private val _isRunning = MutableLiveData(false)
        val isRunning: LiveData<Boolean> = _isRunning

        private val _isPaused = MutableLiveData(false)
        val isPaused: LiveData<Boolean> = _isPaused

        private val _currentLat = MutableLiveData(0.0)
        val currentLat: LiveData<Double> = _currentLat

        private val _currentLng = MutableLiveData(0.0)
        val currentLng: LiveData<Double> = _currentLng

        private val _currentSpeedKmh = MutableLiveData(0f)
        val currentSpeedKmh: LiveData<Float> = _currentSpeedKmh

        /** True only while a walk route is actively running (not fixed-location spoofing). */
        private val _isWalkMode = MutableLiveData(false)
        val isWalkMode: LiveData<Boolean> = _isWalkMode

        private val _stepCount = MutableLiveData(0)
        val stepCount: LiveData<Int> = _stepCount

        fun incrementSteps(amount: Int) {
            val current = _stepCount.value ?: 0
            _stepCount.postValue(current + amount)
        }

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
            minSpeedKmh: Float = 0f,
            maxSpeedKmh: Float = 20f,
            varyKmh: Float = 0f,
            loop: Boolean = false
        ) {
            val intent = Intent(context, SpoofService::class.java).apply {
                action = ACTION_START_WALK
                putExtra(EXTRA_LATS, lats)
                putExtra(EXTRA_LNGS, lngs)
                putExtra(EXTRA_SPEED_KMH, speedKmh)
                putExtra(EXTRA_MIN_SPEED_KMH, minSpeedKmh)
                putExtra(EXTRA_MAX_SPEED_KMH, maxSpeedKmh)
                putExtra(EXTRA_VARY_KMH, varyKmh)
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

        fun updateSpeed(context: Context, speedKmh: Float) {
            context.startService(Intent(context, SpoofService::class.java).apply {
                action = ACTION_UPDATE_SPEED
                putExtra(EXTRA_SPEED_KMH, speedKmh)
            })
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
    private var baseSpeedMps: Float = 0f
    private var minSpeedMps: Float = 0f
    private var maxSpeedMps: Float = 20f * 1000f / 3600f
    private var varyMps: Float = 0f

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
                _isWalkMode.postValue(false)
                _currentLat.postValue(lat)
                _currentLng.postValue(lng)
            }
            ACTION_START_WALK -> {
                val lats = intent.getDoubleArrayExtra(EXTRA_LATS) ?: DoubleArray(0)
                val lngs = intent.getDoubleArrayExtra(EXTRA_LNGS) ?: DoubleArray(0)
                val speedKmh = intent.getFloatExtra(EXTRA_SPEED_KMH, 4f)
                val minKmh = intent.getFloatExtra(EXTRA_MIN_SPEED_KMH, 0f)
                val maxKmh = intent.getFloatExtra(EXTRA_MAX_SPEED_KMH, 20f)
                val varyKmh = intent.getFloatExtra(EXTRA_VARY_KMH, 0f)
                val loop = intent.getBooleanExtra(EXTRA_LOOP, false)
                if (lats.size < 2 || lats.size != lngs.size) return START_NOT_STICKY
                lastLat = lats[0]
                lastLng = lngs[0]
                baseSpeedMps = speedKmh * 1000f / 3600f
                minSpeedMps = minKmh * 1000f / 3600f
                maxSpeedMps = minOf(maxKmh, 20f) * 1000f / 3600f
                varyMps = varyKmh * 1000f / 3600f
                currentSpeedMps = baseSpeedMps
                _currentSpeedKmh.postValue(speedKmh)
                startForeground(NOTIFICATION_ID, buildNotification("Walking @ ${"%.1f".format(speedKmh)} km/h"))
                setupTestProvider()
                startPushLoop()
                startWalkJob(lats, lngs, loop)
                _isRunning.postValue(true)
                _isPaused.postValue(false)
                _isWalkMode.postValue(true)
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
            ACTION_UPDATE_SPEED -> {
                val speedKmh = intent.getFloatExtra(EXTRA_SPEED_KMH, 4f)
                baseSpeedMps = speedKmh * 1000f / 3600f
                currentSpeedMps = baseSpeedMps
                _currentSpeedKmh.postValue(speedKmh)
                val nm = getSystemService(NotificationManager::class.java)
                nm.notify(NOTIFICATION_ID, buildNotification("Walking @ ${"%.1f".format(speedKmh)} km/h"))
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

    private fun startWalkJob(lats: DoubleArray, lngs: DoubleArray, loop: Boolean) {
        walkJob?.cancel()
        val tickMs = 500L
        walkJob = serviceScope.launch {
            // Speed variation coroutine: every random 1–3 s, nudge speed by ±varyMps
            launch {
                while (isActive) {
                    delay(Random.nextLong(1000L, 3001L))
                    if (varyMps > 0f) {
                        val delta = (Random.nextFloat() * 2f - 1f) * varyMps
                        currentSpeedMps = (baseSpeedMps + delta).coerceIn(minSpeedMps, maxSpeedMps)
                        _currentSpeedKmh.postValue(currentSpeedMps * 3600f / 1000f)
                    }
                }
            }

            do {
                var segIdx = 0
                while (isActive && segIdx < lats.size - 1) {
                    val aLat = lats[segIdx]; val aLng = lngs[segIdx]
                    val bLat = lats[segIdx + 1]; val bLng = lngs[segIdx + 1]
                    val segLen = haversine(aLat, aLng, bLat, bLng)
                    if (segLen < 0.01) { segIdx++; continue }
                    currentBearing = bearing(aLat, aLng, bLat, bLng).toFloat()
                    var traveled = 0.0
                    while (isActive && traveled < segLen) {
                        val frac = (traveled / segLen).coerceIn(0.0, 1.0)
                        lastLat = aLat + (bLat - aLat) * frac
                        lastLng = aLng + (bLng - aLng) * frac
                        _currentLat.postValue(lastLat)
                        _currentLng.postValue(lastLng)
                        delay(tickMs)
                        val metersPerSec = currentSpeedMps.toDouble().coerceAtLeast(0.1)
                        val distThisTick = metersPerSec * (tickMs / 1000.0)
                        traveled += distThisTick
                        val stepsThis = (distThisTick / 0.78).toInt()
                        if (stepsThis > 0) {
                            SpoofService.incrementSteps(stepsThis)
                            // Write to HC for games (from walk simulation)
                            launch {
                                try {
                                    val client = HealthConnectClient.getOrCreate(this@SpoofService)
                                    val now = Instant.now()
                                    val start = now.minus(1, ChronoUnit.MINUTES)
                                    val record = StepsRecord(
                                        count = stepsThis.toLong(),
                                        startTime = start,
                                        endTime = now,
                                        startZoneOffset = ZoneOffset.systemDefault().rules.getOffset(start),
                                        endZoneOffset = ZoneOffset.systemDefault().rules.getOffset(now)
                                    )
                                    client.insertRecords(listOf(record))
                                } catch (_: Exception) {}
                            }
                        }
                    }
                    segIdx++
                }
                if (loop) {
                    lastLat = lats[0]; lastLng = lngs[0]
                }
            } while (isActive && loop)
            currentSpeedMps = 0f
            _currentSpeedKmh.postValue(0f)
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
        _isWalkMode.postValue(false)
        _currentLat.postValue(0.0)
        _currentLng.postValue(0.0)
        _currentSpeedKmh.postValue(0f)
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
