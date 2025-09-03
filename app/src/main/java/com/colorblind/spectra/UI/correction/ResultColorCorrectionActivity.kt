package com.colorblind.spectra.UI.correction

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.colorblind.spectra.core.ColorBlindCorrection
import com.colorblind.spectra.data.lokal.room.AppDatabase
import com.colorblind.spectra.databinding.ActivityResultColorCorrectionBinding
import com.colorblind.spectra.utils.ColorUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.io.OutputStream
import kotlin.random.Random

class ResultColorCorrectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultColorCorrectionBinding
    private var correctedBitmap: Bitmap? = null   // Variabel untuk menyimpan hasil gambar yang sudah dikoreksi

    companion object {
        var originalBitmap: Bitmap? = null   // Gambar asli yang dikirim dari activity sebelumnya
        private const val STORAGE_PERMISSION_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inisialisasi OpenCV
        if (!OpenCVLoader.initDebug()) {
            Toast.makeText(this, "Gagal memuat pustaka OpenCV", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding = ActivityResultColorCorrectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Memulai proses utama menggunakan Coroutine
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            binding.tvInfo.text = "Menerapkan koreksi dan menghitung perbedaan warna..."

            val biodataDao = AppDatabase.getInstance(this@ResultColorCorrectionActivity).biodataDao()
            val latestUserData = withContext(Dispatchers.IO) { biodataDao.getLatest() }

            val bitmap = originalBitmap
            if (bitmap == null) {
                Toast.makeText(this@ResultColorCorrectionActivity, "Gagal memuat gambar", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }

            val safeOriginalBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)

            val matTmp = Mat()
            Utils.bitmapToMat(safeOriginalBitmap, matTmp)

            val srcMat = Mat()
            Imgproc.cvtColor(matTmp, srcMat, Imgproc.COLOR_RGBA2RGB)
            matTmp.release()

            val correctedMat = when (latestUserData?.hasilTes) {
                "Protanopia" -> ColorBlindCorrection.daltonize(srcMat, "protan")
                "Deuteranopia" -> ColorBlindCorrection.daltonize(srcMat, "deutan")
                else -> srcMat.clone()
            }
            srcMat.release()

            val resultBitmap = Bitmap.createBitmap(correctedMat.cols(), correctedMat.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(correctedMat, resultBitmap)
            correctedMat.release()
            correctedBitmap = resultBitmap

            val averageDeltaE = withContext(Dispatchers.Default) {
                calculateAverageDeltaE(safeOriginalBitmap, resultBitmap)
            }

            binding.progressBar.visibility = View.GONE
            binding.imgResult.setImageBitmap(correctedBitmap)

            val correctionType = latestUserData?.hasilTes ?: "Tidak diketahui"
            val formattedDeltaE = String.format("%.2f", averageDeltaE)

            if (correctionType == "Normal" || averageDeltaE < 1.0) {
                binding.tvInfo.text = "Tipe: Normal (Tidak ada koreksi diterapkan)"
            } else {
                binding.tvInfo.text = "Tipe koreksi: $correctionType\nRata-rata ΔE2000: $formattedDeltaE"
            }

            // 🔹 Tambahan: tampilkan nilai RGB sebelum & sesudah koreksi
            showSampleRGB(safeOriginalBitmap, resultBitmap)
        }

        binding.btnBack.setOnClickListener { finish() }

        binding.btnSave.setOnClickListener {
            correctedBitmap?.let { bmp ->
                requestStoragePermissionAndSave(bmp)
            } ?: Toast.makeText(this, "Gambar belum selesai diproses", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Fungsi untuk menampilkan RGB random sample sebelum & sesudah koreksi
     */
    private fun showSampleRGB(original: Bitmap, corrected: Bitmap) {
        if (original.width == 0 || original.height == 0) return

        val x = Random.nextInt(original.width)
        val y = Random.nextInt(original.height)

        val origPixel = original.getPixel(x, y)
        val corrPixel = corrected.getPixel(x, y)

        val r1 = (origPixel shr 16) and 0xff
        val g1 = (origPixel shr 8) and 0xff
        val b1 = origPixel and 0xff

        val r2 = (corrPixel shr 16) and 0xff
        val g2 = (corrPixel shr 8) and 0xff
        val b2 = corrPixel and 0xff

        binding.tvRgbInfo.text = "RGB Sebelum: ($r1, $g1, $b1)\nRGB Sesudah: ($r2, $g2, $b2)"
    }

    private fun calculateAverageDeltaE(original: Bitmap, corrected: Bitmap): Double {
        if (original.width != corrected.width || original.height != corrected.height) {
            Log.e("DeltaE_Frame", "Error: Ukuran bitmap tidak sama!")
            return 0.0
        }

        var totalDeltaE = 0.0
        val width = original.width
        val height = original.height
        val pixelCount = width * height

        for (y in 0 until height) {
            for (x in 0 until width) {
                val originalPixel = original.getPixel(x, y)
                val correctedPixel = corrected.getPixel(x, y)

                if (originalPixel == correctedPixel) continue

                val r1 = (originalPixel shr 16) and 0xff
                val g1 = (originalPixel shr 8) and 0xff
                val b1 = originalPixel and 0xff

                val r2 = (correctedPixel shr 16) and 0xff
                val g2 = (correctedPixel shr 8) and 0xff
                val b2 = correctedPixel and 0xff

                val lab1 = ColorUtils.rgbToLab(r1, g1, b1)
                val lab2 = ColorUtils.rgbToLab(r2, g2, b2)
                val deltaE = ColorUtils.deltaE2000(lab1, lab2)

                totalDeltaE += deltaE
            }
        }

        if (pixelCount == 0) return 0.0
        return totalDeltaE / pixelCount
    }

    private fun requestStoragePermissionAndSave(bitmap: Bitmap) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), STORAGE_PERMISSION_CODE)
            } else {
                saveImageToGallery(bitmap)
            }
        } else {
            saveImageToGallery(bitmap)
        }
    }

    private fun saveImageToGallery(bitmap: Bitmap) {
        val filename = "SpectraCorrection_${System.currentTimeMillis()}.png"
        val fos: OutputStream?

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/Spectra")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        try {
            fos = imageUri?.let { contentResolver.openOutputStream(it) }
            fos?.use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                Toast.makeText(this, "Gambar berhasil disimpan", Toast.LENGTH_SHORT).show()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                imageUri?.let { contentResolver.update(it, contentValues, null, null) }
            }
        } catch (e: Exception) {
            Log.e("SaveImage", "Gagal menyimpan gambar", e)
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
                Toast.makeText(this, "Izin penyimpanan ditolak. Gambar tidak dapat disimpan.", Toast.LENGTH_LONG).show()
            }
        }
    }
}
