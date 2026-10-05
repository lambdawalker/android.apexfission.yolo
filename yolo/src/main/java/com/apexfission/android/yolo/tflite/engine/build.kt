package com.apexfission.android.yolo.tflite.engine

import android.content.Context
import com.apexfission.android.yolo.engine.NumThreads


fun buildInferenceEngine(
    context: Context,
    modelPath: String,
    useGpu: Boolean,
    numThreads: NumThreads = NumThreads.Default,
    sharedDispatcher: EngineThreadDispatcher? = null
): InferenceEngine = buildThreadConfinedInferenceEngine(context, modelPath, useGpu, numThreads, sharedDispatcher)

fun buildThreadConfinedInferenceEngine(
    context: Context,
    modelPath: String,
    useGpu: Boolean,
    numThreads: NumThreads = NumThreads.Default,
    sharedDispatcher: EngineThreadDispatcher? = null
): InferenceEngine {
    return ThreadConfinedInferenceEngine(sharedDispatcher) {
        InferenceCore(context, modelPath, useGpu, numThreads)
    }
}
