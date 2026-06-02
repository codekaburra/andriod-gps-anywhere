package com.gpsanywhere.app.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.gpsanywhere.app.routes.LocationPoint

object WaypointJson {
    private val gson = Gson()
    private val listType = object : TypeToken<List<LocationPoint>>() {}.type

    fun toJson(waypoints: List<LocationPoint>): String = gson.toJson(waypoints)

    fun fromJson(json: String): List<LocationPoint> {
        if (json.isBlank()) return emptyList()
        return gson.fromJson(json, listType) ?: emptyList()
    }
}
