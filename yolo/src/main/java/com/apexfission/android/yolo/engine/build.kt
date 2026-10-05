package com.apexfission.android.yolo.engine

import android.content.Context
import com.apexfission.android.yolo.tflite.engine.EngineThreadDispatcher

fun buildYoloDetector(
    context: Context,
    modelPath: String,
    scoreThreshold: Float,
    iouThreshold: Float,
    useGpu: Boolean,
    maxNmsCandidates: Int = 150,
    numThreads: NumThreads = NumThreads.Default,
    sharedDispatcher: EngineThreadDispatcher? = null
): Detector = buildThreadConfinedYoloDetector(
    context = context,
    modelPath = modelPath,
    scoreThreshold = scoreThreshold,
    iouThreshold = iouThreshold,
    useGpu = useGpu,
    maxNmsCandidates = maxNmsCandidates,
    numThreads = numThreads,
    sharedDispatcher = sharedDispatcher
)

fun buildThreadConfinedYoloDetector(
    context: Context,
    modelPath: String,
    scoreThreshold: Float,
    iouThreshold: Float,
    useGpu: Boolean,
    maxNmsCandidates: Int = 150,
    numThreads: NumThreads = NumThreads.Default,
    sharedDispatcher: EngineThreadDispatcher? = null
): Detector {
    return ThreadConfinedYoloDetector(sharedDispatcher) {
        YoloDetector(
            context = context,
            modelPath = modelPath,
            scoreThreshold = scoreThreshold,
            iouThreshold = iouThreshold,
            useGpu = useGpu,
            maxNmsCandidates = maxNmsCandidates,
            numThreads = numThreads,
            sharedDispatcher = sharedDispatcher
        )
    }
}
