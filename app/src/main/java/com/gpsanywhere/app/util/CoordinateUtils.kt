package com.gpsanywhere.app.util

/**
 * A parsed coordinate pair.
 *
 * Named fields rather than a Pair. The parser used to return `Pair(lng, lat)` —
 * longitude first, against the order it reads them in — and every call site had
 * to remember to take `.second` as the latitude. One of them did not, and the
 * route editor built its points reversed. Naming the fields removes the chance.
 */
data class Coordinates(val latitude: Double, val longitude: Double)

/**
 * Parses a "latitude, longitude" string.
 *
 * Expected format: two comma-separated decimal numbers where the left value is
 * latitude (-90..90) and the right is longitude (-180..180).
 *
 * @return the pair, or null if the text is not two in-range numbers.
 */
fun parseClipboardCoordinates(raw: String): Coordinates? {
    val parts = raw.split(",")
    if (parts.size != 2) return null
    val lat = parts[0].trim().toDoubleOrNull() ?: return null
    val lng = parts[1].trim().toDoubleOrNull() ?: return null
    if (lat !in -90.0..90.0 || lng !in -180.0..180.0) return null
    return Coordinates(latitude = lat, longitude = lng)
}
