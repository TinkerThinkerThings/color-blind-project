package com.colorblind.spectra.core

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

/**
 * Realtime Color Correction untuk simulasi & koreksi buta warna.
 */
object RealtimeColorCorrection {

    enum class Type { NORMAL, PROTAN, DEUTAN }

    /** Matriks konversi RGB (linear) -> LMS */
    private fun rgb2lmsMat(): Mat = Mat(3, 3, CvType.CV_64F).apply {
        put(
            0, 0,
            17.8824, 43.5161, 4.11935,
            3.45565, 27.1554, 3.86714,
            0.0299566, 0.184309, 1.46709
        )
    }

    /** Matriks konversi LMS -> RGB (linear) */
    private fun lms2rgbMat(): Mat = Mat(3, 3, CvType.CV_64F).apply {
        put(
            0, 0,
            0.0809444479, -0.130504409, 0.116721066,
            -0.0102485335, 0.0540193266, -0.113614708,
            -0.0003652969, -0.0041216147, 0.693511405
        )
    }

    /** Simulasi Protanopia (buta merah) */
    private fun simProtan(): Mat = Mat(3, 3, CvType.CV_64F).apply {
        put(
            0, 0,
            0.0, 1.05118294, -0.05116099,
            0.0, 1.0, 0.0,
            0.0, 0.0, 1.0
        )
    }

    /** Simulasi Deuteranopia (buta hijau) */
    private fun simDeutan(): Mat = Mat(3, 3, CvType.CV_64F).apply {
        put(
            0, 0,
            1.0, 0.0, 0.0,
            0.9513092, 0.0, 0.04866992,
            0.0, 0.0, 1.0
        )
    }

    /** Helper Scalar dengan nilai sama untuk semua channel */
    private fun scalarAll(v: Double) = Scalar(v, v, v)

    /**
     * FUNGSI BARU: Menerapkan konversi sRGB -> Linear RGB ke Matriks
     * Rumus:
     * - if (s <= 0.04045) -> s / 12.92
     * - else -> ((s + 0.055) / 1.055) ^ 2.4
     * @param src Matriks input CV_64F dengan nilai 0.0..1.0
     * @return Matriks hasil konversi CV_64F
     */
    private fun applySrgbToLinear(src: Mat): Mat {
        // Matriks untuk menampung hasil akhir
        val dst = Mat(src.rows(), src.cols(), src.type())

        // Buat 'mask' untuk elemen yang <= 0.04045
        val mask = Mat()
        Core.compare(src, scalarAll(0.04045), mask, Core.CMP_LE) // LE = Less than or Equal

        // Buat mask yang dibalik (inverted)
        val invertedMask = Mat()
        Core.subtract(Mat(mask.size(), mask.type(), Scalar(255.0)), mask, invertedMask)

        // Hitung bagian 'else' -> pow(((s + 0.055) / 1.055), 2.4)
        val termElse = Mat()
        Core.add(src, scalarAll(0.055), termElse)
        Core.divide(termElse, scalarAll(1.055), termElse)
        Core.pow(termElse, 2.4, termElse)
        // Salin hasil 'else' ke dst menggunakan invertedMask
        termElse.copyTo(dst, invertedMask)

        // Hitung bagian 'if' -> s / 12.92
        val termIf = Mat()
        Core.divide(src, scalarAll(12.92), termIf)
        // Salin hasil 'if' ke dst hanya jika mask-nya 255 (memenuhi kondisi LE)
        termIf.copyTo(dst, mask)

        return dst
    }


    /**
     * FUNGSI BARU: Menerapkan konversi Linear RGB -> sRGB ke Matriks
     * Rumus:
     * - if (x <= 0.0031308) -> 12.92 * x
     * - else -> 1.055 * (x ^ (1/2.4)) - 0.055
     * @param src Matriks input CV_64F dengan nilai 0.0..1.0
     * @return Matriks hasil konversi CV_64F
     */
    private fun applyLinearToSrgb(src: Mat): Mat {
        // Matriks untuk menampung hasil akhir
        val dst = Mat(src.rows(), src.cols(), src.type())

        // Buat 'mask' untuk elemen yang <= 0.0031308
        val mask = Mat()
        Core.compare(src, scalarAll(0.0031308), mask, Core.CMP_LE)

        // Buat mask yang dibalik (inverted)
        val invertedMask = Mat()
        Core.subtract(Mat(mask.size(), mask.type(), Scalar(255.0)), mask, invertedMask)

        // Hitung bagian 'else' -> 1.055 * pow(x, 1.0 / 2.4) - 0.055
        val termElse = Mat()
        Core.pow(src, 1.0 / 2.4, termElse)
        Core.multiply(termElse, scalarAll(1.055), termElse)
        Core.subtract(termElse, scalarAll(0.055), termElse)
        // Salin hasil 'else' ke dst menggunakan invertedMask
        termElse.copyTo(dst, invertedMask)

        // Hitung bagian 'if' -> 12.92 * x
        val termIf = Mat()
        Core.multiply(src, scalarAll(12.92), termIf)
        // Salin hasil 'if' ke dst hanya jika mask-nya 255
        termIf.copyTo(dst, mask)

        return dst
    }

    /**
     * Proses 1 frame Bitmap → hasil koreksi
     * @param type Jenis simulasi (NORMAL, PROTAN, DEUTAN)
     * @param severity Intensitas simulasi (0..1)
     * @param boost Tingkat penguatan koreksi (0..1)
     * @param useLabFinishing Apakah pakai finishing LAB + CLAHE
     */
    fun processFrame(
        srcBitmap: Bitmap,
        type: Type,
        severity: Double,
        boost: Double,
        useLabFinishing: Boolean
    ): Bitmap {
        // Convert Bitmap → Mat (RGB)
        val rgb = Mat()
        Utils.bitmapToMat(srcBitmap, rgb)
        Imgproc.cvtColor(rgb, rgb, Imgproc.COLOR_RGBA2RGB)

        // Konversi ke float64
        val rgbF = Mat()
        rgb.convertTo(rgbF, CvType.CV_64F)

        // --- PERUBAHAN DIMULAI DI SINI ---

        // Normalisasi ke 0..1
        val normalized = Mat()
        Core.divide(rgbF, scalarAll(255.0), normalized)

        // sRGB → Linear RGB
        val lin = applySrgbToLinear(normalized)

        // --- AKHIR DARI PERUBAHAN BAGIAN 1 ---

        // Reshape jadi Nx3
        val rows = lin.rows()
        val cols = lin.cols()
        val lin3 = lin.reshape(1, rows * cols)

        // Linear RGB → LMS
        val Mrgb2lms = rgb2lmsMat()
        val lms = Mat()
        Core.gemm(lin3, Mrgb2lms.t(), 1.0, Mat(), 0.0, lms)

        // Simulasi defisiensi
        val simMat = when (type) {
            Type.PROTAN -> simProtan()
            Type.DEUTAN -> simDeutan()
            else -> Mat.eye(3, 3, CvType.CV_64F)
        }
        val lmsSimFull = Mat()
        Core.gemm(lms, simMat.t(), 1.0, Mat(), 0.0, lmsSimFull)

        // Campur hasil simulasi sesuai severity
        val lmsSim = Mat()
        Core.addWeighted(lms, 1.0 - severity, lmsSimFull, severity, 0.0, lmsSim)

        // LMS → Linear RGB
        val Mlms2rgb = lms2rgbMat()
        val rgbSim = Mat()
        Core.gemm(lmsSim, Mlms2rgb.t(), 1.0, Mat(), 0.0, rgbSim)

        // Hitung error (selisih)
        val err = Mat()
        Core.subtract(lin3, rgbSim, err)

        // Tambahkan error sesuai boost → hasil koreksi
        val corr = Mat()
        Core.addWeighted(lin3, 1.0, err, boost, 0.0, corr)

        // Reshape kembali ke citra
        val corrImg = corr.reshape(3, rows)

        // --- PERUBAHAN DIMULAI DI SINI ---

        // Pastikan nilai berada di rentang 0.0 - 1.0 sebelum konversi balik
        Core.min(corrImg, scalarAll(1.0), corrImg)
        Core.max(corrImg, scalarAll(0.0), corrImg)

        // Linear RGB → sRGB
        val srgbNonNormalized = applyLinearToSrgb(corrImg)

        // Skala kembali ke 0..255
        val srgb = Mat()
        Core.multiply(srgbNonNormalized, scalarAll(255.0), srgb)

        // --- AKHIR DARI PERUBAHAN BAGIAN 2 ---

        // Convert ke 8-bit
        val srgb8 = Mat()
        srgb.convertTo(srgb8, CvType.CV_8UC3)

        var out = Mat()
        if (useLabFinishing) {
            // Finishing pakai LAB + CLAHE
            Imgproc.cvtColor(srgb8, out, Imgproc.COLOR_RGB2Lab)
            val lab = ArrayList<Mat>(3)
            Core.split(out, lab)

            val clahe = Imgproc.createCLAHE(2.0, Size(8.0, 8.0))
            clahe.apply(lab[0], lab[0])

            Core.merge(lab, out)
            Imgproc.cvtColor(out, out, Imgproc.COLOR_Lab2RGB)
        } else {
            out = srgb8
        }

        // Convert ke RGBA untuk Bitmap
        Imgproc.cvtColor(out, out, Imgproc.COLOR_RGB2RGBA)

        // Buat Bitmap hasil
        val bmpOut = Bitmap.createBitmap(cols, rows, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(out, bmpOut)

        return bmpOut
    }
}