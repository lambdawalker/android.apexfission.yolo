package com.apexfission.android.yolo.tflite.engine

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import com.apexfission.android.math.models.ImageBox
import com.apexfission.android.yolo.engine.Detector
import com.apexfission.android.yolo.engine.Detection
import com.apexfission.android.yolo.engine.ThreadConfinedYoloDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock

class SharedEngineDispatcherTest {

    @Test
    fun `shared EngineThreadDispatcher executes pipeline wrappers on the exact same thread`() {
        val sharedEngineDispatcher = EngineThreadDispatcher()

        val mockInferenceEngine = object : InferenceEngine {
            override val isInt8: Boolean = false
            override val inputImageWidth: Int = 640
            override val outLayout: InferenceEngine.OutputLayout = InferenceEngine.OutputLayout.ATTRS_X_BOXES
            override val outAttrs: Int = 85
            override val outBoxes: Int = 8400
            override val numClasses: Int = 81
            override val lastInferenceTimeMs: Long = 10L

            override fun runInference(bitmap: Bitmap): FloatArray = floatArrayOf()
            override fun close() {}
        }

        val mockDetector = object : Detector {
            override var enabled: Boolean = true
            override fun detect(bitmap: Bitmap): List<Detection> = emptyList()
            override fun detect(imageProxy: ImageProxy): List<Detection> = emptyList()
            override fun close() {}
        }

        val inferenceEngineWrapper = ThreadConfinedInferenceEngine(sharedEngineDispatcher) {
            mockInferenceEngine
        }

        val yoloDetectorWrapper = ThreadConfinedYoloDetector(sharedEngineDispatcher) {
            mockDetector
        }

        // Verify wrappers initialized on the exact same worker thread
        val engineThreadId = inferenceEngineWrapper.engineThreadId
        val yoloThreadId = yoloDetectorWrapper.detectorThreadId

        assertEquals("InferenceEngine and YoloDetector must share thread ID", engineThreadId, yoloThreadId)

        val bitmap = mock<Bitmap>()
        inferenceEngineWrapper.runInference(bitmap)
        yoloDetectorWrapper.detect(bitmap)

        assertEquals("Inference runInference thread", engineThreadId, inferenceEngineWrapper.lastInferenceThreadId)
        assertEquals("Yolo detect thread", engineThreadId, yoloDetectorWrapper.lastDetectThreadId)

        yoloDetectorWrapper.close()
        inferenceEngineWrapper.close()

        sharedEngineDispatcher.close()
        assertTrue("Shared executor should be shut down after close()", sharedEngineDispatcher.isShutdown)
    }
}
