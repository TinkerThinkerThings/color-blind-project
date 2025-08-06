package com.colorblind.spectra.UI

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.colorblind.spectra.R
import com.colorblind.spectra.UI.menu.MenuDirectionActivity
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
                        // Panggil database di thread IO
                        CoroutineScope(Dispatchers.IO).launch {
                            val db = AppDatabase.getInstance(this@SplashActivity)
                            val biodataList = db.biodataDao().getAll()

                            val nextActivity = when {
                                biodataList.isEmpty() -> {
                                    // Belum ada data → jalankan onboarding Slider
                                    SliderActivity::class.java
                                }
                                !biodataList[0].isIshiharaDone -> {
                                    // Ada biodata, tapi belum selesai tes → ke menu petunjuk
                                   MenuDirectionActivity::class.java
                                }
                                else -> {
                                    // Sudah ada data & sudah selesaikan tes → ke menu utama
                                    MenuOptionActivity::class.java
                                }
                            }
                            // Pindah activity di main thread
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
