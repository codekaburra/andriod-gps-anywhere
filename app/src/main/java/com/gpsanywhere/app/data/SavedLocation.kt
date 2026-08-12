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

    /**
     * The name for [language], falling back to the other one when it is blank.
     * Either field may be empty: the editor asks for both but requires only one.
     */
    fun displayName(language: AppLanguage): String =
        if (language.prefersChinese) name.ifBlank { nameEn } else nameEn.ifBlank { name }

    /** Returns the tags as a list, or empty list if no tags. */
    val tagList: List<String>
        get() = tags.toTagList()

    /** English tags as a list, or empty when none were provided. */
    val tagsEnList: List<String>
        get() = tagsEn.toTagList()

    /** Tags for [language], falling back to the other set when it is blank. */
    fun displayTags(language: AppLanguage): List<String> =
        if (language.prefersChinese) tagList.ifEmpty { tagsEnList }
        else tagsEnList.ifEmpty { tagList }
}

private fun String.toTagList(): List<String> =
    if (isBlank()) emptyList() else split("|").map { it.trim() }.filter { it.isNotEmpty() }
