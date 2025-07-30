package com.colorblind.spectra.data.lokal.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.colorblind.spectra.data.lokal.entity.EntityBiodata

@Dao
interface BiodataDao {
    @Insert
    suspend fun insert(biodata: EntityBiodata)

    @Query("SELECT * FROM biodata")
    suspend fun getAll(): List<EntityBiodata>
}
