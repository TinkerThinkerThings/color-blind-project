package com.colorblind.spectra.utils

import kotlin.math.*

// Representasi warna dalam ruang CIELAB
data class LAB(val L: Double, val a: Double, val b: Double)

object ColorUtils {

    // =========================
    //  RGB -> XYZ -> LAB
    // =========================
    fun rgbToLab(r: Int, g: Int, b: Int): LAB {
        // Normalisasi [0,255] -> [0,1]
        var rNorm = r / 255.0
        var gNorm = g / 255.0
        var bNorm = b / 255.0

        // Koreksi gamma (sRGB)
        rNorm = if (rNorm > 0.04045) ((rNorm + 0.055) / 1.055).pow(2.4) else rNorm / 12.92
        gNorm = if (gNorm > 0.04045) ((gNorm + 0.055) / 1.055).pow(2.4) else gNorm / 12.92
        bNorm = if (bNorm > 0.04045) ((bNorm + 0.055) / 1.055).pow(2.4) else bNorm / 12.92

        // Convert ke XYZ (D65)
        val x = (rNorm * 0.4124 + gNorm * 0.3576 + bNorm * 0.1805) / 0.95047
        val y = (rNorm * 0.2126 + gNorm * 0.7152 + bNorm * 0.0722) / 1.00000
        val z = (rNorm * 0.0193 + gNorm * 0.1192 + bNorm * 0.9505) / 1.08883

        // Fungsi f(t)
        fun f(t: Double): Double {
            val delta = 6.0 / 29.0
            return if (t > delta * delta * delta) t.pow(1.0 / 3.0) else (t / (3 * delta * delta) + 4.0 / 29.0)
        }

        val fx = f(x)
        val fy = f(y)
        val fz = f(z)

        // Konversi ke LAB
        val L = 116 * fy - 16
        val a = 500 * (fx - fy)
        val bVal = 200 * (fy - fz)

        return LAB(L, a, bVal)
    }

    // =========================
    //  Delta E 2000
    // =========================
    fun deltaE2000(lab1: LAB, lab2: LAB): Double {
        val (L1, a1, b1) = lab1
        val (L2, a2, b2) = lab2

        val kL = 1.0
        val kC = 1.0
        val kH = 1.0

        val C1 = sqrt(a1 * a1 + b1 * b1)
        val C2 = sqrt(a2 * a2 + b2 * b2)
        val CBar = (C1 + C2) / 2.0

        val G = 0.5 * (1 - sqrt(CBar.pow(7.0) / (CBar.pow(7.0) + 25.0.pow(7.0))))
        val a1p = (1 + G) * a1
        val a2p = (1 + G) * a2

        val C1p = sqrt(a1p * a1p + b1 * b1)
        val C2p = sqrt(a2p * a2p + b2 * b2)

        val h1p = atan2(b1, a1p).let { if (it >= 0) it else it + 2 * Math.PI } * 180 / Math.PI
        val h2p = atan2(b2, a2p).let { if (it >= 0) it else it + 2 * Math.PI } * 180 / Math.PI

        val dLp = L2 - L1
        val dCp = C2p - C1p

        var dhp = h2p - h1p
        if (dhp > 180) dhp -= 360.0
        if (dhp < -180) dhp += 360.0
        val dHp = 2 * sqrt(C1p * C2p) * sin(Math.toRadians(dhp / 2))

        val LpBar = (L1 + L2) / 2.0
        val CpBar = (C1p + C2p) / 2.0

        var hpBar = (h1p + h2p) / 2.0
        if (abs(h1p - h2p) > 180) hpBar += 180.0
        if (hpBar >= 360) hpBar -= 360.0

        val T = 1 - 0.17 * cos(Math.toRadians(hpBar - 30)) +
                0.24 * cos(Math.toRadians(2 * hpBar)) +
                0.32 * cos(Math.toRadians(3 * hpBar + 6)) -
                0.20 * cos(Math.toRadians(4 * hpBar - 63))

        val dTheta = 30 * exp(-((hpBar - 275) / 25).pow(2.0))
        val Rc = 2 * sqrt(CpBar.pow(7.0) / (CpBar.pow(7.0) + 25.0.pow(7.0)))
        val Sl = 1 + (0.015 * (LpBar - 50).pow(2.0)) / sqrt(20 + (LpBar - 50).pow(2.0))
        val Sc = 1 + 0.045 * CpBar
        val Sh = 1 + 0.015 * CpBar * T
        val Rt = -sin(Math.toRadians(2 * dTheta)) * Rc

        return sqrt(
            (dLp / (kL * Sl)).pow(2.0) +
                    (dCp / (kC * Sc)).pow(2.0) +
                    (dHp / (kH * Sh)).pow(2.0) +
                    Rt * (dCp / (kC * Sc)) * (dHp / (kH * Sh))
        )
    }
}
