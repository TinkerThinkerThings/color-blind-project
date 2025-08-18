package com.colorblind.spectra.UI.menu

import android.Manifest
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.colorblind.spectra.R
import com.colorblind.spectra.core.RealtimeColorCorrection
import com.colorblind.spectra.core.YuvToRgbConverter
import com.colorblind.spectra.data.lokal.entity.EntityBiodata
import com.colorblind.spectra.data.lokal.room.AppDatabase
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

class MenuProcessingActivity : AppCompatActivity() {

    // UI
    private lateinit var previewView: PreviewView
    private lateinit var imageResult: ImageView
    private lateinit var swLab: MaterialSwitch
    private lateinit var seekSeverity: Slider
    private lateinit var seekBoost: Slider
    private lateinit var tvSeverity: TextView
    private lateinit var tvBoost: TextView
    private lateinit var tvFps: TextView
    private lateinit var rbNormal: RadioButton
    private lateinit var rbProtan: RadioButton
    private lateinit var rbDeutan: RadioButton
    private lateinit var btnToggle: MaterialSwitch
    private lateinit var progress: ProgressBar

    // Variabel logic
    private var userType: RealtimeColorCorrection.Type = RealtimeColorCorrection.Type.DEUTAN
    private var severity = 1.0
    private var boost = 1.0
    private var running = false

    // CameraX
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private lateinit var analyzer: ImageAnalysis

    private val askCam = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startCamera() else finish()
    }

    companion object {
        init {
            try {
                System.loadLibrary("opencv_java4")
                Log.i("OpenCV", "✅ OpenCV library loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                Log.e("OpenCV", "❌ Failed to load OpenCV: ${e.message}")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_menu_processing)

        // Handle insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Bind UI
        previewView = findViewById(R.id.previewView)
        imageResult = findViewById(R.id.imageResult)
        swLab = findViewById(R.id.swLab)
        tvSeverity = findViewById(R.id.tvSeverity)
        tvBoost = findViewById(R.id.tvBoost)
        seekSeverity = findViewById(R.id.seekSeverity)
        seekBoost = findViewById(R.id.seekBoost)
        tvFps = findViewById(R.id.tvFps)
        rbNormal = findViewById(R.id.rbNormal)
        rbProtan = findViewById(R.id.rbProtan)
        rbDeutan = findViewById(R.id.rbDeutan)
        btnToggle = findViewById(R.id.btnToggle)
        progress = findViewById(R.id.progress)

        // Sinkronkan state awal toggle ke variabel running
        running = btnToggle.isChecked

        // Ambil hasil tes dari DB
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getInstance(applicationContext)
            val latest: EntityBiodata? = db.biodataDao().getLatest()

            val mapped = when (latest?.hasilTes?.lowercase()) {
                "protanopia", "protan" -> RealtimeColorCorrection.Type.PROTAN
                "deuteranopia", "deutan" -> RealtimeColorCorrection.Type.DEUTAN
                else -> RealtimeColorCorrection.Type.NORMAL
            }

            withContext(Dispatchers.Main) {
                userType = mapped
                when (userType) {
                    RealtimeColorCorrection.Type.NORMAL -> rbNormal.isChecked = true
                    RealtimeColorCorrection.Type.PROTAN -> rbProtan.isChecked = true
                    RealtimeColorCorrection.Type.DEUTAN -> rbDeutan.isChecked = true
                }
            }
        }

        // Setup sliders
        seekSeverity.valueFrom = 0f
        seekSeverity.valueTo = 1f
        seekSeverity.value = 1f
        tvSeverity.text = "Severity: 1.00"

        seekBoost.valueFrom = 0f
        seekBoost.valueTo = 1f
        seekBoost.value = 1f
        tvBoost.text = "Boost: 1.00"

        seekSeverity.addOnChangeListener { _, value, _ ->
            severity = value.toDouble()
            tvSeverity.text = "Severity: ${"%.2f".format(severity)}"
        }

        seekBoost.addOnChangeListener { _, value, _ ->
            boost = value.toDouble()
            tvBoost.text = "Boost: ${"%.2f".format(boost)}"
        }

        // Radio button override
        rbNormal.setOnCheckedChangeListener { _, checked -> if (checked) userType = RealtimeColorCorrection.Type.NORMAL }
        rbProtan.setOnCheckedChangeListener { _, checked -> if (checked) userType = RealtimeColorCorrection.Type.PROTAN }
        rbDeutan.setOnCheckedChangeListener { _, checked -> if (checked) userType = RealtimeColorCorrection.Type.DEUTAN }

        // Toggle button
        btnToggle.setOnCheckedChangeListener { _, checked ->
            running = checked
            Log.i("MenuProcessing", "▶ Running mode = $running")
        }

        // Minta permission camera
        askCam.launch(Manifest.permission.CAMERA)
    }

    @OptIn(ExperimentalGetImage::class)
    private fun startCamera() {
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            val provider = providerFuture.get()

            val preview = Preview.Builder()
                .setTargetResolution(Size(640, 480))
                .build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }

            analyzer = ImageAnalysis.Builder()
                .setTargetResolution(Size(640, 480))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            val yuvToRgb = YuvToRgbConverter(this)

            analyzer.setAnalyzer(cameraExecutor) { imageProxy ->
                try {
                    val bmp = Bitmap.createBitmap(
                        imageProxy.width,
                        imageProxy.height,
                        Bitmap.Config.ARGB_8888
                    )

                    // Konversi YUV -> RGB
                    yuvToRgb.yuvToRgb(imageProxy, bmp)

                    val start = System.nanoTime()

                    // 🔹 Pilih output: normal camera atau hasil processing
                    val out = if (!running || userType == RealtimeColorCorrection.Type.NORMAL) {
                        bmp
                    } else {
                        RealtimeColorCorrection.processFrame(
                            srcBitmap = bmp,
                            type = userType,
                            severity = severity,
                            boost = boost,
                            useLabFinishing = swLab.isChecked
                        )
                    }

                    val end = System.nanoTime()
                    val fps = 1e9 / (end - start)

                    // 🔹 Rotasi sesuai orientasi kamera
                    val rotation = imageProxy.imageInfo.rotationDegrees
                    val rotated = if (rotation != 0) {
                        val matrix = Matrix()
                        matrix.postRotate(rotation.toFloat())
                        Bitmap.createBitmap(out, 0, 0, out.width, out.height, matrix, true)
                    } else {
                        out
                    }

                    runOnUiThread {
                        imageResult.setImageBitmap(rotated)

                        // FPS hanya tampil kalau processing aktif
                        if (running && userType != RealtimeColorCorrection.Type.NORMAL) {
                            tvFps.text = "FPS ~ ${"%.1f".format(fps)}"
                            tvFps.visibility = View.VISIBLE
                            progress.visibility = View.GONE
                        } else {
                            tvFps.visibility = View.GONE
                            progress.visibility = View.VISIBLE
                        }
                    }

                } catch (e: Exception) {
                    Log.e("Analyzer", "❌ Error processing frame: ${e.message}")
                } finally {
                    imageProxy.close()
                }
            }

            val selector = CameraSelector.DEFAULT_BACK_CAMERA
            provider.unbindAll()
            provider.bindToLifecycle(this, selector, preview, analyzer)
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
