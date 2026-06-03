package com.gpsanywhere.app.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.gpsanywhere.app.routes.LocationPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

object DefaultSavedRouteSeeder {
    private const val PREFS_NAME = "gpsanywhere_default_saved_routes"
    private const val KEY_SEEDED = "seeded_v8" // bumped: mutex fix + dedup re-run
    private val mutex = Mutex()
    private const val DEFAULT_ROUTE_METHOD = "MANUAL_MAP"
    const val ASSET_FOLDER = "saved_routes"

    private val gson = Gson()

    data class DefaultRouteAsset(
        @SerializedName("route_id") val routeId: String? = null,
        @SerializedName("route_name") val routeName: String,
        val version: Int = 1,
        @SerializedName("speed_kmh") val speedKmh: Double = 4.0,
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

    /** Load all bundled JSON routes from assets/saved_routes/. */
    fun loadAllAssets(context: Context): List<DefaultRouteAsset> {
        val appContext = context.applicationContext
        return appContext.assets.list(ASSET_FOLDER)
            ?.filter { it.endsWith(".json") }
            ?.sortedBy { it }
            ?.mapNotNull { filename ->
                runCatching {
                    appContext.assets.open("$ASSET_FOLDER/$filename").bufferedReader().use {
                        gson.fromJson(it, DefaultRouteAsset::class.java)
                    }
                }.getOrNull()
            } ?: emptyList()
    }

    suspend fun seedIfNeeded(context: Context, routeDao: RouteDao) = withContext(Dispatchers.IO) {
        // Mutex ensures concurrent ViewModel inits (WalkViewModel + SavedRoutesViewModel)
        // don't both pass the prefs check and double-seed simultaneously.
        mutex.withLock {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_SEEDED, false)) return@withLock

        val assets = loadAllAssets(appContext)

        // Remove duplicate rows caused by previous seeder key bumps:
        // for each preset name, keep only one row (prefer the one that already has routeId set).
        assets.forEach { route ->
            val rows = routeDao.getAllByName(route.routeName)
            if (rows.size > 1) {
                val keep = rows.first() // query orders: routeId NOT NULL first
                val deleteIds = rows.drop(1).map { it.id }
                routeDao.deleteByIds(deleteIds)
                // Backfill routeId on the kept row if it is missing
                if (keep.routeId == null && route.routeId != null) {
                    routeDao.update(keep.copy(routeId = route.routeId))
                }
            }
        }

        // Seed any preset that is not yet in the DB (check routeId first, then name)
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
                        speedKmh = route.speedKmh,
                        routeMethod = DEFAULT_ROUTE_METHOD,
                        distanceMeters = estimateDistance(points),
                        routeId = route.routeId
                    )
                )
            }
        }

        prefs.edit().putBoolean(KEY_SEEDED, true).apply()
        } // end mutex.withLock
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
