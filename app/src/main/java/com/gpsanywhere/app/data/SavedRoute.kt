package com.gpsanywhere.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_routes")
data class SavedRoute(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val waypointsJson: String,
    val speedKmh: Double = 4.0,
    val routeMethod: String,
    val distanceMeters: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
