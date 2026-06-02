package com.gpsanywhere.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SavedRoute::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun routeDao(): RouteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gpsanywhere.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
