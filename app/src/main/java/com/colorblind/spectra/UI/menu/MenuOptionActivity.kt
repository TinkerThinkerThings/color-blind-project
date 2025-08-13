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
    private lateinit var tvStatusButaWarna: TextView
    private lateinit var menuProfile: LinearLayout
    private lateinit var menuKoreksi: LinearLayout
    private lateinit var menuRealtime: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu_option)

        tvWelcome = findViewById(R.id.tvWelcome)
        tvStatusButaWarna = findViewById(R.id.tvStatusButaWarna)
        menuProfile = findViewById(R.id.menuProfile)
        menuKoreksi = findViewById(R.id.menuKoreksi)
        menuRealtime = findViewById(R.id.menuRealtime)

        val biodataDao = AppDatabase.getInstance(this).biodataDao()

        lifecycleScope.launch(Dispatchers.IO) {
            val biodata = biodataDao.getLatest()
            val nama = biodata?.nama ?: "User"
            val hasilTes = biodata?.hasilTes ?: "Belum melakukan tes"

            withContext(Dispatchers.Main) {
                tvWelcome.text = "Selamat Datang, $nama"
                tvStatusButaWarna.text = "Status buta warna : $hasilTes"
            }
        }

        menuProfile.setOnClickListener {
            val intent = Intent(this, MenuProfileActivity::class.java)
            intent.putExtra("fromMenuOption", true) // Tambah flag untuk loading
            startActivity(intent)
        }

        menuKoreksi.setOnClickListener {
            startActivity(Intent(this, ColorCorrectionDirectionActivity::class.java))
        }

        menuRealtime.setOnClickListener {
            startActivity(Intent(this, RealtimeProcessDirectionActivity::class.java))
        }
    }
}

