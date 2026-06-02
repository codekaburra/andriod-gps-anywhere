package com.gpsanywhere.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.gpsanywhere.app.service.SpoofService
import com.gpsanywhere.app.settings.AppPreferences
import com.gpsanywhere.app.settings.ThemeMode

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = AppPreferences(application)

    val isSpoofing: LiveData<Boolean> = SpoofService.isRunning
    val currentLat: LiveData<Double> = SpoofService.currentLat
    val currentLng: LiveData<Double> = SpoofService.currentLng

    val hasActiveLocation: LiveData<Boolean> = isSpoofing.map { it }

    private val _themeMode = MutableLiveData(prefs.themeMode)
    val themeMode: LiveData<ThemeMode> = _themeMode

    fun toggleSpoofing() {
        val context = getApplication<Application>()
        if (SpoofService.isRunning.value == true) {
            SpoofService.stop(context)
        }
    }

    fun cycleTheme() {
        val next = (_themeMode.value ?: ThemeMode.SYSTEM).next()
        prefs.themeMode = next
        _themeMode.value = next
    }

    fun loadTheme() {
        _themeMode.value = prefs.themeMode
    }
}
