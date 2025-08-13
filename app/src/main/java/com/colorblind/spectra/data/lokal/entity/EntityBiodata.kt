package com.colorblind.spectra.data.lokal.entity

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "biodata")
data class EntityBiodata(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val nama: String,
    val usia: Int,
    val jenisKelamin: String,
    val isIshiharaDone: Boolean = false,
    val score: Int? = null,
    val hasilTes: String? = null,
    // Kolom baru untuk menyimpan skor tiap kategori
    val scoreNormal: Int? = null,
    val scoreDeuteranopia: Int? = null,
    val scoreProtanopia: Int? = null
): Parcelable


