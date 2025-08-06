package com.colorblind.spectra.UI.menu

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.colorblind.spectra.R
import com.colorblind.spectra.UI.form.FormActivity
import com.colorblind.spectra.data.lokal.room.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MenuOptionActivity : AppCompatActivity() {

    private lateinit var tvWelcome: TextView
    private lateinit var menuProfile: LinearLayout
    private lateinit var menuKoreksi: LinearLayout
    private lateinit var menuRealtime: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu_option)

        tvWelcome = findViewById(R.id.tvWelcome)
        menuProfile = findViewById(R.id.menuProfile)
        menuKoreksi = findViewById(R.id.menuKoreksi)
        menuRealtime = findViewById(R.id.menuRealtime)

        val biodataDao = AppDatabase.getInstance(this).biodataDao()

        lifecycleScope.launch(Dispatchers.IO) {
            val biodata = biodataDao.getLatest()
            val nama = biodata?.nama ?: "User"
            withContext(Dispatchers.Main) {
                tvWelcome.text = "Selamat Datang, $nama"
            }
        }

        menuProfile.setOnClickListener {
            startActivity(Intent(this, MenuProfileActivity::class.java))
        }

        menuKoreksi.setOnClickListener {
            startActivity(Intent(this, MenuColorCorrectionActivity::class.java))
        }

        menuRealtime.setOnClickListener {
            startActivity(Intent(this, MenuProcessingActivity::class.java))
        }
    }
}
