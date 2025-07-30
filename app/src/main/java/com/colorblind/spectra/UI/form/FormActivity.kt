package com.colorblind.spectra.UI.form

import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.colorblind.spectra.R
import com.colorblind.spectra.data.lokal.entity.EntityBiodata
import com.colorblind.spectra.data.lokal.room.AppDatabase
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FormActivity : AppCompatActivity() {

    private lateinit var etNama: EditText
    private lateinit var etUsia: EditText
    private lateinit var etJenisKelamin: EditText
    private lateinit var btnSimpan: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_form)

        etNama = findViewById(R.id.namaLengkapEdit)
        etUsia = findViewById(R.id.usiaEdit)
        etJenisKelamin = findViewById(R.id.jenisKelaminEdit)
        btnSimpan = findViewById(R.id.btnSimpan)

        val db = AppDatabase.getInstance(this)
        val biodataDao = db.biodataDao()

        btnSimpan.setOnClickListener {
            val nama = etNama.text.toString().trim()
            val usiaText = etUsia.text.toString().trim()
            val jenisKelamin = etJenisKelamin.text.toString().trim()

            if (nama.isEmpty() || usiaText.isEmpty() || jenisKelamin.isEmpty()) {
                Toast.makeText(this, "Isi semua data!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val usia = usiaText.toIntOrNull()
            if (usia == null) {
                etUsia.error = "Usia harus angka"
                return@setOnClickListener
            }

            CoroutineScope(Dispatchers.IO).launch {
                biodataDao.insert(EntityBiodata(nama = nama, usia = usia, jenisKelamin = jenisKelamin))

                runOnUiThread {
                    Toast.makeText(this@FormActivity, "Data disimpan!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
