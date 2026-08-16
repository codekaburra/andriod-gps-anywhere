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
        /** Traditional-Chinese name, kept so import can recognise rows seeded under the old TC display name. */
        val routeNameTc: String? = null,
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

    /**
     * Parse CSV route content. Returns null if no coordinates could be parsed.
     * The route name may be blank when the content has no `# route_name*` header
     * (callers that require a name should check `routeName.isNotBlank()`).
     */
    internal fun parseCsv(content: String): DefaultRouteAsset? {
        var routeName: String? = null
        var routeNameTc: String? = null
        var routeId: String? = null
        var version = 1
        val coordinates = mutableListOf<DefaultLocationAsset>()
        var headerSkipped = false

        for (rawLine in content.lineSequence()) {
            val line = rawLine.trim()
            when {
                line.startsWith("# route_name_eng:") ->
                    routeName = line.removePrefix("# route_name_eng:").trim()
                line.startsWith("# route_name_tc:") ->
                    routeNameTc = line.removePrefix("# route_name_tc:").trim()
                line.startsWith("# route_name:") ->
                    routeName = line.removePrefix("# route_name:").trim()
                line.startsWith("# route_id:") ->
                    routeId = line.removePrefix("# route_id:").trim().ifBlank { routeId }
                line.startsWith("# version:") ->
                    version = line.removePrefix("# version:").trim().toIntOrNull() ?: 1
                line.startsWith("#") -> {
                    // First bare "# <id>" comment (no colon) acts as the route id.
                    val body = line.removePrefix("#").trim()
                    if (routeId == null && body.isNotEmpty() && !body.contains(':')) routeId = body
                }
                line.isEmpty() -> Unit
                else -> {
                    val parts = parseCsvLine(line)
                    val lat = parts.getOrNull(0)?.toDoubleOrNull()
                    val lng = parts.getOrNull(1)?.toDoubleOrNull()
                    if (lat == null || lng == null) {
                        // Non-numeric row: treat the first one as the column header, ignore the rest.
                        if (!headerSkipped) headerSkipped = true
                        continue
                    }
                    val name = when {
                        parts.size >= 4 -> parts[2].ifBlank { parts[3] }
                        parts.size == 3 -> parts[2]
                        else -> ""
                    }
                    coordinates.add(DefaultLocationAsset(name = name, latitude = lat, longitude = lng))
                }
            }
        }

        if (coordinates.isEmpty()) return null
        return DefaultRouteAsset(
            routeId = routeId,
            routeName = routeName ?: routeNameTc ?: "",
            routeNameTc = routeNameTc,
            version = version,
            coordinates = coordinates
        )
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
                    parseCsv(content)?.takeIf { it.routeName.isNotBlank() }
                }.getOrNull()
            } ?: emptyList()
    }

    /** Waypoint signature: first point (5 dp) + point count. Identifies a route regardless of its display name. */
    private fun waypointSignature(points: List<LocationPoint>): String? =
        points.firstOrNull()?.let { "%.5f,%.5f|%d".format(it.latitude, it.longitude, points.size) }

    /**
     * Import every bundled route into the DB on demand (no auto-seed gate).
     *
     * A bundled route is matched against existing rows by routeId, by either of its
     * display names (English or Traditional Chinese — older versions seeded the TC name),
     * or by waypoint signature. Matching rows are kept (the best one gets the routeId
     * backfilled) and redundant seed copies are deleted, so re-importing heals
     * duplicates instead of creating more.
     */
    suspend fun importAll(context: Context, routeDao: RouteDao) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val appContext = context.applicationContext
            val existing = routeDao.getAll().toMutableList()

            loadAllAssets(appContext).forEach { route ->
                val points = route.toLocationPoints()
                if (points.isEmpty()) return@forEach
                val assetSig = waypointSignature(points)
                val assetNames = listOfNotNull(
                    route.routeName.trim().takeIf { it.isNotEmpty() },
                    route.routeNameTc?.trim()?.takeIf { it.isNotEmpty() }
                )

                val matches = existing.filter { row ->
                    (route.routeId != null && row.routeId == route.routeId) ||
                        row.name.trim() in assetNames ||
                        waypointSignature(WaypointJson.fromJson(row.waypointsJson)) == assetSig
                }

                if (matches.isEmpty()) {
                    val inserted = SavedRoute(
                        name = route.routeName,
                        nameTc = route.routeNameTc.orEmpty(),
                        waypointsJson = WaypointJson.toJson(points),
                        routeMethod = DEFAULT_ROUTE_METHOD,
                        distanceMeters = estimateDistance(points),
                        routeId = route.routeId
                    )
                    val newId = routeDao.insert(inserted)
                    existing.add(inserted.copy(id = newId))
                    return@forEach
                }

                // Keep the best match: prefer a row that already carries the routeId, then the oldest.
                val keep = matches.sortedWith(
                    compareByDescending<SavedRoute> { it.routeId != null }.thenBy { it.createdAt }
                ).first()
                val needsUpdate = (route.routeId != null && keep.routeId == null) ||
                    (route.routeNameTc != null && keep.nameTc.isBlank())
                if (needsUpdate) {
                    val updated = keep.copy(
                        routeId = route.routeId ?: keep.routeId,
                        nameTc = route.routeNameTc.orEmpty().ifBlank { keep.nameTc }
                    )
                    routeDao.update(updated)
                    existing[existing.indexOf(keep)] = updated
                }

                // Delete redundant seed copies: rows that are clearly this bundled route
                // (matching routeId or one of its known names). Rows matching only by
                // waypoint signature may be user-edited copies, so they are left alone.
                val redundant = matches.filter { row ->
                    row !== keep &&
                        ((route.routeId != null && row.routeId == route.routeId) || row.name.trim() in assetNames)
                }
                if (redundant.isNotEmpty()) {
                    routeDao.deleteByIds(redundant.map { it.id })
                    existing.removeAll(redundant)
                }
            }

            appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_SEEDED, true).apply()
        }
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
