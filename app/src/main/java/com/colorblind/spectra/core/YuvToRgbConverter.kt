package com.colorblind.spectra.core

import android.content.Context
import android.graphics.Bitmap
import android.media.Image
import androidx.camera.core.ImageProxy
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicYuvToRGB
import android.renderscript.Type
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage

class YuvToRgbConverter(context: Context) {

    private val rsYuv: RenderScript = RenderScript.create(context)
    private val scriptYuvToRgb: ScriptIntrinsicYuvToRGB =
        ScriptIntrinsicYuvToRGB.create(rsYuv, Element.U8_4(rsYuv))

    private var yuvByteArray: ByteArray? = null
    private var yuvType: Type? = null
    private var allocationIn: Allocation? = null
    private var allocationOut: Allocation? = null

    /**
     * Konversi dari format YUV ke RGB Bitmap
     */
    fun yuvToRgb(image: Image, output: Bitmap) {
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        // Alokasikan ulang jika ukuran buffer berubah
        if (yuvByteArray == null || yuvByteArray!!.size != ySize + uSize + vSize) {
            yuvByteArray = ByteArray(ySize + uSize + vSize)
        }

        // Susun data YUV (Y + V + U)
        yBuffer.get(yuvByteArray!!, 0, ySize)
        vBuffer.get(yuvByteArray!!, ySize, vSize)
        uBuffer.get(yuvByteArray!!, ySize + vSize, uSize)

        // Inisialisasi RenderScript jika belum ada
        if (yuvType == null) {
            val elemYuv = Element.U8(rsYuv)
            yuvType = Type.Builder(rsYuv, elemYuv)
                .setX(yuvByteArray!!.size)
                .create()

            allocationIn = Allocation.createTyped(rsYuv, yuvType)
            allocationOut = Allocation.createFromBitmap(rsYuv, output)
        }

        // Proses konversi
        allocationIn!!.copyFrom(yuvByteArray)
        scriptYuvToRgb.setInput(allocationIn)
        scriptYuvToRgb.forEach(allocationOut)
        allocationOut!!.copyTo(output)
    }

    /**
     * Overload untuk ImageProxy (CameraX)
     */
    @OptIn(ExperimentalGetImage::class)
    fun yuvToRgb(imageProxy: ImageProxy, output: Bitmap) {
        yuvToRgb(imageProxy.image!!, output)
    }
}
