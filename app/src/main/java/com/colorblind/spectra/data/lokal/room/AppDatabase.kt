package com.colorblind.spectra.data.lokal.room
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.colorblind.spectra.data.lokal.entity.EntityBiodata

@Database(entities = [EntityBiodata::class], version = 1)
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
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
