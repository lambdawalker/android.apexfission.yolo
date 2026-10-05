package com.apexfission.android.yolo.tflite.buffers


import com.apexfission.android.yolo.tflite.engine.InferenceEngine
import com.apexfission.android.yolo.tflite.validation.ModelTensorContract
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Encapsulates the output specifications, quantization parameters, and pre-allocated I/O buffers
 * required to extract raw float arrays from model inference.
 */
class OutputTensorBuffers(
    val count: Int,
    val layout: InferenceEngine.OutputLayout,
    val attributes: Int,
    val boxes: Int,
    val isInt8: Boolean,
    val scale: Float,
    val zeroPoint: Int,
    val buffer: ByteBuffer,
    val floatArray: FloatArray,
) {
    companion object {
        fun build(contract: ModelTensorContract): OutputTensorBuffers = OutputTensorBuffers(
            count = contract.outputElementCount,
            layout = contract.outputLayout,
            attributes = contract.outputAttributes,
            boxes = contract.outputBoxes,
            isInt8 = contract.isOutputInt8,
            scale = contract.outputQuantizationScale,
            zeroPoint = contract.outputQuantizationZeroPoint,
            buffer = ByteBuffer.allocateDirect(contract.outputByteCount).order(ByteOrder.nativeOrder()),
            floatArray = FloatArray(contract.outputElementCount),
        )
    }

    val numClasses: Int get() = attributes - 4

    /**
     * Extracts raw model outputs from [buffer] and de-quantizes them into an independent [FloatArray].
     */
    fun extractFloats(): FloatArray {
        buffer.rewind()
        if (isInt8) {
            for (i in 0 until count) {
                val q = buffer.get().toInt()
                floatArray[i] = (q - zeroPoint) * scale
            }
        } else {
            buffer.asFloatBuffer().get(floatArray)
        }
        return floatArray.copyOf()
    }
}
