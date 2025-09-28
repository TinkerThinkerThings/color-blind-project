package com.colorblind.spectra.UI.quiz

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import com.colorblind.spectra.R
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
    private val prefs by lazy { getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Tandai bahwa user sedang ada di ResultActivity
        prefs.edit().putBoolean("IS_IN_RESULT", true).apply()

        // Buat AlertDialog loading dari XML
        val dialogView = layoutInflater.inflate(R.layout.dialog_loading, null)
        val loadingDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        loadingDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        loadingDialog.window?.setDimAmount(0.8f)
        loadingDialog.show()

        CoroutineScope(Dispatchers.IO).launch {
            val dataJob = async {
                val db = AppDatabase.getInstance(applicationContext)
                db.biodataDao().getLatest()
            }
            delay(1500L)
            val latestBiodata = dataJob.await()

            withContext(Dispatchers.Main) {
                loadingDialog.dismiss()

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

                // Tombol kembali ke Menu Utama
                binding.buttonMenu.setOnClickListener {
                    // Hapus flag ResultActivity
                    prefs.edit().putBoolean("IS_IN_RESULT", false).apply()

                    val intent = Intent(this@ResultActivity, MenuOptionActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }

            }
        }
        onBackPressedDispatcher.addCallback(this) {
            // Saat tombol back ditekan, jangan kembali ke MenuOptionActivity
            moveTaskToBack(true)
        }
    }
}
