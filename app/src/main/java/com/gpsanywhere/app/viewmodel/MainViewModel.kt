package com.gpsanywhere.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.gpsanywhere.app.settings.AppPreferences
import com.gpsanywhere.app.settings.ThemeMode

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = AppPreferences(application)

    private val _themeMode = MutableLiveData(prefs.themeMode)
    val themeMode: LiveData<ThemeMode> = _themeMode

    fun cycleTheme() {
        val next = (_themeMode.value ?: ThemeMode.SYSTEM).next()
        prefs.themeMode = next
        _themeMode.value = next
    }

    fun loadTheme() {
        _themeMode.value = prefs.themeMode
    }
}
