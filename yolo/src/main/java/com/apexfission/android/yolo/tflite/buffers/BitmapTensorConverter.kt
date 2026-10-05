package com.apexfission.android.yolo.tflite.buffers

import android.graphics.Bitmap
import java.nio.ByteBuffer

/**
 * Applies the INT8 quantization formula to a single float value.
 */
fun quantizeToInt8(v: Float, zeroPoint: Int): Byte =
    (v + zeroPoint).toInt().coerceIn(-128, 127).toByte()

/**
 * Prepares bitmap data for an FP32 model by normalizing pixel values to the [0.0, 1.0] range
 * and writing them to the destination [buf].
 */
fun fillBitmapToFloatBuffer(
    bitmap: Bitmap,
    buf: ByteBuffer,
    pixelBuffer: IntArray,
    inputImageWidth: Int
) {
    buf.rewind()
    bitmap.getPixels(pixelBuffer, 0, inputImageWidth, 0, 0, inputImageWidth, inputImageWidth)
    val inv255 = 1.0f / 255.0f
    for (v in pixelBuffer) {
        buf.putFloat(((v shr 16) and 0xFF) * inv255) // R
        buf.putFloat(((v shr 8) and 0xFF) * inv255)  // G
        buf.putFloat((v and 0xFF) * inv255)         // B
    }
}

/**
 * Prepares bitmap data for an INT8 model by normalizing and then quantizing pixel values,
 * writing them to the destination [buf].
 */
fun fillBitmapToByteBuffer(
    bitmap: Bitmap,
    buf: ByteBuffer,
    pixelBuffer: IntArray,
    inputImageWidth: Int,
    inScale: Float,
    inZeroPoint: Int
) {
    buf.rewind()
    bitmap.getPixels(pixelBuffer, 0, inputImageWidth, 0, 0, inputImageWidth, inputImageWidth)
    val invScale = if (inScale != 0f) 1f / inScale else 1f
    val combinedMultiplier = (1.0f / 255.0f) * invScale
    for (v in pixelBuffer) {
        buf.put(quantizeToInt8(((v shr 16) and 0xFF) * combinedMultiplier, inZeroPoint))
        buf.put(quantizeToInt8(((v shr 8) and 0xFF) * combinedMultiplier, inZeroPoint))
        buf.put(quantizeToInt8((v and 0xFF) * combinedMultiplier, inZeroPoint))
    }
}
