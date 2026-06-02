package com.gpsanywhere.app.settings

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    fun next(): ThemeMode = when (this) {
        SYSTEM -> LIGHT
        LIGHT -> DARK
        DARK -> SYSTEM
    }
}
