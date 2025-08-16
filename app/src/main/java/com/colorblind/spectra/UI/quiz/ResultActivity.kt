package com.colorblind.spectra.UI.quiz

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import com.colorblind.spectra.R // Pastikan import R benar
import com.colorblind.spectra.UI.menu.MenuOptionActivity
import com.colorblind.spectra.data.lokal.room.AppDatabase
import com.colorblind.spectra.databinding.ActivityResultBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hapus pemanggilan class LoadingDialog
        // val loadingDialog = LoadingDialog(this)

        // 1. Buat AlertDialog secara langsung dari file XML
        val dialogView = layoutInflater.inflate(R.layout.dialog_loading, null)
        val loadingDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        // Opsi: agar background transparan seperti contoh sebelumnya
        loadingDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        loadingDialog.window?.setDimAmount(0.8f) // tingkat gelap 0.0 - 1.0
        // 2. Tampilkan dialog
        loadingDialog.show()

        CoroutineScope(Dispatchers.IO).launch {
            // Logika untuk ambil data dan delay tetap sama
            val dataJob = async {
                val db = AppDatabase.getInstance(applicationContext)
                db.biodataDao().getLatest()
            }
            delay(1500L)
            val latestBiodata = dataJob.await()

            withContext(Dispatchers.Main) {
                // 3. Tutup dialog yang sudah dibuat
                loadingDialog.dismiss()

                // Tampilkan hasil ke UI
                if (latestBiodata != null) {
                    binding.textClassification.text = latestBiodata.hasilTes
                    binding.valueNormal.text = "${latestBiodata.scoreNormal}"
                    binding.valueDeuteranopia.text = "${latestBiodata.scoreDeuteranopia}"
                    binding.valueProtanopia.text = "${latestBiodata.scoreProtanopia}"
                } else {
                    binding.textClassification.text = "Data tidak ditemukan"
                    binding.valueNormal.text = "Normal: -"
                    binding.valueDeuteranopia.text = "Deuteranopia: -"
                    binding.valueProtanopia.text = "Protanopia: -"
                }

                binding.buttonMenu.setOnClickListener {
                    val intent = Intent(this@ResultActivity, MenuOptionActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                    finish()
                }
            }
        }
    }
}