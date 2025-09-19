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
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.io.OutputStream

class ResultColorCorrectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultColorCorrectionBinding
    private var correctedBitmap: Bitmap? = null   // hasil koreksi

    companion object {
        var originalBitmap: Bitmap? = null   // gambar asli sebelum koreksi
        private const val STORAGE_PERMISSION_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load OpenCV
        if (!OpenCVLoader.initDebug()) {
            Toast.makeText(this, "Gagal memuat OpenCV", Toast.LENGTH_SHORT).show()
            return
        }

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

            // Pastikan bitmap ARGB_8888 & mutable
            val safeBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)

            // Konversi Bitmap ke Mat
            val matTmp = Mat()
            Utils.bitmapToMat(safeBitmap, matTmp)

            // Konversi ke RGB 3-channel (sesuai ColorBlindCorrection)
            val srcMat = Mat()
            if (matTmp.channels() == 4) {
                Imgproc.cvtColor(matTmp, srcMat, Imgproc.COLOR_RGBA2RGB)
            } else if (matTmp.channels() == 3) {
                Imgproc.cvtColor(matTmp, srcMat, Imgproc.COLOR_BGR2RGB)
            } else {
                matTmp.release()
                srcMat.release()
                Toast.makeText(this@ResultColorCorrectionActivity, "Format gambar tidak didukung", Toast.LENGTH_SHORT).show()
                return@launch
            }

            // Terapkan KOREKSI (daltonize) sesuai hasil tes
            val correctedMat = when (latest?.hasilTes) {
                "Protanopia" -> ColorBlindCorrection.daltonize(srcMat, "protan")
                "Deuteranopia" -> ColorBlindCorrection.daltonize(srcMat, "deutan")
                "Normal" -> srcMat
                else -> srcMat
            }

            // Konversi kembali Mat ke Bitmap
            val resultBitmap = Bitmap.createBitmap(
                correctedMat.cols(),
                correctedMat.rows(),
                Bitmap.Config.ARGB_8888
            )
            Utils.matToBitmap(correctedMat, resultBitmap)

            correctedBitmap = resultBitmap

            // Tampilkan default = hasil koreksi
            binding.imgResult.setImageBitmap(correctedBitmap)
            binding.tvInfo.text = "Tipe koreksi: ${latest?.hasilTes ?: "Tidak diketahui"}"
        }

        // 🔹 Tombol Before → tampilkan gambar asli
        binding.btnBefore.setOnClickListener {
            originalBitmap?.let {
                binding.imgResult.setImageBitmap(it)
                binding.tvMode.text = "Tampilan: Sebelum Koreksi"
            }
        }

        // 🔹 Tombol After → tampilkan hasil koreksi
        binding.btnAfter.setOnClickListener {
            correctedBitmap?.let {
                binding.imgResult.setImageBitmap(it)
                binding.tvMode.text = "Tampilan: Sesudah Koreksi"
            }
        }

        // Tombol kembali
        binding.btnBack.setOnClickListener { finish() }

        // Tombol simpan
        binding.btnSave.setOnClickListener {
            correctedBitmap?.let { bmp ->
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
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
