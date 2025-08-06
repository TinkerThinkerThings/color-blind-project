package com.colorblind.spectra.data.lokal.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.colorblind.spectra.data.lokal.entity.EntityBiodata

@Dao
interface BiodataDao {

    @Insert
    suspend fun insert(biodata: EntityBiodata)

    @Query("SELECT * FROM biodata ORDER BY id DESC LIMIT 1")
    suspend fun getLatest(): EntityBiodata?

    // Opsional kalau mau lihat semua data
    @Query("SELECT * FROM biodata")
    suspend fun getAll(): List<EntityBiodata>
    @Query("UPDATE biodata SET isIshiharaDone = 1 WHERE id = :id")
    suspend fun setIshiharaDone(id: Int)
    @Update
    suspend fun update(biodata: EntityBiodata)
}
