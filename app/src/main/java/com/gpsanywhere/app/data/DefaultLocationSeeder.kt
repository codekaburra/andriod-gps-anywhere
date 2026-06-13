package com.gpsanywhere.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DefaultLocationSeeder {
    private const val PREFS_NAME = "gpsanywhere_default_locations"
    private const val KEY_SEEDED = "seeded_v10" // bumped: removed source_id from CSV, auto-generate from name_en
    const val ASSET_FOLDER = "saved_locations"

    data class DefaultLocationPack(
        val packName: String,
        val version: Int,
        val locations: List<DefaultLocationAsset>
    )

    data class DefaultLocationAsset(
        val sourceId: String,
        val name: String,
        val nameEng: String = "",
        val latitude: Double,
        val longitude: Double,
        val tags: String = ""
    )

    /** Parse a single CSV location pack file. Returns null if malformed. */
    private fun parseCsv(content: String): DefaultLocationPack? {
        var packName: String? = null
        var version = 1
        val locations = mutableListOf<DefaultLocationAsset>()
        var headerSkipped = false

        for (rawLine in content.lineSequence()) {
            val line = rawLine.trim()
            when {
                line.startsWith("# pack_name:") ->
                    packName = line.removePrefix("# pack_name:").trim()
                line.startsWith("# version:") ->
                    version = line.removePrefix("# version:").trim().toIntOrNull() ?: 1
                line.startsWith("#") || line.isEmpty() -> Unit
                !headerSkipped -> headerSkipped = true // skip header row
                else -> {
                    // CSV format: latitude,longitude,name_tc,name_en,tags
                    val parts = parseCsvLine(line)
                    if (parts.size >= 4) {
                        val lat = parts[0].toDoubleOrNull() ?: continue
                        val lng = parts[1].toDoubleOrNull() ?: continue
                        val nameTc = parts[2]
                        val nameEn = parts[3]
                        val tags = parts.getOrElse(4) { "" }
                        val sourceId = nameEn.lowercase()
                            .replace(Regex("[^a-z0-9]+"), "-")
                            .trim('-')
                            .ifBlank { nameTc }
                        locations.add(DefaultLocationAsset(
                            sourceId = sourceId,
                            name = nameTc,
                            nameEng = nameEn,
                            latitude = lat,
                            longitude = lng,
                            tags = tags
                        ))
                    }
                }
            }
        }

        if (packName == null) return null
        return DefaultLocationPack(packName = packName, version = version, locations = locations)
    }

    /**
     * Split a CSV line into fields, respecting double-quoted fields that may contain commas.
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
                    if (i + 1 < line.length && line[i + 1] == '"') { sb.append('"'); i++ }
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

    /** Load all bundled CSV location packs from assets/saved_locations/. */
    fun loadAllPacks(context: Context): List<DefaultLocationPack> {
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
                            longitude = loc.longitude,
                            tags = loc.tags
                        )
                    )
                }
            }

            prefs.edit().putBoolean(KEY_SEEDED, true).apply()
        }
}
