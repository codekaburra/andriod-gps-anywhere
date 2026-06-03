package com.gpsanywhere.app.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DefaultLocationSeeder {
    private const val PREFS_NAME = "gpsanywhere_default_locations"
    private const val KEY_SEEDED = "seeded_v1"
    private const val ASSET_FOLDER = "saved_locations"

    private val gson = Gson()

    suspend fun seedIfNeeded(context: Context, dao: SavedLocationDao) =
        withContext(Dispatchers.IO) {
            val prefs = context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            if (prefs.getBoolean(KEY_SEEDED, false)) return@withContext

            context.applicationContext.assets
                .list(ASSET_FOLDER)
                ?.filter { it.endsWith(".json") }
                ?.forEach { filename ->
                    runCatching {
                        val pack = context.assets
                            .open("$ASSET_FOLDER/$filename")
                            .bufferedReader()
                            .use { gson.fromJson(it, LocationPackAsset::class.java) }

                        pack.locations.forEach { loc ->
                            if (dao.countBySourceId(loc.sourceId) == 0) {
                                dao.insert(
                                    SavedLocation(
                                        sourceId = loc.sourceId,
                                        name = loc.name,
                                        latitude = loc.latitude,
                                        longitude = loc.longitude,
                                        category = loc.category
                                    )
                                )
                            }
                        }
                    }
                }

            prefs.edit().putBoolean(KEY_SEEDED, true).apply()
        }

    private data class LocationPackAsset(
        @SerializedName("pack_name") val packName: String,
        val version: Int,
        val locations: List<LocationAsset>
    )

    private data class LocationAsset(
        @SerializedName("source_id") val sourceId: String,
        val name: String,
        val latitude: Double,
        val longitude: Double,
        val category: String?
    )
}
