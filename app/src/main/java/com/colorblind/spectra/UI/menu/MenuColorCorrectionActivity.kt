package com.colorblind.spectra.UI.menu

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.colorblind.spectra.UI.correction.ResultColorCorrectionActivity
import com.colorblind.spectra.databinding.ActivityMenuColorCorrectionBinding

class MenuColorCorrectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMenuColorCorrectionBinding
    private val REQUEST_CAMERA = 100
    private val REQUEST_GALLERY = 200
    private val CAMERA_PERMISSION_CODE = 300

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMenuColorCorrectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Tombol kamera
        binding.btnCamera.setOnClickListener {
            checkCameraPermission()
        }

        // Tombol galeri
        binding.btnGallery.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, REQUEST_GALLERY)
        }
    }

    /**
     * Cek permission kamera
     */
    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        } else {
            openCamera()
        }
    }

    /**
     * Kalau user pilih izinkan
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                Toast.makeText(this, "Izin kamera ditolak", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Buka kamera
     */
    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, REQUEST_CAMERA)
    }

    /**
     * Ambil hasil dari kamera / galeri
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_CAMERA -> {
                    val bitmap = data?.extras?.get("data") as? Bitmap
                    bitmap?.let { goToResult(it) }
                }
                REQUEST_GALLERY -> {
                    val uri: Uri? = data?.data
                    uri?.let {
                        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            val source = ImageDecoder.createSource(contentResolver, it)
                            ImageDecoder.decodeBitmap(source)
                        } else {
                            MediaStore.Images.Media.getBitmap(contentResolver, it)
                        }
                        goToResult(bitmap)
                    }
                }
            }
        }
    }

    /**
     * Kirim bitmap ke ResultColorCorrectionActivity
     */
    private fun goToResult(bitmap: Bitmap) {
        ResultColorCorrectionActivity.originalBitmap = bitmap
        startActivity(Intent(this, ResultColorCorrectionActivity::class.java))
    }
}
