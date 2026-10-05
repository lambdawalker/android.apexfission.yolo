package com.apexfission.android.yolo.postprocess

import com.apexfission.android.math.models.ImageBox
import com.apexfission.android.yolo.engine.Detection
import com.apexfission.android.yolo.engine.LetterboxResult
import com.apexfission.android.yolo.tflite.engine.InferenceEngine


/**
 * Handles the complex task of decoding and post-processing the raw output from a YOLO TFLite model.
 *
 * This class is responsible for converting the model's raw tensor (a flat `FloatArray`) into a
 * meaningful and clean list of [Detection] objects.
 *
 * ### Key Operations:
 * 1.  **Decoding**: Iterates through the raw output, identifying candidate bounding boxes and their
 *     most likely class based on confidence scores.
 * 2.  **Coordinate Transformation**: Maps the coordinates from the model's input space (e.g., a 640x640
 *     letterboxed image) back to the coordinate space of the original, un-padded, and un-cropped image.
 * 3.  **Non-Max Suppression (NMS)**: Eliminates redundant, overlapping bounding boxes for the same object,
 *     keeping only the one with the highest confidence score.
 *
 * @param outLayout The memory layout of the model's output tensor, as determined by [InferenceEngine].
 * @param outBoxes The total number of candidate bounding boxes the model produces.
 * @param outAttrs The number of attributes per box (e.g., 4 coordinates + N class scores).
 * @param numClasses The number of distinct object categories the model can detect.
 * @param inputImageWidth The width of the square image fed into the model (e.g., 640).
 * @param scoreThreshold The minimum confidence score (from 0.0 to 1.0) required for a detection
 *                       to be considered valid. Anything below this is discarded.
 * @param iouThreshold The Intersection-over-Union (IoU) threshold (from 0.0 to 1.0) for NMS. If two
 *                     boxes of the same class have an IoU greater than this value, the one with the
 *                     lower score is suppressed.
 * @param maxNmsCandidates The absolute maximum number of detections to return after NMS, even if more
 *                      are available.
 * @param outputScalingMode Defines how the raw bounding box coordinates from the model's tensor
 *                          are interpreted—either as normalized values (0..1) or as direct pixel values.
 */
class YoloPostProcessor(
    private val outLayout: InferenceEngine.OutputLayout,
    private val outBoxes: Int,
    private val outAttrs: Int,
    private val numClasses: Int,
    private val inputImageWidth: Int,
    private val scoreThreshold: Float,
    private val iouThreshold: Float,
    private val maxNmsCandidates: Int,
    private val outputScalingMode: OutputScalingMode = OutputScalingMode.NORMALIZED
) {

    /** Defines how raw tensor coordinates are interpreted. */
    enum class OutputScalingMode {
        /** Assumes model outputs are raw pixel values (e.g., 0 to 640). No scaling is applied. */
        NONE,

        /** Assumes model outputs are normalized values (0.0 to 1.0) and scales them by [inputImageWidth]. */
        NORMALIZED
    }

    /**
     * Executes the entire post-processing pipeline on the raw output from the TFLite interpreter.
     *
     * @param output The flattened `FloatArray` directly from the TFLite model's output tensor.
     */
    fun process(
        output: FloatArray, letterboxResult: LetterboxResult
    ): List<Detection> {
        val rawDetections = decodeDetections(
            output = output,
            lbScale = letterboxResult.scale,
            padX = letterboxResult.padX,
            padY = letterboxResult.padY,
            sourceWidth = letterboxResult.sourceWidth,
            sourceHeight = letterboxResult.sourceHeight,
        )

        return YoloNms.suppress(
            detections = rawDetections,
            iouThreshold = iouThreshold,
            maxNmsCandidates = maxNmsCandidates,
        )
    }

    /** Decodes the raw model output, reversing the letterboxing transformation. */
    private fun decodeDetections(
        output: FloatArray,
        lbScale: Float,
        padX: Float,
        padY: Float,
        sourceWidth: Int,
        sourceHeight: Int,
    ): ArrayList<Detection> {
        val detections = ArrayList<Detection>(128)

        if (!lbScale.isFinite() || lbScale <= 0f || sourceWidth <= 0 || sourceHeight <= 0) {
            return detections
        }

        val isBoxesFirst = outLayout == InferenceEngine.OutputLayout.ATTRS_X_BOXES
        val strideBox = if (isBoxesFirst) 1 else outAttrs
        val strideAttr = if (isBoxesFirst) outBoxes else 1

        val modelInputImageWidth = if (outputScalingMode == OutputScalingMode.NONE) 1f else inputImageWidth.toFloat()

        for (i in 0 until outBoxes) {
            val bIdx = i * strideBox
            var maxClassScore = 0f
            var bestCls = -1

            // Find the class with the highest score for this box.
            for (c in 0 until numClasses) {
                val score = output[bIdx + (4 + c) * strideAttr]
                if (score > maxClassScore) {
                    maxClassScore = score
                    bestCls = c
                }
            }

            if (!maxClassScore.isFinite() || maxClassScore < scoreThreshold) continue

            // Extract coordinates.

            //centerPoint
            val cx = output[bIdx]
            val cy = output[bIdx + strideAttr]

            //size
            val w = output[bIdx + 2 * strideAttr]
            val h = output[bIdx + 3 * strideAttr]

            if (!cx.isFinite() || !cy.isFinite() || !w.isFinite() || !h.isFinite()) continue
            if (w <= 0f || h <= 0f) continue

            val halfW = (w * modelInputImageWidth) / 2f
            val halfH = (h * modelInputImageWidth) / 2f

            // Reverse the letterbox transformation: remove padding and apply inverse scale.
            val x1 = ((cx * modelInputImageWidth - halfW) - padX) / lbScale
            val y1 = ((cy * modelInputImageWidth - halfH) - padY) / lbScale
            val x2 = ((cx * modelInputImageWidth + halfW) - padX) / lbScale
            val y2 = ((cy * modelInputImageWidth + halfH) - padY) / lbScale

            if (!x1.isFinite() || !y1.isFinite() || !x2.isFinite() || !y2.isFinite()) continue

            val left = x1.coerceIn(0f, sourceWidth.toFloat()).toInt()
            val top = y1.coerceIn(0f, sourceHeight.toFloat()).toInt()
            val right = x2.coerceIn(0f, sourceWidth.toFloat()).toInt()
            val bottom = y2.coerceIn(0f, sourceHeight.toFloat()).toInt()

            if (right <= left || bottom <= top) continue

            // Convert only after validation and clamping so negative model values can
            // never wrap into large unsigned coordinates.
            detections += Detection(
                ImageBox.from2P(
                    x1 = left, y1 = top, x2 = right, y2 = bottom
                ), maxClassScore, bestCls
            )
        }

        return detections
    }
}
