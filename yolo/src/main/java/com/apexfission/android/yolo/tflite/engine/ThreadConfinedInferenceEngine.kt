package com.apexfission.android.yolo.tflite.engine

import android.graphics.Bitmap

/** Confines native construction, operations and cleanup to an owned or shared physical thread. */
class ThreadConfinedInferenceEngine private constructor(
    private val resource: ThreadConfinedResource<InferenceEngine>,
) : InferenceEngine by resource.value {
    constructor(
        sharedDispatcher: EngineThreadDispatcher? = null,
        engineFactory: () -> InferenceEngine,
    ) : this(ThreadConfinedResource(sharedDispatcher, engineFactory))

    internal val engineThreadId: Long = resource.call({ -1L }) { Thread.currentThread().id }
    internal var lastInferenceThreadId: Long = -1L
        private set
    internal var closeThreadId: Long = -1L
        private set

    override fun runInference(bitmap: Bitmap): FloatArray = resource.call({ FloatArray(0) }) {
        lastInferenceThreadId = Thread.currentThread().id
        it.runInference(bitmap)
    }

    override val lastInferenceTimeMs: Long
        get() = resource.call({ 0L }) { it.lastInferenceTimeMs }

    override fun close() {
        resource.close()
        closeThreadId = resource.closeThreadId
    }
}
