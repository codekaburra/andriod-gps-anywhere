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

class SpoofService : Service() {

    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "spoof_channel"

        const val ACTION_START_FIXED = "com.gpsanywhere.app.START_FIXED"
        const val ACTION_STOP = "com.gpsanywhere.app.STOP"
        const val ACTION_PAUSE = "com.gpsanywhere.app.PAUSE"
        const val ACTION_RESUME = "com.gpsanywhere.app.RESUME"

        const val EXTRA_LATITUDE = "extra_latitude"
        const val EXTRA_LONGITUDE = "extra_longitude"

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

        fun stop(context: Context) {
            val intent = Intent(context, SpoofService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private var locationManager: LocationManager? = null
    private val testProviderName = LocationManager.GPS_PROVIDER
    private var lastLat = 0.0
    private var lastLng = 0.0

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
                lastLat = lat
                lastLng = lng
                startForeground(NOTIFICATION_ID, buildNotification("Spoofing: $lat, $lng"))
                setupTestProvider()
                setMockLocation(lat, lng)
                _isRunning.postValue(true)
                _isPaused.postValue(false)
                _currentLat.postValue(lat)
                _currentLng.postValue(lng)
            }
            ACTION_PAUSE -> {
                if (_isRunning.value == true && _isPaused.value == false) {
                    cleanupTestProvider()
                    _isPaused.postValue(true)
                    val nm = getSystemService(NotificationManager::class.java)
                    nm.notify(NOTIFICATION_ID, buildNotification("Paused — real GPS active"))
                }
            }
            ACTION_RESUME -> {
                if (_isRunning.value == true && _isPaused.value == true) {
                    setupTestProvider()
                    setMockLocation(lastLat, lastLng)
                    _isPaused.postValue(false)
                    val nm = getSystemService(NotificationManager::class.java)
                    nm.notify(NOTIFICATION_ID, buildNotification("Spoofing: $lastLat, $lastLng"))
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
        cleanupTestProvider()
        _isRunning.postValue(false)
        _isPaused.postValue(false)
        super.onDestroy()
    }

    private fun setupTestProvider() {
        try {
            locationManager?.removeTestProvider(testProviderName)
        } catch (_: Exception) {}

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                locationManager?.addTestProvider(
                    testProviderName,
                    false, false, false, false, false,
                    true, true,
                    ProviderProperties.POWER_USAGE_LOW,
                    ProviderProperties.ACCURACY_FINE
                )
            } else {
                @Suppress("DEPRECATION")
                locationManager?.addTestProvider(
                    testProviderName,
                    false, false, false, false, false,
                    true, true, 1, 1
                )
            }
            locationManager?.setTestProviderEnabled(testProviderName, true)
        } catch (e: SecurityException) {
            stopSpoofing()
        }
    }

    private fun setMockLocation(lat: Double, lng: Double) {
        try {
            val location = Location(testProviderName).apply {
                latitude = lat
                longitude = lng
                accuracy = 3.0f
                altitude = 0.0
                time = System.currentTimeMillis()
                elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
            }
            locationManager?.setTestProviderLocation(testProviderName, location)
        } catch (_: Exception) {}
    }

    private fun cleanupTestProvider() {
        try {
            locationManager?.setTestProviderEnabled(testProviderName, false)
            locationManager?.removeTestProvider(testProviderName)
        } catch (_: Exception) {}
    }

    private fun stopSpoofing() {
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
                "GPS Spoofing",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when GPS spoofing is active"
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
