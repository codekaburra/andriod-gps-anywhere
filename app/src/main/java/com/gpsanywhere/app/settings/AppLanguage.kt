package com.gpsanywhere.app.settings

import java.util.Locale

/** In-app display language. SYSTEM follows the device locale. */
enum class AppLanguage(val tag: String) {
    SYSTEM(""),
    ENGLISH("en"),
    TRADITIONAL_CHINESE("zh-TW");

    /**
     * Whether saved locations and routes should show their Chinese names.
     * SYSTEM defers to the device locale, matching what the UI strings do.
     */
    val prefersChinese: Boolean
        get() = when (this) {
            TRADITIONAL_CHINESE -> true
            ENGLISH -> false
            SYSTEM -> Locale.getDefault().language == "zh"
        }
}
