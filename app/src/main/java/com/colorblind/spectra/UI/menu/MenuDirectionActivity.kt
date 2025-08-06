package com.colorblind.spectra.UI.menu

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import com.colorblind.spectra.R
import com.colorblind.spectra.UI.quiz.IshiharaActivity
import com.colorblind.spectra.databinding.ActivityMenuDirectionBinding

class MenuDirectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMenuDirectionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMenuDirectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnStart.setOnClickListener {
            // Tampilkan progress bar
            binding.progressBar.visibility = View.VISIBLE
            binding.btnStart.isEnabled = false

            // Simulasi loading 2 detik, lalu berpindah ke MenuIshiharaActivity
            Handler(Looper.getMainLooper()).postDelayed({
                binding.progressBar.visibility = View.GONE
                startActivity(Intent(this, IshiharaActivity::class.java))
                finish()
            }, 2000)
        }
    }
}
