package com.gpsanywhere.app.directions

import com.gpsanywhere.app.routes.LocationPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Simple forward geocoder using the free Nominatim (OpenStreetMap) public API.
 * No API key required. Must respect rate limits (max ~1 request/sec) and provide a good User-Agent.
 * https://nominatim.org/release-docs/latest/api/Search/
 */
class NominatimClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * Search for a place name / address and return possible matches with coordinates.
     */
    suspend fun search(
        query: String,
        limit: Int = 6
    ): List<NominatimResult> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val url = "https://nominatim.openstreetmap.org/search" +
            "?q=$encoded" +
            "&format=json" +
            "&limit=$limit" +
            "&addressdetails=1" +
            "&extratags=0"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "GPSAnywhere/1.0 (com.gpsanywhere.app)")
            .get()
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IllegalStateException("Nominatim search failed: HTTP ${response.code}")
        }

        val body = response.body?.string() ?: return@withContext emptyList()
        parseResults(body)
    }

    private fun parseResults(json: String): List<NominatimResult> {
        val array = JSONArray(json)
        val results = mutableListOf<NominatimResult>()

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val lat = obj.optString("lat").toDoubleOrNull() ?: continue
            val lon = obj.optString("lon").toDoubleOrNull() ?: continue
            val name = obj.optString("display_name", obj.optString("name", "Unknown location"))

            results += NominatimResult(
                latitude = lat,
                longitude = lon,
                displayName = name,
            )
        }
        return results
    }
}

/**
 * Lightweight result from Nominatim.
 */
data class NominatimResult(
    val latitude: Double,
    val longitude: Double,
    val displayName: String
) {
    val locationPoint: LocationPoint get() = LocationPoint(latitude, longitude)
}
