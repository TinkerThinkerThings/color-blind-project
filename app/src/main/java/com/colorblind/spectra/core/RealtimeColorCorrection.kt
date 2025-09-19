package com.colorblind.spectra.core

import android.graphics.Bitmap
import android.util.Log
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc

object RealtimeColorCorrection {

    enum class Type { NORMAL, PROTAN, DEUTAN }

    // --- Matrices (float32) ---
    // RGB -> LMS
    private val RGB2LMS = Mat(3, 3, CvType.CV_32F).apply {
        put(0, 0,
            17.8824, 43.5161, 4.11935,
            3.45565, 27.1554, 3.86714,
            0.0299566, 0.184309, 1.46709
        )
    }

    // LMS -> RGB
    private val LMS2RGB = Mat(3, 3, CvType.CV_32F).apply {
        put(0, 0,
            0.0809444479, -0.1305044090, 0.1167210660,
            -0.0102485335, 0.0540193266, -0.1136147080,
            -0.0003652969, -0.0041216147, 0.6935114050
        )
    }

    // Simulasi protan & deutan (sederhana, stabil)
    private val PROTAN_SIM = Mat(3, 3, CvType.CV_32F).apply {
        put(0, 0,
            0.0, 2.02344, -2.52581,
            0.0, 1.0,      0.0,
            0.0, 0.0,      1.0
        )
    }

    private val DEUTAN_SIM = Mat(3, 3, CvType.CV_32F).apply {
        put(0, 0,
            1.0, 0.0, 0.0,
            0.494207, 0.0, 1.24827,
            0.0, 0.0, 1.0
        )
    }

    /**
     * Proses satu frame.
     * - srcBitmap: ARGB_8888 dari kamera
     * - severity: 0..1 (seberapa kuat koreksi)
     * - boost: 0..1 (intensitas finishing Lab)
     */
    fun processFrame(
        srcBitmap: Bitmap,
        type: Type,
        severity: Double,
        boost: Double,
        useLabFinishing: Boolean
    ): Bitmap {
        if (type == Type.NORMAL || severity <= 0.0) {
            return srcBitmap
        }

        val w = srcBitmap.width
        val h = srcBitmap.height

        // Bitmap (RGBA) -> Mat
        val rgba = Mat()
        Utils.bitmapToMat(srcBitmap, rgba) // 8UC4

        // RGBA -> RGB (8UC3)
        val rgb8u = Mat()
        Imgproc.cvtColor(rgba, rgb8u, Imgproc.COLOR_RGBA2RGB)
        rgba.release()

        // Ke float 0..1
        val rgb = Mat()
        rgb8u.convertTo(rgb, CvType.CV_32FC3, 1.0 / 255.0)
        rgb8u.release()

        // ----- RGB -> LMS -----
        val lms = Mat(rgb.rows(), rgb.cols(), CvType.CV_32FC3)
        Core.transform(rgb, lms, RGB2LMS)

        // ----- Simulasikan buta warna -----
        val simMat = if (type == Type.PROTAN) PROTAN_SIM else DEUTAN_SIM
        val lmsSim = Mat(lms.rows(), lms.cols(), lms.type())
        Core.transform(lms, lmsSim, simMat)

        // Error di domain LMS
        val lmsErr = Mat(lms.rows(), lms.cols(), lms.type())
        Core.subtract(lms, lmsSim, lmsErr)

        // ----- Kompensasi error -----
        // Split ke 3 channel (sekarang aman: 3-channel image, bukan Nx3 1-channel!)
        val lmsCh = ArrayList<Mat>(3)
        Core.split(lms, lmsCh)

        val errCh = ArrayList<Mat>(3)
        Core.split(lmsErr, errCh)

        val sev = severity.toFloat()

        when (type) {
            Type.PROTAN -> {
                // L channel error -> distribusikan ke M dan S
                val add = Mat()
                Core.multiply(errCh[0], Scalar.all(sev.toDouble()), add)
                Core.add(lmsCh[1], add, lmsCh[1]) // M += sev * eL
                Core.add(lmsCh[2], add, lmsCh[2]) // S += sev * eL
                add.release()
            }
            Type.DEUTAN -> {
                // M channel error -> distribusikan ke L dan S
                val add = Mat()
                Core.multiply(errCh[1], Scalar.all(sev.toDouble()), add)
                Core.add(lmsCh[0], add, lmsCh[0]) // L += sev * eM
                Core.add(lmsCh[2], add, lmsCh[2]) // S += sev * eM
                add.release()
            }
            else -> { /* NORMAL sudah di-return awal */ }
        }

        // Merge kembali hasil koreksi
        val lmsCorr = Mat(lms.rows(), lms.cols(), lms.type())
        Core.merge(lmsCh, lmsCorr)

        // ----- LMS -> RGB -----
        val rgbCorr = Mat(rgb.rows(), rgb.cols(), rgb.type())
        Core.transform(lmsCorr, rgbCorr, LMS2RGB)

        // Clamp ke [0,1] untuk aman
        Core.max(rgbCorr, Scalar(0.0, 0.0, 0.0), rgbCorr)
        Core.min(rgbCorr, Scalar(1.0, 1.0, 1.0), rgbCorr)

        // ----- (Opsional) Finishing di Lab -----
        val post = if (useLabFinishing && boost > 0.0) {
            val lab = Mat()
            Imgproc.cvtColor(rgbCorr, lab, Imgproc.COLOR_RGB2Lab)

            val labCh = ArrayList<Mat>(3)
            Core.split(lab, labCh)

            // Skala lembut: L sedikit, a/b sedikit lebih kuat
            val b = boost.toFloat() // 0..1
            val lScale = 1.0 + 0.15 * b
            val aScale = 1.0 + 0.25 * b
            val bScale = 1.0 + 0.25 * b

            Core.multiply(labCh[0], Scalar.all(lScale), labCh[0]) // L di range ~0..100
            Core.multiply(labCh[1], Scalar.all(aScale), labCh[1])
            Core.multiply(labCh[2], Scalar.all(bScale), labCh[2])

            Core.merge(labCh, lab)
            val rgbFin = Mat()
            Imgproc.cvtColor(lab, rgbFin, Imgproc.COLOR_Lab2RGB)
            lab.release()
            labCh.forEach { it.release() }
            rgbFin
        } else {
            rgbCorr
        }

        // Clamp lagi (konversi balik bisa overshoot)
        Core.max(post, Scalar(0.0, 0.0, 0.0), post)
        Core.min(post, Scalar(1.0, 1.0, 1.0), post)

        // Float 0..1 -> 8UC3
        val out8u3 = Mat()
        post.convertTo(out8u3, CvType.CV_8UC3, 255.0)

        // RGB -> RGBA untuk Bitmap ARGB_8888
        val out8u4 = Mat()
        Imgproc.cvtColor(out8u3, out8u4, Imgproc.COLOR_RGB2RGBA)

        val outBmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(out8u4, outBmp)

        // Cleanup
        rgb.release(); lms.release(); lmsSim.release(); lmsErr.release()
        lmsCh.forEach { it.release() }; errCh.forEach { it.release() }
        lmsCorr.release(); rgbCorr.release(); if (post !== rgbCorr) rgbCorr.release()
        out8u3.release(); out8u4.release()

        Log.d("RealtimeCC", "✅ frame corrected type=$type sev=$severity boost=$boost lab=$useLabFinishing")
        return outBmp
    }
}
