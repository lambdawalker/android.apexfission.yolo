package com.apexfission.android.yolo.engine

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy

import com.apexfission.android.yolo.tflite.engine.EngineThreadDispatcher
import com.apexfission.android.yolo.tflite.engine.ThreadConfinedResource

/** Confines native construction, operations and cleanup to an owned or shared physical thread. */
class ThreadConfinedYoloDetector private constructor(
    private val resource: ThreadConfinedResource<Detector>,
) : Detector by resource.value {
    constructor(
        sharedDispatcher: EngineThreadDispatcher? = null,
        detectorFactory: () -> Detector,
    ) : this(ThreadConfinedResource(sharedDispatcher, detectorFactory))

    internal val detectorThreadId: Long = resource.call({ -1L }) { Thread.currentThread().id }
    internal var lastDetectThreadId: Long = -1L
        private set
    internal var closeThreadId: Long = -1L
        private set

    override fun detect(bitmap: Bitmap): List<Detection> = resource.call({ emptyList() }) {
        lastDetectThreadId = Thread.currentThread().id
        it.detect(bitmap)
    }

    override fun detect(imageProxy: ImageProxy): List<Detection> = resource.call({ emptyList() }) {
        lastDetectThreadId = Thread.currentThread().id
        it.detect(imageProxy)
    }

    override var enabled: Boolean
        get() = resource.call({ false }) { it.enabled }
        set(value) {
            resource.call({ }) { it.enabled = value }
        }

    override fun close() {
        resource.close()
        closeThreadId = resource.closeThreadId
    }
}
