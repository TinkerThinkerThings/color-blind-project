package com.colorblind.spectra.UI

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.colorblind.spectra.R
import com.colorblind.spectra.UI.menu.MenuOptionActivity
import com.colorblind.spectra.UI.slider.SliderActivity
import com.colorblind.spectra.data.lokal.room.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
                        // Jalankan pengecekan Room di thread IO
                        CoroutineScope(Dispatchers.IO).launch {
                            val db = AppDatabase.getInstance(this@SplashActivity)
                            val biodataList = db.biodataDao().getAll()

                            val nextActivity = if (biodataList.isNotEmpty()) {
                                // Sudah ada data → langsung ke MenuOptionActivity
                                MenuOptionActivity::class.java
                            } else {
                                // Belum ada data → jalankan onboarding Slider
                                SliderActivity::class.java
                            }

                            // Pindah activity di UI thread
                            Handler(Looper.getMainLooper()).post {
                                startActivity(Intent(this@SplashActivity, nextActivity))
                                finish()
                            }
                        }
                    }
                    .start()
            }
            .start()
    }
}
