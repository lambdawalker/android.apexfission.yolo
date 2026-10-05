package com.apexfission.android.yolo.engine

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.ImageProxy


import com.apexfission.android.yolo.image.LetterboxBuilder
import com.apexfission.android.yolo.image.toUprightBitmap
import com.apexfission.android.yolo.postprocess.YoloPostProcessor
import com.apexfission.android.yolo.tflite.engine.EngineThreadDispatcher
import com.apexfission.android.yolo.tflite.engine.InferenceEngine
import com.apexfission.android.yolo.tflite.engine.buildThreadConfinedInferenceEngine
import java.util.concurrent.atomic.AtomicBoolean

class YoloDetector(
    context: Context,
    modelPath: String,
    scoreThreshold: Float,
    iouThreshold: Float,
    useGpu: Boolean,
    maxNmsCandidates: Int = 150,
    numThreads: NumThreads = NumThreads.Default,
    private val sharedDispatcher: EngineThreadDispatcher? = null,
) : Detector {
    private val _enabled = AtomicBoolean(true)
    override var enabled: Boolean
        get() = _enabled.get()
        set(value) {
            _enabled.set(value)
        }

    private val isClosed = AtomicBoolean(false)

    private val interpreter: InferenceEngine = buildThreadConfinedInferenceEngine(context, modelPath, useGpu, numThreads, sharedDispatcher)
    private val letterboxBuilder = LetterboxBuilder()

    private val postProcessor = YoloPostProcessor(
        interpreter.outLayout,
        interpreter.outBoxes,
        interpreter.outAttrs,
        interpreter.numClasses,
        interpreter.inputImageWidth,
        scoreThreshold,
        iouThreshold,
        maxNmsCandidates
    )

    // Dispatch before acquiring the monitor: nested shared-pipeline calls must never
    // wait for a monitor held by an external caller waiting for this same worker.
    private fun <T> onContext(block: () -> T): T =
        if (sharedDispatcher != null) sharedDispatcher.call(block) else block()

    @Synchronized
    override fun detect(bitmap: Bitmap): List<Detection> = onContext {
        synchronized(this) {
            if (!enabled || isClosed.get()) return@synchronized emptyList()

            val letterboxResult: LetterboxResult = letterboxBuilder.build(bitmap, interpreter.inputImageWidth)
            val output: FloatArray = interpreter.runInference(letterboxResult.bitmap)

            postProcessor.process(
                output = output,
                letterboxResult = letterboxResult
            )
        }
    }

    override fun detect(imageProxy: ImageProxy): List<Detection> {
        if (!enabled || isClosed.get()) return emptyList()
        val bitmap = imageProxy.toUprightBitmap()
        return try {
            detect(bitmap)
        } finally {
            bitmap.recycle()
        }
    }

    override fun close() = onContext {
        synchronized(this) {
            if (isClosed.getAndSet(true)) return@synchronized
            runCatching {
                interpreter.close()
            }

            runCatching {
                letterboxBuilder.close()
            }
            Unit
        }
    }
}
