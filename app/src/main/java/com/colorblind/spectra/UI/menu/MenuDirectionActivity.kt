package com.colorblind.spectra.UI.menu

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import com.colorblind.spectra.R
import com.colorblind.spectra.UI.quiz.IshiharaActivity
import com.colorblind.spectra.databinding.ActivityMenuDirectionBinding

class MenuDirectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMenuDirectionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🔑 Cek apakah user sedang dalam proses tes
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val isInIshihara = prefs.getBoolean("IS_IN_ISHIHARA", false)

        if (isInIshihara) {
            // Kalau iya → langsung lompat ke IshiharaActivity
            startActivity(Intent(this, IshiharaActivity::class.java))
            finish()
            return
        }

        binding = ActivityMenuDirectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnStart.setOnClickListener {
            // Simpan flag bahwa tes sedang berlangsung
            prefs.edit().putBoolean("IS_IN_ISHIHARA", true).apply()

            // Tampilkan progress bar
            binding.progressBar.visibility = View.VISIBLE
            binding.btnStart.isEnabled = false

            // Simulasi loading 2 detik, lalu berpindah ke IshiharaActivity
            Handler(Looper.getMainLooper()).postDelayed({
                binding.progressBar.visibility = View.GONE
                startActivity(Intent(this, IshiharaActivity::class.java))
                finish()
            }, 2000)
        }
        onBackPressedDispatcher.addCallback(this) {
            // Saat tombol back ditekan, jangan kembali ke MenuOptionActivity
            moveTaskToBack(true)
        }
    }
}
