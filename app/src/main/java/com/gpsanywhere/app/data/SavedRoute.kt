package com.gpsanywhere.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gpsanywhere.app.settings.AppLanguage

@Entity(tableName = "saved_routes")
data class SavedRoute(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** Non-null for routes seeded from bundled assets. */
    val routeId: String? = null,
    /** English (or only) display name. */
    val name: String,
    /** Traditional-Chinese display name for prebuilt routes; blank for user-created routes. */
    @ColumnInfo(defaultValue = "")
    val nameTc: String = "",
    val waypointsJson: String,
    val routeMethod: String,
    val distanceMeters: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isPreinstalled: Boolean get() = routeId != null

    /** The TC name when [language] calls for Chinese and one exists; otherwise [name]. */
    fun displayName(language: AppLanguage): String =
        if (language.prefersChinese && nameTc.isNotBlank()) nameTc else name
}
