package com.colorblind.spectra.UI.menu

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.colorblind.spectra.R

class RealtimeProcessDirectionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_realtime_process_direction)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val buttonStart = findViewById<Button>(R.id.buttonStart)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        buttonStart.setOnClickListener {
            // Tampilkan progress bar
            progressBar.visibility = View.VISIBLE

            // Nonaktifkan tombol agar tidak diklik berkali-kali
            buttonStart.isEnabled = false

            // Jalankan delay 2 detik
            Handler(Looper.getMainLooper()).postDelayed({
                val intent = Intent(this, MenuProcessingActivity::class.java)
                startActivity(intent)
                // Sembunyikan progress bar (opsional kalau kembali ke activity ini)
                progressBar.visibility = View.GONE
                buttonStart.isEnabled = true
            }, 2000)
        }
    }
}