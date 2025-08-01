package com.colorblind.spectra.data.lokal.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "biodata")
data class EntityBiodata(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val nama: String,
    val usia: Int,
    val jenisKelamin: String
)
