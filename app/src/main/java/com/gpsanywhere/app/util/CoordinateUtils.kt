package com.gpsanywhere.app.util

/**
 * Parses a "latitude, longitude" string.
 *
 * Expected format: two comma-separated decimal numbers where the
 * left value is latitude (-90..90) and the right is longitude (-180..180).
 *
 * @return Pair(lng, lat) matching the caller convention, or null if invalid.
 */
fun parseClipboardCoordinates(raw: String): Pair<Double, Double>? {
    val parts = raw.split(",")
    if (parts.size != 2) return null
    val lat = parts[0].trim().toDoubleOrNull() ?: return null
    val lng = parts[1].trim().toDoubleOrNull() ?: return null
    if (lat !in -90.0..90.0 || lng !in -180.0..180.0) return null
    return lng to lat  // Pair(lng, lat) as expected by caller
}
