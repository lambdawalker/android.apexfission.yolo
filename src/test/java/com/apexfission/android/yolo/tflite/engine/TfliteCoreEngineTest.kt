package com.apexfission.android.yolo.tflite.engine

import android.graphics.Bitmap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock

class TfliteCoreEngineTest {

    @Test
    fun `ThreadConfinedInferenceEngine delegates properties and calls on dedicated thread`() {
        var factoryCalled = false
        var inferenceCalled = false
        var closedCalled = false

        val mockEngine = object : InferenceEngine {
            override val isInt8: Boolean = false
            override val inputImageWidth: Int = 640
            override val outLayout: InferenceEngine.OutputLayout = InferenceEngine.OutputLayout.ATTRS_X_BOXES
            override val outAttrs: Int = 85
            override val outBoxes: Int = 8400
            override val numClasses: Int = 81
            override val lastInferenceTimeMs: Long = 12L

            override fun runInference(bitmap: Bitmap): FloatArray {
                inferenceCalled = true
                return floatArrayOf(1f, 2f)
            }

            override fun close() {
                closedCalled = true
            }
        }

        val wrapper = ThreadConfinedInferenceEngine {
            factoryCalled = true
            mockEngine
        }

        assertTrue("Factory must be invoked during wrapper init", factoryCalled)
        assertEquals(false, wrapper.isInt8)
        assertEquals(640, wrapper.inputImageWidth)
        assertEquals(InferenceEngine.OutputLayout.ATTRS_X_BOXES, wrapper.outLayout)
        assertEquals(85, wrapper.outAttrs)
        assertEquals(8400, wrapper.outBoxes)
        assertEquals(81, wrapper.numClasses)
        assertEquals(12L, wrapper.lastInferenceTimeMs)

        val bitmap = mock<Bitmap>()
        val result = wrapper.runInference(bitmap)

        assertTrue("Inference must be delegated to core engine", inferenceCalled)
        assertEquals(2, result.size)

        wrapper.close()
        assertTrue("Close must be delegated to core engine", closedCalled)
    }
}
