package com.colorblind.spectra.UI.form

import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.colorblind.spectra.R
import com.google.android.material.button.MaterialButton

class FormActivity : AppCompatActivity() {

    private lateinit var etNama: EditText
    private lateinit var etUsia: EditText
    private lateinit var etJenisKelamin: EditText
    private lateinit var btnSimpan: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_form)

        // Temukan view
        etNama = findViewById(R.id.namaLengkapEdit)
        etUsia = findViewById(R.id.usiaEdit)
        etJenisKelamin = findViewById(R.id.jenisKelaminEdit)
        btnSimpan = findViewById(R.id.btnSimpan)

        // Atur input type & filter untuk usia
        etUsia.inputType = InputType.TYPE_CLASS_NUMBER
        etUsia.filters = arrayOf(InputFilter.LengthFilter(2))

        btnSimpan.setOnClickListener {
            validateForm()
        }
    }

    private fun validateForm() {
        val nama = etNama.text.toString().trim()
        val usia = etUsia.text.toString().trim()
        val jenisKelamin = etJenisKelamin.text.toString().trim()

        if (nama.isEmpty()) {
            etNama.error = "Nama harus diisi"
            return
        }

        if (!nama.matches(Regex("^[a-zA-Z ]+$"))) {
            etNama.error = "Nama hanya huruf"
            return
        }

        if (usia.isEmpty()) {
            etUsia.error = "Usia harus diisi"
            return
        }

        if (!usia.matches(Regex("^[0-9]{1,2}$"))) {
            etUsia.error = "Usia hanya angka 1-99"
            return
        }

        if (jenisKelamin.isEmpty()) {
            etJenisKelamin.error = "Jenis kelamin harus diisi"
            return
        }

        Toast.makeText(this, "Biodata berhasil disimpan!", Toast.LENGTH_SHORT).show()
    }
}
