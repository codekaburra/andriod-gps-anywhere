package com.gpsanywhere.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "saved_locations",
    indices = [Index(value = ["sourceId"], unique = true, name = "idx_saved_locations_source_id")]
)
data class SavedLocation(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** Non-null for preinstalled locations, null for user-created. */
    val sourceId: String? = null,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val category: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isPreinstalled: Boolean get() = sourceId != null
}
