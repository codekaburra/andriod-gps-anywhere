package com.gpsanywhere.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.gpsanywhere.app.data.AppDatabase
import com.gpsanywhere.app.data.DefaultLocationSeeder
import com.gpsanywhere.app.data.DefaultSavedRouteSeeder
import com.gpsanywhere.app.data.WaypointJson
import com.gpsanywhere.app.settings.AppLanguage
import com.gpsanywhere.app.settings.AppPreferences
import com.gpsanywhere.app.settings.ColorTheme
import com.gpsanywhere.app.settings.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = AppPreferences(application)

    private val _isImporting = MutableLiveData(false)
    val isImporting: LiveData<Boolean> = _isImporting

    /** Import all bundled prebuilt locations into the database. */
    fun importPrebuiltLocations(onDone: () -> Unit = {}) {
        if (_isImporting.value == true) return
        _isImporting.value = true
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val db = AppDatabase.getInstance(getApplication())
                DefaultLocationSeeder.importAll(getApplication(), db.savedLocationDao())
            }
            _isImporting.value = false
            onDone()
        }
    }

    /** Import all bundled prebuilt routes into the database. */
    fun importPrebuiltRoutes(onDone: () -> Unit = {}) {
        if (_isImporting.value == true) return
        _isImporting.value = true
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val db = AppDatabase.getInstance(getApplication())
                DefaultSavedRouteSeeder.importAll(getApplication(), db.routeDao())
            }
            _isImporting.value = false
            onDone()
        }
    }

    /** Remove all user-created locations and routes (keeps prebuilt items). */
    fun deleteCustom(onDone: () -> Unit = {}) {
        if (_isImporting.value == true) return
        _isImporting.value = true
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val db = AppDatabase.getInstance(getApplication())
                db.savedLocationDao().deleteAllCustom()

                val routeDao = db.routeDao()
                val assets = DefaultSavedRouteSeeder.loadAllAssets(getApplication())
                val prebuiltNames = assets.flatMap {
                    listOf(it.routeName.trim(), it.routeId?.trim().orEmpty())
                }.filter { it.isNotEmpty() }.toSet()
                fun sig(lat: Double, lng: Double, count: Int) = "%.5f,%.5f|%d".format(lat, lng, count)
                val prebuiltSigs = assets.mapNotNull { a ->
                    a.coordinates.firstOrNull()?.let { sig(it.latitude, it.longitude, a.coordinates.size) }
                }.toSet()
                routeDao.getAll().forEach { r ->
                    val pts = WaypointJson.fromJson(r.waypointsJson)
                    val rSig = pts.firstOrNull()?.let { sig(it.latitude, it.longitude, pts.size) }
                    val isPrebuilt = r.routeId != null || r.name.trim() in prebuiltNames || rSig in prebuiltSigs
                    if (!isPrebuilt) routeDao.delete(r)
                }
            }
            _isImporting.value = false
            onDone()
        }
    }

    /** Remove all bundled prebuilt locations (keeps user-created items and routes). */
    fun deletePrebuiltLocations(onDone: () -> Unit = {}) {
        if (_isImporting.value == true) return
        _isImporting.value = true
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                AppDatabase.getInstance(getApplication()).savedLocationDao().deleteAllPreinstalled()
            }
            _isImporting.value = false
            onDone()
        }
    }

    private val _themeMode = MutableLiveData(prefs.themeMode)
    val themeMode: LiveData<ThemeMode> = _themeMode

    private val _colorTheme = MutableLiveData(prefs.colorTheme)
    val colorTheme: LiveData<ColorTheme> = _colorTheme

    fun cycleTheme() {
        val next = (_themeMode.value ?: ThemeMode.SYSTEM).next()
        prefs.themeMode = next
        _themeMode.value = next
    }

    fun setTheme(mode: ThemeMode) {
        prefs.themeMode = mode
        _themeMode.value = mode
    }

    fun setColorTheme(theme: ColorTheme) {
        prefs.colorTheme = theme
        _colorTheme.value = theme
    }

    fun loadTheme() {
        _themeMode.value = prefs.themeMode
    }

    private val _appLanguage = MutableLiveData(prefs.appLanguage)
    val appLanguage: LiveData<AppLanguage> = _appLanguage

    fun setLanguage(language: AppLanguage) {
        prefs.appLanguage = language
        _appLanguage.value = language
    }
}
