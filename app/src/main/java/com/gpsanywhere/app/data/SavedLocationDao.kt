package com.gpsanywhere.app.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface SavedLocationDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(location: SavedLocation): Long

    @Delete
    suspend fun delete(location: SavedLocation)

    @Query("SELECT * FROM saved_locations ORDER BY name ASC")
    fun observeAll(): LiveData<List<SavedLocation>>

    @Query("SELECT COUNT(*) FROM saved_locations WHERE sourceId = :sourceId")
    suspend fun countBySourceId(sourceId: String): Int

    @Query("DELETE FROM saved_locations WHERE sourceId IS NULL")
    suspend fun deleteAllCustom()
}
