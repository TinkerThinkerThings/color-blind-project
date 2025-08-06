package com.colorblind.spectra.data.lokal.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.colorblind.spectra.data.lokal.entity.EntityBiodata

@Database(entities = [EntityBiodata::class], version = 2) // versi naik
abstract class AppDatabase : RoomDatabase() {

    abstract fun biodataDao(): BiodataDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "biodata_db"
                )
                    .addMigrations(MIGRATION_1_2) // Tambah migrasi
                    .build()
                INSTANCE = instance
                instance
            }
        }

        // Migrate versi 1 → 2 (tambah kolom isIshiharaDone default false/0)
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE biodata ADD COLUMN isIshiharaDone INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
