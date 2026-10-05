package com.apexfission.android.yolo.tflite.engine

import android.graphics.Bitmap
import java.io.Closeable

/**
 * Custom exception thrown when the [InferenceEngine] fails to initialize.
 */
class TfliteInitializationException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Defines the contract for an object detection TFLite inference engine.
 */
interface InferenceEngine : Closeable {

    /** `true` if the model input uses INT8 quantization, `false` if it uses FP32. */
    val isInt8: Boolean

    /** The required width and height of the square input image for the model (e.g., 640). */
    val inputImageWidth: Int

    /** Represents the dimensional layout of the YOLO model's output tensor. */
    enum class OutputLayout {
        ATTRS_X_BOXES, BOXES_X_ATTRS
    }

    /** The detected output layout of the loaded model. */
    val outLayout: OutputLayout

    /** The number of attributes per detection. */
    val outAttrs: Int

    /** The total number of bounding boxes the model can predict. */
    val outBoxes: Int

    /** The number of distinct object classes the model can identify. */
    val numClasses: Int

    /** The execution time of the most recent [runInference] call in milliseconds. */
    val lastInferenceTimeMs: Long

    /**
     * Executes the TFLite model inference on the provided [bitmap].
     */
    fun runInference(bitmap: Bitmap): FloatArray
}
