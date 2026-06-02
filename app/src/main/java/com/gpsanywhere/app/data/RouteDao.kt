package com.gpsanywhere.app.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface RouteDao {

    @Insert
    suspend fun insert(route: SavedRoute): Long

    @Update
    suspend fun update(route: SavedRoute)

    @Delete
    suspend fun delete(route: SavedRoute)

    @Query("SELECT * FROM saved_routes WHERE id = :id")
    suspend fun getById(id: Long): SavedRoute?

    @Query("SELECT * FROM saved_routes ORDER BY updatedAt DESC")
    fun observeAll(): LiveData<List<SavedRoute>>

    @Query("SELECT * FROM saved_routes ORDER BY updatedAt DESC")
    suspend fun getAll(): List<SavedRoute>
}
