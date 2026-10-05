package com.apexfission.android.yolo.demo

import android.graphics.Bitmap
import com.apexfission.android.yolo.engine.Detection
import com.apexfission.android.yolo.engine.LetterboxResult
import com.apexfission.android.yolo.postprocess.YoloPostProcessor
import com.apexfission.android.yolo.tflite.engine.InferenceEngine

/** Real public post-processing API with deterministic tensor data, not model inference. */
object DemoExamples {
    fun postProcess(bitmap: Bitmap): List<Detection> = YoloPostProcessor(
        outLayout = InferenceEngine.OutputLayout.BOXES_X_ATTRS,
        outBoxes = 3,
        outAttrs = 5,
        numClasses = 1,
        inputImageWidth = 640,
        scoreThreshold = 0.35f,
        iouThreshold = 0.45f,
        maxNmsCandidates = 150,
    ).process(
        floatArrayOf(
            0.30f, 0.30f, 0.40f, 0.25f, 0.90f,
            0.31f, 0.31f, 0.40f, 0.25f, 0.80f,
            0.80f, 0.80f, 0.20f, 0.20f, 0.70f,
        ),
        LetterboxResult(bitmap, 1f, 0f, 0f, 640, 640),
    )

    val classLabels = listOf(
        "Horizontal card", "Vertical card", "Card back", "Photo", "Slim barcode",
        "PDF417", "MRZ text", "Barcode", "QR code",
    )
}
