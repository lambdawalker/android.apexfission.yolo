package com.apexfission.android.yolo.tflite.buffers

import android.graphics.Bitmap
import com.apexfission.android.yolo.tflite.validation.ModelTensorContract

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Encapsulates the input specifications, quantization parameters, and pre-allocated I/O buffers
 * required to prepare bitmap data for model inference.
 */
class InputTensorBuffers(
    val imageWidth: Int,
    val isInt8: Boolean,
    val scale: Float,
    val zeroPoint: Int,
    val buffer: ByteBuffer,
    val pixelBuffer: IntArray,
) {
    companion object {
        fun build(contract: ModelTensorContract): InputTensorBuffers = InputTensorBuffers(
            imageWidth = contract.inputImageWidth,
            isInt8 = contract.isInputInt8,
            scale = contract.inputQuantizationScale,
            zeroPoint = contract.inputQuantizationZeroPoint,
            buffer = ByteBuffer.allocateDirect(contract.inputByteCount).order(ByteOrder.nativeOrder()),
            pixelBuffer = IntArray(contract.inputElementCount / 3),
        )
    }

    /**
     * Fills [buffer] with normalized or quantized pixel data from [bitmap].
     */
    fun fill(bitmap: Bitmap) {
        if (isInt8) {
            fillBitmapToByteBuffer(
                bitmap = bitmap,
                buf = buffer,
                pixelBuffer = pixelBuffer,
                inputImageWidth = imageWidth,
                inScale = scale,
                inZeroPoint = zeroPoint,
            )
        } else {
            fillBitmapToFloatBuffer(
                bitmap = bitmap,
                buf = buffer,
                pixelBuffer = pixelBuffer,
                inputImageWidth = imageWidth,
            )
        }
    }
}
