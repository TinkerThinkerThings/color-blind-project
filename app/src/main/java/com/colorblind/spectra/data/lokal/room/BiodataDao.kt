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
    @Query("UPDATE biodata SET hasilTes = :hasil WHERE id = :id")
    suspend fun updateHasilTes(id: Int, hasil: String)
    @Query("UPDATE biodata SET scoreNormal = :normal, scoreDeuteranopia = :deut, scoreProtanopia = :prot WHERE id = :id")
    suspend fun updateScores(id: Int, normal: Int, deut: Int, prot: Int)
    @Update
    suspend fun update(biodata: EntityBiodata)
    @Query("DELETE FROM biodata")
    suspend fun deleteAll()
}
