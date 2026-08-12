package com.gpsanywhere.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [SavedRoute::class, SavedLocation::class],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun routeDao(): RouteDao
    abstract fun savedLocationDao(): SavedLocationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `saved_locations` ADD COLUMN `tagsEn` TEXT NOT NULL DEFAULT ''")
                // Backfill the bundled packs' tags so existing installs get English
                // tags on update, without having to re-import from Settings. Only
                // prebuilt rows are touched; user-written tags are left alone.
                db.execSQL(
                    """
                    UPDATE `saved_locations` SET `tagsEn` = CASE `tags`
                        WHEN '香港' THEN 'Hong Kong'
                        WHEN '日本' THEN 'Japan'
                        WHEN '台灣' THEN 'Taiwan'
                        WHEN '西雅圖' THEN 'Seattle'
                        ELSE ''
                    END
                    WHERE `sourceId` IS NOT NULL
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `saved_locations` ADD COLUMN `nameEn` TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `saved_routes` ADD COLUMN `nameTc` TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `saved_locations` ADD COLUMN `tags` TEXT NOT NULL DEFAULT ''")
            }
        }

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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
