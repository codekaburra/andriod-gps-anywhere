package com.gpsanywhere.app.data

import android.content.Context
import com.gpsanywhere.app.routes.LocationPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

object DefaultSavedRouteSeeder {
    private const val PREFS_NAME = "gpsanywhere_default_saved_routes"
    private const val KEY_SEEDED = "seeded_v10" // bumped: CSV format changed to Latitude,Longitude,Name_TC,Name_EN
    private val mutex = Mutex()
    private const val DEFAULT_ROUTE_METHOD = "MANUAL_MAP"
    const val ASSET_FOLDER = "saved_routes"

    data class DefaultRouteAsset(
        val routeId: String? = null,
        val routeName: String,
        val version: Int = 1,
        val coordinates: List<DefaultLocationAsset>
    ) {
        fun toLocationPoints() = coordinates.map {
            LocationPoint(latitude = it.latitude, longitude = it.longitude, name = it.name)
        }
    }

    data class DefaultLocationAsset(
        val name: String,
        val latitude: Double,
        val longitude: Double
    )

    /** Parse a single CSV route file. Returns null if the file is malformed. */
    private fun parseCsv(content: String): DefaultRouteAsset? {
        var routeName: String? = null
        var version = 1
        val coordinates = mutableListOf<DefaultLocationAsset>()
        var headerSkipped = false

        for (rawLine in content.lineSequence()) {
            val line = rawLine.trim()
            when {
                line.startsWith("# route_name:") ->
                    routeName = line.removePrefix("# route_name:").trim()
                line.startsWith("# version:") ->
                    version = line.removePrefix("# version:").trim().toIntOrNull() ?: 1
                line.startsWith("#") || line.isEmpty() -> Unit // skip other comments / blank
                !headerSkipped -> headerSkipped = true // skip "name,latitude,longitude" header
                else -> {
                    val parts = parseCsvLine(line)
                    if (parts.size >= 4) {
                        val lat = parts[0].toDoubleOrNull() ?: continue
                        val lng = parts[1].toDoubleOrNull() ?: continue
                        val name = parts[2].ifBlank { parts[3] }
                        coordinates.add(DefaultLocationAsset(name = name, latitude = lat, longitude = lng))
                    } else if (parts.size >= 3) {
                        val lat = parts[0].toDoubleOrNull() ?: continue
                        val lng = parts[1].toDoubleOrNull() ?: continue
                        coordinates.add(DefaultLocationAsset(name = parts[2], latitude = lat, longitude = lng))
                    }
                }
            }
        }

        if (routeName == null || coordinates.isEmpty()) return null
        return DefaultRouteAsset(routeName = routeName, version = version, coordinates = coordinates)
    }

    /**
     * Split a CSV line into fields, respecting double-quoted fields that may contain commas.
     * e.g. "\"My, Place\",22.5,114.1" → ["My, Place", "22.5", "114.1"]
     */
    private fun parseCsvLine(line: String): List<String> {
        val fields = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' && !inQuotes -> inQuotes = true
                ch == '"' && inQuotes -> {
                    if (i + 1 < line.length && line[i + 1] == '"') { sb.append('"'); i++ } // escaped ""
                    else inQuotes = false
                }
                ch == ',' && !inQuotes -> { fields.add(sb.toString()); sb.clear() }
                else -> sb.append(ch)
            }
            i++
        }
        fields.add(sb.toString())
        return fields
    }

    /** Load all bundled CSV routes from assets/saved_routes/. */
    fun loadAllAssets(context: Context): List<DefaultRouteAsset> {
        val appContext = context.applicationContext
        return appContext.assets.list(ASSET_FOLDER)
            ?.filter { it.endsWith(".csv") }
            ?.sortedBy { it }
            ?.mapNotNull { filename ->
                runCatching {
                    val content = appContext.assets.open("$ASSET_FOLDER/$filename")
                        .bufferedReader().use { it.readText() }
                    parseCsv(content)
                }.getOrNull()
            } ?: emptyList()
    }

    suspend fun seedIfNeeded(context: Context, routeDao: RouteDao) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val appContext = context.applicationContext
            val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            if (prefs.getBoolean(KEY_SEEDED, false)) return@withLock

            val assets = loadAllAssets(appContext)

            // Remove duplicate rows caused by previous seeder key bumps
            assets.forEach { route ->
                val rows = routeDao.getAllByName(route.routeName)
                if (rows.size > 1) {
                    val keep = rows.first()
                    val deleteIds = rows.drop(1).map { it.id }
                    routeDao.deleteByIds(deleteIds)
                    if (keep.routeId == null && route.routeId != null) {
                        routeDao.update(keep.copy(routeId = route.routeId))
                    }
                }
            }

            // Seed any preset not yet in DB
            assets.forEach { route ->
                val points = route.toLocationPoints()
                if (points.isEmpty()) return@forEach
                val alreadyExists = if (route.routeId != null) {
                    routeDao.countByRouteId(route.routeId) > 0 || routeDao.countByName(route.routeName) > 0
                } else {
                    routeDao.countByName(route.routeName) > 0
                }
                if (!alreadyExists) {
                    routeDao.insert(
                        SavedRoute(
                            name = route.routeName,
                            waypointsJson = WaypointJson.toJson(points),
                            routeMethod = DEFAULT_ROUTE_METHOD,
                            distanceMeters = estimateDistance(points),
                            routeId = route.routeId
                        )
                    )
                }
            }

            prefs.edit().putBoolean(KEY_SEEDED, true).apply()
        }
    }

    private fun estimateDistance(points: List<LocationPoint>): Double {
        if (points.size < 2) return 0.0
        var total = 0.0
        for (i in 0 until points.size - 1) {
            val a = points[i]; val b = points[i + 1]
            val results = FloatArray(1)
            android.location.Location.distanceBetween(
                a.latitude, a.longitude, b.latitude, b.longitude, results
            )
            total += results[0]
        }
        return total.toDouble()
    }
}
