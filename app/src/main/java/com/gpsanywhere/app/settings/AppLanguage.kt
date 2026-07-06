package com.gpsanywhere.app.settings

/** In-app display language. SYSTEM follows the device locale. */
enum class AppLanguage(val tag: String) {
    SYSTEM(""),
    ENGLISH("en"),
    TRADITIONAL_CHINESE("zh-TW")
}
