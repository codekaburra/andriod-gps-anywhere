package com.gpsanywhere.app.settings

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

class AppPreferences(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var themeMode: ThemeMode
        get() {
            val stored = prefs.getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            return when (stored) {
                AppCompatDelegate.MODE_NIGHT_NO -> ThemeMode.LIGHT
                AppCompatDelegate.MODE_NIGHT_YES -> ThemeMode.DARK
                else -> ThemeMode.SYSTEM
            }
        }
        set(value) {
            val nightMode = when (value) {
                ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
                ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            prefs.edit().putInt(KEY_THEME_MODE, nightMode).apply()
            AppCompatDelegate.setDefaultNightMode(nightMode)
        }

    var onboardingShown: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_SHOWN, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_SHOWN, value).apply()

    var complianceAcknowledged: Boolean
        get() = prefs.getBoolean(KEY_COMPLIANCE_ACK, false)
        set(value) = prefs.edit().putBoolean(KEY_COMPLIANCE_ACK, value).apply()

    fun applySavedTheme() {
        val nightMode = prefs.getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    companion object {
        private const val PREFS_NAME = "gpsanywhere_prefs"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_ONBOARDING_SHOWN = "onboarding_shown"
        private const val KEY_COMPLIANCE_ACK = "compliance_ack"
    }
}
