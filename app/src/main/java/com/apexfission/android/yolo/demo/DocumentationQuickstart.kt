package com.apexfission.android.yolo.demo

import android.content.Context
import android.graphics.Bitmap
import com.apexfission.android.yolo.engine.Detection
import com.apexfission.android.yolo.engine.buildYoloDetector

/** Blocking one-image example. The caller owns [bitmap] and must use a worker thread. */
fun detectImage(context: Context, bitmap: Bitmap, modelAssetPath: String): List<Detection> =
    buildYoloDetector(
        context = context.applicationContext,
        modelPath = modelAssetPath,
        scoreThreshold = 0.35f,
        iouThreshold = 0.45f,
        useGpu = false,
    ).use { detector -> detector.detect(bitmap) }
