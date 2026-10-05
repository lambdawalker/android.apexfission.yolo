package com.apexfission.android.yolo.tflite.buffers

import com.apexfission.android.yolo.tflite.engine.InferenceEngine
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class TensorBuffersTest {

    @Test
    fun `OutputTensorBuffers extractFloats dequantizes INT8 values correctly`() {
        val count = 4
        val byteBuffer = ByteBuffer.allocateDirect(count * Byte.SIZE_BYTES).order(ByteOrder.nativeOrder())
        byteBuffer.put((-128).toByte())
        byteBuffer.put(0.toByte())
        byteBuffer.put(64.toByte())
        byteBuffer.put(127.toByte())

        val outputBuffers = OutputTensorBuffers(
            count = count,
            layout = InferenceEngine.OutputLayout.ATTRS_X_BOXES,
            attributes = 5,
            boxes = 1,
            isInt8 = true,
            scale = 0.5f,
            zeroPoint = -128,
            buffer = byteBuffer,
            floatArray = FloatArray(count),
        )

        assertEquals(1, outputBuffers.numClasses)

        val floats = outputBuffers.extractFloats()

        val expected = floatArrayOf(0.0f, 64.0f, 96.0f, 127.5f)
        assertArrayEquals(expected, floats, 0.001f)
    }

    @Test
    fun `OutputTensorBuffers extractFloats extracts FP32 values correctly`() {
        val count = 3
        val byteBuffer = ByteBuffer.allocateDirect(count * Float.SIZE_BYTES).order(ByteOrder.nativeOrder())
        val floatBuf = byteBuffer.asFloatBuffer()
        floatBuf.put(1.5f)
        floatBuf.put(2.5f)
        floatBuf.put(3.5f)

        val outputBuffers = OutputTensorBuffers(
            count = count,
            layout = InferenceEngine.OutputLayout.ATTRS_X_BOXES,
            attributes = 5,
            boxes = 1,
            isInt8 = false,
            scale = 0f,
            zeroPoint = 0,
            buffer = byteBuffer,
            floatArray = FloatArray(count),
        )

        val floats = outputBuffers.extractFloats()
        val expected = floatArrayOf(1.5f, 2.5f, 3.5f)
        assertArrayEquals(expected, floats, 0.001f)
    }
}
