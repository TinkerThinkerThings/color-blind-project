package com.colorblind.spectra.UI

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.colorblind.spectra.R
import com.colorblind.spectra.UI.slider.SliderActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logoImage: ImageView = findViewById(R.id.logoImage)
        val appNameText: TextView = findViewById(R.id.appNameText)

        // Fade-in logo
        logoImage.animate()
            .alpha(1f)
            .setDuration(1500)
            .withEndAction {
                // Lanjut fade-in teks setelah logo selesai
                appNameText.animate()
                    .alpha(1f)
                    .setDuration(1000)
                    .withEndAction {
                        // Setelah selesai semua, delay 1 detik lalu masuk MainActivity
                        Handler(Looper.getMainLooper()).postDelayed({
                            startActivity(Intent(this, SliderActivity::class.java))
                            finish()
                        }, 1000)
                    }
                    .start()
            }
            .start()
    }
}