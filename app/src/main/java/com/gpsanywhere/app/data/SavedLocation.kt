package com.gpsanywhere.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.gpsanywhere.app.settings.AppLanguage

@Entity(
    tableName = "saved_locations",
    indices = [Index(value = ["sourceId"], unique = true, name = "idx_saved_locations_source_id")]
)
data class SavedLocation(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** Non-null for preinstalled locations, null for user-created. */
    val sourceId: String? = null,
    /** Chinese (or only) display name. */
    val name: String,
    /** English display name for prebuilt locations; blank for user-created ones. */
    @ColumnInfo(defaultValue = "")
    val nameEn: String = "",
    val latitude: Double,
    val longitude: Double,
    val category: String? = null,
    /** Pipe-separated Chinese (or only) tags, e.g. "zoo|animals|family". Empty means none. */
    val tags: String = "",
    /** Pipe-separated English tags for prebuilt locations; blank for user-created ones. */
    @ColumnInfo(defaultValue = "")
    val tagsEn: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isPreinstalled: Boolean get() = sourceId != null

    /** The English name when [language] calls for it and one exists; otherwise [name]. */
    fun displayName(language: AppLanguage): String =
        if (!language.prefersChinese && nameEn.isNotBlank()) nameEn else name

    /** Returns the tags as a list, or empty list if no tags. */
    val tagList: List<String>
        get() = tags.toTagList()

    /**
     * Tags in the language [language] asks for, falling back to [tagList] when no
     * English set exists — user-created locations never have one, and locations
     * saved before English tags shipped may not either.
     */
    fun displayTags(language: AppLanguage): List<String> =
        if (!language.prefersChinese && tagsEn.isNotBlank()) tagsEn.toTagList() else tagList
}

private fun String.toTagList(): List<String> =
    if (isBlank()) emptyList() else split("|").map { it.trim() }.filter { it.isNotEmpty() }
