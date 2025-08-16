package com.colorblind.spectra.UI.correction

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.colorblind.spectra.core.ColorBlindCorrection
import com.colorblind.spectra.data.lokal.room.AppDatabase
import com.colorblind.spectra.databinding.ActivityResultColorCorrectionBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream

class ResultColorCorrectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultColorCorrectionBinding
    private var correctedBitmap: Bitmap? = null   // simpan hasil koreksi

    companion object {
        var originalBitmap: Bitmap? = null   // gambar asli sebelum koreksi
        private const val STORAGE_PERMISSION_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultColorCorrectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launch {
            val biodataDao = AppDatabase.getInstance(this@ResultColorCorrectionActivity).biodataDao()
            val latest = withContext(Dispatchers.IO) { biodataDao.getLatest() }

            val bitmap = originalBitmap
            if (bitmap == null) {
                finish()
                return@launch
            }

            correctedBitmap = when (latest?.hasilTes) {
                "Protanopia" -> ColorBlindCorrection.applyCorrection(
                    bitmap,
                    ColorBlindCorrection.Type.PROTAN
                )
                "Deuteranopia" -> ColorBlindCorrection.applyCorrection(
                    bitmap,
                    ColorBlindCorrection.Type.DEUTAN
                )
                "Normal" -> bitmap
                else -> bitmap
            }

            binding.imgResult.setImageBitmap(correctedBitmap)
            binding.tvInfo.text = "Tipe koreksi: ${latest?.hasilTes ?: "Tidak diketahui"}"
        }

        // Tombol kembali
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Tombol simpan
        binding.btnSave.setOnClickListener {
            correctedBitmap?.let { bmp ->
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    // Android 9 ke bawah butuh WRITE_EXTERNAL_STORAGE
                    if (ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                            STORAGE_PERMISSION_CODE
                        )
                    } else {
                        saveImageToGallery(bmp)
                    }
                } else {
                    // Android 10 ke atas, langsung simpan
                    saveImageToGallery(bmp)
                }
            } ?: Toast.makeText(this, "Gambar belum tersedia", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveImageToGallery(bitmap: Bitmap) {
        val filename = "Correction_${System.currentTimeMillis()}.png"
        val fos: OutputStream?

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/ColorCorrection")
            }
        }

        val imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        fos = imageUri?.let { contentResolver.openOutputStream(it) }

        fos?.use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            Toast.makeText(this, "Gambar berhasil disimpan", Toast.LENGTH_SHORT).show()
        } ?: run {
            Toast.makeText(this, "Gagal menyimpan gambar", Toast.LENGTH_SHORT).show()
        }
    }

    // Callback hasil request permission
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                correctedBitmap?.let { saveImageToGallery(it) }
            } else {
                Toast.makeText(this, "Izin penyimpanan ditolak", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
