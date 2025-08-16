package com.colorblind.spectra.core

import android.graphics.Bitmap
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.max
import kotlin.math.min

/**
 * Koreksi warna untuk Protanopia / Deuteranopia berbasis LMS + Daltonization.
 * Sumber matriks: varian umum Hunt-Pointer-Estevez (RGB↔LMS) + CTM prota/deuta (Viénot/Machado style).
 * Implementasi CPU murni; cukup cepat untuk foto ≤ 2–4MP.
 */
object ColorBlindCorrection {

    enum class Type { NORMAL, PROTAN, DEUTAN }

    /** 3x3 matrix + multiply helpers */
    private data class M3(val m: DoubleArray) {
        operator fun times(v: DoubleArray): DoubleArray = doubleArrayOf(
            m[0]*v[0] + m[1]*v[1] + m[2]*v[2],
            m[3]*v[0] + m[4]*v[1] + m[5]*v[2],
            m[6]*v[0] + m[7]*v[1] + m[8]*v[2]
        )
    }

    // sRGB <-> linear helpers
    private fun srgbToLinear(c: Double): Double =
        if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)

    private fun linearToSrgb(c: Double): Double =
        if (c <= 0.0031308) 12.92 * c else 1.055 * c.pow(1.0 / 2.4) - 0.055

    private fun clamp01(x: Double) = max(0.0, min(1.0, x))

    // --- Matriks konversi RGB (linear) <-> LMS (Hunt-Pointer-Estevez aproks.)
    private val M_RGB2LMS = M3(doubleArrayOf(
        17.8824, 43.5161, 4.11935,
        3.45565, 27.1554, 3.86714,
        0.0299566, 0.184309, 1.46709
    ))

    private val M_LMS2RGB = M3(doubleArrayOf(
        0.0809444479, -0.130504409, 0.116721066,
        -0.0102485335, 0.0540193266, -0.113614708,
        -0.0003652969, -0.0041216147, 0.693511405
    ))

    // --- Matriks simulasi buta warna
    private val SIM_PROTAN = M3(doubleArrayOf(
        0.0, 1.05118294, -0.05116099,
        0.0, 1.0, 0.0,
        0.0, 0.0, 1.0
    ))

    private val SIM_DEUTAN = M3(doubleArrayOf(
        1.0, 0.0, 0.0,
        0.9513092, 0.0, 0.04866992,
        0.0, 0.0, 1.0
    ))

    /**
     * Daltonize sebuah bitmap sesuai tipe (PROTAN/DEUTAN).
     * @param severity 0.0..1.0 keparahan defisiensi (1.0 = penuh)
     * @param boost 0.0..1.0 seberapa kuat kompensasi error ditambahkan
     */
    fun applyCorrection(
        src: Bitmap,
        type: Type,
        severity: Double = 1.0,
        boost: Double = 1.0
    ): Bitmap {
        if (type == Type.NORMAL) return src.copy(Bitmap.Config.ARGB_8888, false)

        val w = src.width
        val h = src.height
        val out = src.copy(Bitmap.Config.ARGB_8888, true)
        val pixels = IntArray(w * h)
        out.getPixels(pixels, 0, w, 0, 0, w, h)

        val simMat = when (type) {
            Type.PROTAN -> SIM_PROTAN
            Type.DEUTAN -> SIM_DEUTAN
            else -> SIM_DEUTAN
        }

        for (i in pixels.indices) {
            val p = pixels[i]
            val a = (p ushr 24) and 0xFF
            val r = (p ushr 16) and 0xFF
            val g = (p ushr 8) and 0xFF
            val b = p and 0xFF

            // sRGB 0..1
            val sr = r / 255.0
            val sg = g / 255.0
            val sb = b / 255.0

            // ke linear RGB
            val lr = srgbToLinear(sr)
            val lg = srgbToLinear(sg)
            val lb = srgbToLinear(sb)

            // ke LMS
            val lms = M_RGB2LMS * doubleArrayOf(lr, lg, lb)

            // simulasi defisiensi di LMS (lerp dengan severity)
            val lmsSimFull = simMat * lms
            val lmsSim = doubleArrayOf(
                lms[0] + (lmsSimFull[0] - lms[0]) * severity,
                lms[1] + (lmsSimFull[1] - lms[1]) * severity,
                lms[2] + (lmsSimFull[2] - lms[2]) * severity
            )

            // kembali ke RGB (linear) hasil simulasi
            val rgbSim = M_LMS2RGB * lmsSim

            // Error antara original dan simulasi (linear RGB)
            val err = doubleArrayOf(
                (lr - rgbSim[0]),
                (lg - rgbSim[1]),
                (lb - rgbSim[2])
            )

            // Tambahkan error ke kanal yang tersisa (kompensasi)
            val corr = doubleArrayOf(
                lr + boost * err[0],
                lg + boost * err[1],
                lb + boost * err[2]
            )

            // back to sRGB + clamp
            val rr = (clamp01(linearToSrgb(corr[0])) * 255.0).roundToInt()
            val gg = (clamp01(linearToSrgb(corr[1])) * 255.0).roundToInt()
            val bb = (clamp01(linearToSrgb(corr[2])) * 255.0).roundToInt()

            pixels[i] = (a shl 24) or (rr shl 16) or (gg shl 8) or bb
        }

        out.setPixels(pixels, 0, w, 0, 0, w, h)
        return out
    }
}
