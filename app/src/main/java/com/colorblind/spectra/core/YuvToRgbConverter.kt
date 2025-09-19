package com.colorblind.spectra.core

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

class YuvToRgbConverter {

    @OptIn(ExperimentalGetImage::class)
    fun yuvToRgb(imageProxy: ImageProxy, output: Bitmap) {
        val image = imageProxy.image ?: return
        require(image.format == ImageFormat.YUV_420_888) { "Format bukan YUV_420_888" }

        val nv21 = yuv420888ToNv21(imageProxy)

        // NV21 -> JPEG -> Bitmap
        val yuv = YuvImage(nv21, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
        val out = ByteArrayOutputStream()
        yuv.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 100, out)
        val jpeg = out.toByteArray()

        val bmp = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)
        val scaled = if (bmp.width == output.width && bmp.height == output.height) bmp
        else Bitmap.createScaledBitmap(bmp, output.width, output.height, true)

        // Salin ke output
        val canvas = android.graphics.Canvas(output)
        canvas.drawBitmap(scaled, 0f, 0f, null)

        if (scaled !== bmp) scaled.recycle()
        bmp.recycle()
    }

    private fun yuv420888ToNv21(image: ImageProxy): ByteArray {
        val yBuffer: ByteBuffer = image.planes[0].buffer
        val uBuffer: ByteBuffer = image.planes[1].buffer
        val vBuffer: ByteBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        // Y
        yBuffer.get(nv21, 0, ySize)

        // NV21 = Y + VU interleaved
        val rowStride = image.planes[2].rowStride
        val pixelStride = image.planes[2].pixelStride
        var pos = ySize
        val w2 = image.width / 2
        val h2 = image.height / 2

        for (row in 0 until h2) {
            var vRowPos = row * rowStride
            var uRowPos = row * image.planes[1].rowStride
            for (col in 0 until w2) {
                nv21[pos++] = vBuffer.get(vRowPos + col * pixelStride)
                nv21[pos++] = uBuffer.get(uRowPos + col * image.planes[1].pixelStride)
            }
        }
        return nv21
    }
}
