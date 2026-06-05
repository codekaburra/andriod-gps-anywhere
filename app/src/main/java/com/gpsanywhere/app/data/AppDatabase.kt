package com.gpsanywhere.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [SavedRoute::class, SavedLocation::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun routeDao(): RouteDao
    abstract fun savedLocationDao(): SavedLocationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `saved_routes_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `routeId` TEXT,
                        `name` TEXT NOT NULL,
                        `waypointsJson` TEXT NOT NULL,
                        `routeMethod` TEXT NOT NULL,
                        `distanceMeters` REAL NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `saved_routes_new` (
                        `id`, `routeId`, `name`, `waypointsJson`, `routeMethod`,
                        `distanceMeters`, `createdAt`, `updatedAt`
                    )
                    SELECT
                        `id`, `routeId`, `name`, `waypointsJson`, `routeMethod`,
                        `distanceMeters`, `createdAt`, `updatedAt`
                    FROM `saved_routes`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `saved_routes`")
                db.execSQL("ALTER TABLE `saved_routes_new` RENAME TO `saved_routes`")
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add routeId column to existing saved_routes table
                runCatching {
                    db.execSQL("ALTER TABLE `saved_routes` ADD COLUMN `routeId` TEXT")
                }
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `saved_locations` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `sourceId` TEXT,
                        `name` TEXT NOT NULL,
                        `latitude` REAL NOT NULL,
                        `longitude` REAL NOT NULL,
                        `category` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `idx_saved_locations_source_id` ON `saved_locations` (`sourceId`)"
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gpsanywhere.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
