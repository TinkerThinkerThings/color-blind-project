package com.colorblind.spectra.data.lokal.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.colorblind.spectra.data.lokal.entity.EntityBiodata

@Database(
    entities = [EntityBiodata::class],
    version = 2, // Naikkan versinya
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun biodataDao(): BiodataDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        // Migration untuk tambah kolom baru
        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE biodata ADD COLUMN scoreNormal INTEGER")
                database.execSQL("ALTER TABLE biodata ADD COLUMN scoreDeuteranopia INTEGER")
                database.execSQL("ALTER TABLE biodata ADD COLUMN scoreProtanopia INTEGER")
            }
        }
    }
}
