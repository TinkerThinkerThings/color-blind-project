package com.colorblind.spectra.UI.menu

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.colorblind.spectra.R
import com.colorblind.spectra.UI.form.FormActivity
import com.colorblind.spectra.data.lokal.room.AppDatabase
import com.colorblind.spectra.databinding.ActivityMenuProfileBinding
import com.colorblind.spectra.databinding.DialogWarningBinding
import com.colorblind.spectra.databinding.DialogLoadingBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MenuProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMenuProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMenuProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Cek apakah datang dari MenuOptionActivity
        val fromMenuOption = intent.getBooleanExtra("fromMenuOption", false)
        if (fromMenuOption) {
            showLoadingDialog {
                loadDataProfile()
            }
        } else {
            loadDataProfile()
        }

        binding.btnUlangiTes.setOnClickListener {
            showWarningDialog()
        }
    }

    /** Ambil data profil terakhir dari database */
    private fun loadDataProfile() {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getInstance(applicationContext)
            val biodata = db.biodataDao().getLatest()
            if (biodata != null) {
                withContext(Dispatchers.Main) {
                    binding.tvNamaLengkap.text = biodata.nama
                    binding.tvUsia.text = biodata.usia.toString()
                    binding.tvJenisKelamin.text = biodata.jenisKelamin
                    binding.tvScoreNormal.text = "${biodata.scoreNormal}"
                    binding.tvScoreDeuteranopia.text = "${biodata.scoreDeuteranopia}"
                    binding.tvScoreProtanopia.text = "${biodata.scoreProtanopia}"
                    binding.tvHasilTesFinal.text = biodata.hasilTes
                }
            }
        }
    }

    private fun showWarningDialog() {
        val dialogBinding = DialogWarningBinding.inflate(layoutInflater)

        val dialog = AlertDialog.Builder(this, R.style.CustomDialog)
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnUlangi.setOnClickListener {
            dialog.dismiss()

            // Jalankan coroutine untuk operasi database di background thread
            lifecycleScope.launch(Dispatchers.IO) {
                val db = AppDatabase.getInstance(applicationContext)
                db.biodataDao().deleteAll()

                withContext(Dispatchers.Main) {
                    val intent = Intent(this@MenuProfileActivity, FormActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
        }
        dialog.show()
    }

    /** Menampilkan dialog loading */
    private fun showLoadingDialog(onFinished: () -> Unit) {
        val loadingBinding = DialogLoadingBinding.inflate(layoutInflater)

        val loadingDialog = AlertDialog.Builder(this)
            .setView(loadingBinding.root)
            .setCancelable(false)
            .create()

        loadingDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        loadingDialog.window?.setDimAmount(0.8f) // tingkat gelap 0.0 - 1.0


        loadingDialog.show()

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                Thread.sleep(1500) // durasi loading
            }
            withContext(Dispatchers.Main) {
                loadingDialog.dismiss()
                onFinished()
            }
        }
    }
}