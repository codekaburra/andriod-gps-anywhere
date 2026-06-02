package com.gpsanywhere.app.directions

import com.gpsanywhere.app.routes.LocationPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class OsrmClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun fetchWalkingRoute(
        start: LocationPoint,
        end: LocationPoint
    ): OsrmRouteResult = withContext(Dispatchers.IO) {
        val url = "https://router.project-osrm.org/route/v1/foot/" +
            "${start.longitude},${start.latitude};" +
            "${end.longitude},${end.latitude}" +
            "?overview=full&geometries=geojson"

        val request = Request.Builder().url(url).get().build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IllegalStateException("OSRM request failed: ${response.code}")
        }

        val body = response.body?.string() ?: throw IllegalStateException("Empty OSRM response")
        parseResponse(body)
    }

    private fun parseResponse(json: String): OsrmRouteResult {
        val root = JSONObject(json)
        if (root.optString("code") != "Ok") {
            throw IllegalStateException("OSRM error: ${root.optString("message")}")
        }
        val routes = root.getJSONArray("routes")
        if (routes.length() == 0) throw IllegalStateException("No route found")
        val route = routes.getJSONObject(0)
        val distanceMeters = route.getDouble("distance")
        val durationSeconds = route.getDouble("duration")

        val geometry = route.getJSONObject("geometry")
        val coordinates = geometry.getJSONArray("coordinates")
        val waypoints = mutableListOf<LocationPoint>()
        for (i in 0 until coordinates.length()) {
            val coord = coordinates.getJSONArray(i)
            val lng = coord.getDouble(0)
            val lat = coord.getDouble(1)
            waypoints.add(LocationPoint(lat, lng))
        }

        return OsrmRouteResult(
            waypoints = waypoints,
            distanceMeters = distanceMeters,
            durationSeconds = durationSeconds
        )
    }
}

data class OsrmRouteResult(
    val waypoints: List<LocationPoint>,
    val distanceMeters: Double,
    val durationSeconds: Double
) {
    val distanceKm: Double get() = distanceMeters / 1000.0
    val durationMinutes: Int get() = (durationSeconds / 60.0).toInt().coerceAtLeast(1)
}
