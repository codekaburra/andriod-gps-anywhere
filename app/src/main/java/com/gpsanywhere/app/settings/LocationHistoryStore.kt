package com.gpsanywhere.app.settings

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class HistoryEntry(
    val lat: Double,
    val lng: Double,
    val label: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

class LocationHistoryStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val listType = object : TypeToken<MutableList<HistoryEntry>>() {}.type

    fun load(): List<HistoryEntry> {
        val json = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            gson.fromJson<MutableList<HistoryEntry>>(json, listType) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun push(lat: Double, lng: Double, label: String? = null) {
        val entries = load().toMutableList()
        // Remove duplicate if the same coordinates already exist
        entries.removeAll { it.lat == lat && it.lng == lng }
        entries.add(0, HistoryEntry(lat = lat, lng = lng, label = label?.trim()?.ifBlank { null }))
        val trimmed = entries.take(MAX_ENTRIES)
        prefs.edit().putString(KEY_HISTORY, gson.toJson(trimmed)).apply()
    }

    fun remove(entry: HistoryEntry) {
        val entries = load().toMutableList()
        entries.removeAll { it.lat == entry.lat && it.lng == entry.lng && it.timestamp == entry.timestamp }
        prefs.edit().putString(KEY_HISTORY, gson.toJson(entries)).apply()
    }

    fun rename(entry: HistoryEntry, newLabel: String) {
        val entries = load().toMutableList()
        val idx = entries.indexOfFirst {
            it.lat == entry.lat && it.lng == entry.lng && it.timestamp == entry.timestamp
        }
        if (idx >= 0) {
            entries[idx] = entries[idx].copy(label = newLabel.trim().ifBlank { null })
            prefs.edit().putString(KEY_HISTORY, gson.toJson(entries)).apply()
        }
    }

    fun clear() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    companion object {
        private const val PREFS_NAME = "gpsanywhere_location_history"
        private const val KEY_HISTORY = "history"
        private const val MAX_ENTRIES = 10
    }
}
