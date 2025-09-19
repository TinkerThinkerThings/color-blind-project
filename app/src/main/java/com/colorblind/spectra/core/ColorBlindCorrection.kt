package com.colorblind.spectra.core

import kotlin.math.pow
import org.opencv.core.CvType
import org.opencv.core.Mat

object ColorBlindCorrection {

    // --- Matriks RGB (linear) -> LMS (HPE / Machado) ---
    private val rgb2lms = arrayOf(
        doubleArrayOf(17.8824, 43.5161,  4.11935),
        doubleArrayOf( 3.45565, 27.1554, 3.86714),
        doubleArrayOf( 0.0299566, 0.184309, 1.46709)
    )

    // --- Matriks LMS -> RGB (linear) (inverse dari di atas) ---
    private val lms2rgb = arrayOf(
        doubleArrayOf( 0.0809444479, -0.1305044090,  0.1167210660),
        doubleArrayOf(-0.0102485335,  0.0540193266, -0.1136147080),
        doubleArrayOf(-0.0003652969, -0.0041216147,  0.6935114050)
    )

    // Matriks simulasi kebutaan (Machado/Viénot) – dipakai dasar hitung error
    private val protanSim = arrayOf(
        doubleArrayOf(0.0,      2.02344, -2.52581),
        doubleArrayOf(0.0,      1.0,      0.0),
        doubleArrayOf(0.0,      0.0,      1.0)
    )

    private val deutanSim = arrayOf(
        doubleArrayOf(1.0,      0.0,      0.0),
        doubleArrayOf(0.494207, 0.0,      1.24827),
        doubleArrayOf(0.0,      0.0,      1.0)
    )

    /**
     * KOREKSI (DALTONIZATION) untuk Protanopia/Deuteranopia.
     * 1) Simulasikan penglihatan buta warna.
     * 2) Hitung error (RGB_asli - RGB_sim).
     * 3) Suntikkan error ke kanal yang masih sehat.
     *
     * @param input   Mat 3-channel **RGB** (CV_8UC3 / CV_32FC3, sRGB).
     * @param type    "protan" atau "deutan".
     * @param amount  besaran injeksi error (0..1), default 0.7.
     * @return        Mat CV_8UC3 (RGB, sRGB) hasil koreksi.
     */
    fun daltonize(input: Mat, type: String, amount: Double = 0.7): Mat {
        require(input.channels() == 3) { "daltonize() membutuhkan Mat 3-channel (RGB)." }
        val k = amount.coerceIn(0.0, 1.0)
        val isProtan = type.equals("protan", true)
        val isDeutan = type.equals("deutan", true)

        var imgFloat = Mat()
        if (input.type() != CvType.CV_32FC3) {
            input.convertTo(imgFloat, CvType.CV_32FC3, 1.0 / 255.0)
        } else {
            imgFloat = input.clone()
        }

        val out = Mat(imgFloat.size(), imgFloat.type())

        for (y in 0 until imgFloat.rows()) {
            for (x in 0 until imgFloat.cols()) {
                val p = imgFloat.get(y, x) ?: continue

                // ===== ASUMSI INPUT RGB =====
                // sRGB (0..1)
                val r_s = p[0].toDouble()
                val g_s = p[1].toDouble()
                val b_s = p[2].toDouble()

                // sRGB -> linear
                val r = srgbToLinear(r_s)
                val g = srgbToLinear(g_s)
                val b = srgbToLinear(b_s)

                // --- ORIGINAL (linear) -> LMS
                val lms = mMul3(rgb2lms, r, g, b)

                // --- SIMULATED LMS (buta warna)
                val lmsSim = when {
                    isProtan -> mMul3(protanSim, lms[0], lms[1], lms[2])
                    isDeutan -> mMul3(deutanSim, lms[0], lms[1], lms[2])
                    else     -> doubleArrayOf(lms[0], lms[1], lms[2])
                }

                // --- SIMULATED back to RGB (linear)
                val rgbSim = mMul3(lms2rgb, lmsSim[0], lmsSim[1], lmsSim[2])

                // --- ERROR (linear RGB)
                val errR = (r - rgbSim[0])
                val errG = (g - rgbSim[1])
                val errB = (b - rgbSim[2])

                // --- DALTONIZATION: injeksi error ke kanal sehat
                var rc = r
                var gc = g
                var bc = b

                if (isProtan) {
                    // MERAH hilang → pindahkan info merah ke G dan B
                    gc = clamp01(gc + k * errR)
                    bc = clamp01(bc + k * errR)
                } else if (isDeutan) {
                    // HIJAU hilang → pindahkan info hijau ke R dan B
                    rc = clamp01(rc + k * errG)
                    bc = clamp01(bc + k * errG)
                }

                // linear -> sRGB
                val rOut = linearToSrgb(rc)
                val gOut = linearToSrgb(gc)
                val bOut = linearToSrgb(bc)

                out.put(y, x, floatArrayOf(rOut.toFloat(), gOut.toFloat(), bOut.toFloat()))
            }
        }

        val out8 = Mat()
        out.convertTo(out8, CvType.CV_8UC3, 255.0)
        return out8
    }

    // ================================
    // UTIL
    // ================================

    // Perkalian matriks 3x3 dengan vektor (x,y,z)
    private fun mMul3(m: Array<DoubleArray>, x: Double, y: Double, z: Double): DoubleArray {
        return doubleArrayOf(
            m[0][0]*x + m[0][1]*y + m[0][2]*z,
            m[1][0]*x + m[1][1]*y + m[1][2]*z,
            m[2][0]*x + m[2][1]*y + m[2][2]*z
        )
    }

    // sRGB <-> linear (nilai dalam 0..1)
    private fun srgbToLinear(v01: Double): Double {
        val v = v01.coerceIn(0.0, 1.0)
        return if (v <= 0.04045) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    }

    private fun linearToSrgb(v01: Double): Double {
        val v = v01.coerceIn(0.0, 1.0)
        return if (v <= 0.0031308) v * 12.92 else 1.055 * v.pow(1.0 / 2.4) - 0.055
    }

    private fun clamp01(x: Double): Double = when {
        x < 0.0 -> 0.0
        x > 1.0 -> 1.0
        else -> x
    }
}
