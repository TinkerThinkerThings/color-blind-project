package com.colorblind.spectra.UI.quiz

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.colorblind.spectra.UI.menu.MenuOptionActivity
import com.colorblind.spectra.data.lokal.room.AppDatabase
import com.colorblind.spectra.databinding.ActivityResultBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Tampilkan loading dialog
        val loadingDialog = LoadingDialog(this)
        loadingDialog.show()

        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(applicationContext)
            val latestBiodata = db.biodataDao().getLatest()

            withContext(Dispatchers.Main) {
                loadingDialog.dismiss()

                if (latestBiodata != null) {
                    binding.textScore.text = latestBiodata.score.toString()
                    binding.textClassification.text = latestBiodata.hasilTes
                } else {
                    binding.textScore.text = "-"
                    binding.textClassification.text = "Data tidak ditemukan"
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
