package com.colorblind.spectra.UI.form

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.colorblind.spectra.R
import com.colorblind.spectra.UI.menu.MenuDirectionActivity
import com.colorblind.spectra.UI.menu.MenuOptionActivity
import com.colorblind.spectra.data.lokal.entity.EntityBiodata
import com.colorblind.spectra.data.lokal.room.AppDatabase
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FormActivity : AppCompatActivity() {

    private lateinit var etNama: EditText
    private lateinit var etUsia: EditText
    private lateinit var radioGroupJenisKelamin: RadioGroup
    private lateinit var btnSimpan: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_form)

        etNama = findViewById(R.id.namaLengkapEdit)
        etUsia = findViewById(R.id.usiaEdit)
        radioGroupJenisKelamin = findViewById(R.id.radioGroupJenisKelamin)
        btnSimpan = findViewById(R.id.btnSimpan)

        val db = AppDatabase.getInstance(this)
        val biodataDao = db.biodataDao()

        btnSimpan.setOnClickListener {
            val nama = etNama.text.toString().trim()
            val usiaText = etUsia.text.toString().trim()

            // Ambil RadioButton yang dipilih
            val selectedId = radioGroupJenisKelamin.checkedRadioButtonId
            val jenisKelamin = if (selectedId != -1) {
                findViewById<RadioButton>(selectedId).text.toString().trim()
            } else {
                ""
            }

            // Validasi kosong
            if (nama.isEmpty() || usiaText.isEmpty() || jenisKelamin.isEmpty()) {
                Toast.makeText(this, "Isi semua data!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validasi nama hanya huruf & spasi, max 100 huruf
            val namaRegex = "^[A-Za-z\\s]{1,100}$".toRegex()
            if (!namaRegex.matches(nama)) {
                etNama.error = "Nama hanya huruf & max 100 huruf"
                return@setOnClickListener
            }

            // Validasi usia hanya angka, max 2 digit
            val usiaRegex = "^\\d{1,2}$".toRegex()
            if (!usiaRegex.matches(usiaText)) {
                etUsia.error = "Usia hanya angka, max 2 digit"
                return@setOnClickListener
            }

            val usia = usiaText.toIntOrNull()
            if (usia == null) {
                etUsia.error = "Usia tidak valid"
                return@setOnClickListener
            }

            CoroutineScope(Dispatchers.IO).launch {
                biodataDao.insert(
                    EntityBiodata(
                        nama = nama,
                        usia = usia,
                        jenisKelamin = jenisKelamin
                    )
                )
                runOnUiThread {
                    Toast.makeText(this@FormActivity, "Data disimpan!", Toast.LENGTH_SHORT).show()
                    // Pindah ke MenuOptionActivity setelah simpan
                    val intent = Intent(this@FormActivity, MenuDirectionActivity::class.java)
                    startActivity(intent)
                    finish() // Tutup FormActivity biar tidak bisa kembali
                }
            }
        }
    }
}
