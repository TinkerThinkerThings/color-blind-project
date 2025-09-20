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

    private val prefs by lazy { getSharedPreferences("splash_prefs", MODE_PRIVATE) }
    private val SPLASH_TIMEOUT = 30_000L // 30 detik

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Cek fresh start
        val isFreshStart = isTaskRoot && savedInstanceState == null
        val lastExit = prefs.getLong("LAST_EXIT", 0L)
        val now = System.currentTimeMillis()

        if (!isFreshStart && lastExit != 0L && now - lastExit <= SPLASH_TIMEOUT) {
            // Kalau buka lagi dalam <=30 detik & bukan fresh start → skip splash
            goNext()
            return
        }

        // ✅ Kalau fresh start atau sudah lama ditutup → tampilkan splash
        setContentView(R.layout.activity_splash)

        val logoImage: ImageView = findViewById(R.id.logoImage)
        val appNameText: TextView = findViewById(R.id.appNameText)

        // Fade-in logo
        logoImage.animate()
            .alpha(1f)
            .setDuration(1500)
            .withEndAction {
                // Fade-in teks
                appNameText.animate()
                    .alpha(1f)
                    .setDuration(1000)
                    .withEndAction {
                        goNext()
                    }
                    .start()
            }
            .start()
    }

    private fun goNext() {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(this@SplashActivity)
            val biodataList = db.biodataDao().getAll()

            // Cek halaman terakhir yang dibuka
            val lastActivity = prefs.getString("LAST_ACTIVITY", null)

            val nextActivity = if (lastActivity != null) {
                try {
                    Class.forName(lastActivity)
                } catch (e: Exception) {
                    // fallback kalau class tidak ditemukan
                    if (biodataList.isEmpty()) SliderActivity::class.java
                    else if (!biodataList[0].isIshiharaDone) MenuDirectionActivity::class.java
                    else MenuOptionActivity::class.java
                }
            } else {
                if (biodataList.isEmpty()) SliderActivity::class.java
                else if (!biodataList[0].isIshiharaDone) MenuDirectionActivity::class.java
                else MenuOptionActivity::class.java
            }

            Handler(Looper.getMainLooper()).post {
                startActivity(Intent(this@SplashActivity, nextActivity))
                finish()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Simpan waktu saat aplikasi ditutup
        prefs.edit().putLong("LAST_EXIT", System.currentTimeMillis()).apply()
    }
}
