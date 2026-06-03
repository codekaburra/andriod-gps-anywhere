package com.gpsanywhere.app.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DefaultLocationSeeder {
    private const val PREFS_NAME = "gpsanywhere_default_locations"
    private const val KEY_SEEDED = "seeded_v7" // bumped: regional location packs
    const val ASSET_FOLDER = "saved_locations"

    private val gson = Gson()

    data class DefaultLocationPack(
        @SerializedName("pack_name") val packName: String,
        val version: Int,
        val locations: List<DefaultLocationAsset>
    )

    data class DefaultLocationAsset(
        @SerializedName("source_id") val sourceId: String,
        val name: String,
        val latitude: Double,
        val longitude: Double
    )

    /** Load all bundled location packs from assets/saved_locations/, sorted by filename. */
    fun loadAllPacks(context: Context): List<DefaultLocationPack> {
        val appContext = context.applicationContext
        return appContext.assets.list(ASSET_FOLDER)
            ?.filter { it.endsWith(".json") }
            ?.sortedBy { it }
            ?.mapNotNull { filename ->
                runCatching {
                    appContext.assets.open("$ASSET_FOLDER/$filename").bufferedReader().use {
                        gson.fromJson(it, DefaultLocationPack::class.java)
                    }
                }.getOrNull()
            } ?: emptyList()
    }

    suspend fun seedIfNeeded(context: Context, dao: SavedLocationDao) =
        withContext(Dispatchers.IO) {
            val appContext = context.applicationContext
            val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            if (prefs.getBoolean(KEY_SEEDED, false)) return@withContext

            val assets = loadAllPacks(appContext).flatMap { it.locations }
            val validSourceIds = assets.map { it.sourceId }
            if (validSourceIds.isEmpty()) {
                dao.deleteAllPreinstalled()
            } else {
                dao.deletePreinstalledNotIn(validSourceIds)
            }

            assets.forEach { loc ->
                if (dao.countBySourceId(loc.sourceId) == 0) {
                    dao.insert(
                        SavedLocation(
                            sourceId = loc.sourceId,
                            name = loc.name,
                            latitude = loc.latitude,
                            longitude = loc.longitude
                        )
                    )
                }
            }

            prefs.edit().putBoolean(KEY_SEEDED, true).apply()
        }
}
