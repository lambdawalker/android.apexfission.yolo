package com.apexfission.android.yolo.tflite.buffers

import android.graphics.Bitmap
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class BitmapTensorConverterTest {

    @Test
    fun `quantizeToInt8 correctly quantizes and clamps values`() {
        assertEquals(0.toByte(), quantizeToInt8(0f, 0))
        assertEquals(10.toByte(), quantizeToInt8(10f, 0))
        assertEquals(15.toByte(), quantizeToInt8(10f, 5))
        assertEquals(127.toByte(), quantizeToInt8(200f, 0))
        assertEquals((-128).toByte(), quantizeToInt8(-200f, 0))
    }

    @Test
    fun `fillBitmapToFloatBuffer normalizes RGB pixels to FP32`() {
        val width = 2
        val pixelCount = width * width
        val pixelBuffer = IntArray(pixelCount)
        val byteBuffer = ByteBuffer.allocateDirect(pixelCount * 3 * Float.SIZE_BYTES).order(ByteOrder.nativeOrder())

        val bitmap = mock<Bitmap>()
        val mockPixels = intArrayOf(
            0xFFFF0000.toInt(), // Red
            0xFF00FF00.toInt(), // Green
            0xFF0000FF.toInt(), // Blue
            0xFFFFFFFF.toInt(), // White
        )

        doAnswer { invocation ->
            val pixelsArg = invocation.getArgument<IntArray>(0)
            System.arraycopy(mockPixels, 0, pixelsArg, 0, mockPixels.size)
            null
        }.whenever(bitmap).getPixels(any(), eq(0), eq(width), eq(0), eq(0), eq(width), eq(width))

        fillBitmapToFloatBuffer(
            bitmap = bitmap,
            buf = byteBuffer,
            pixelBuffer = pixelBuffer,
            inputImageWidth = width,
        )

        byteBuffer.rewind()
        val floatBuf = byteBuffer.asFloatBuffer()

        // Red pixel (R=1.0, G=0.0, B=0.0)
        assertEquals(1.0f, floatBuf.get(), 0.001f)
        assertEquals(0.0f, floatBuf.get(), 0.001f)
        assertEquals(0.0f, floatBuf.get(), 0.001f)

        // Green pixel (R=0.0, G=1.0, B=0.0)
        assertEquals(0.0f, floatBuf.get(), 0.001f)
        assertEquals(1.0f, floatBuf.get(), 0.001f)
        assertEquals(0.0f, floatBuf.get(), 0.001f)

        // Blue pixel (R=0.0, G=0.0, B=1.0)
        assertEquals(0.0f, floatBuf.get(), 0.001f)
        assertEquals(0.0f, floatBuf.get(), 0.001f)
        assertEquals(1.0f, floatBuf.get(), 0.001f)

        // White pixel (R=1.0, G=1.0, B=1.0)
        assertEquals(1.0f, floatBuf.get(), 0.001f)
        assertEquals(1.0f, floatBuf.get(), 0.001f)
        assertEquals(1.0f, floatBuf.get(), 0.001f)
    }

    @Test
    fun `fillBitmapToByteBuffer normalizes and quantizes RGB pixels to INT8`() {
        val width = 1
        val pixelCount = width * width
        val pixelBuffer = IntArray(pixelCount)
        val byteBuffer = ByteBuffer.allocateDirect(pixelCount * 3 * Byte.SIZE_BYTES).order(ByteOrder.nativeOrder())

        val bitmap = mock<Bitmap>()
        val mockPixels = intArrayOf(0xFF808080.toInt()) // Mid gray ~ 128

        doAnswer { invocation ->
            val pixelsArg = invocation.getArgument<IntArray>(0)
            System.arraycopy(mockPixels, 0, pixelsArg, 0, mockPixels.size)
            null
        }.whenever(bitmap).getPixels(any(), eq(0), eq(width), eq(0), eq(0), eq(width), eq(width))

        fillBitmapToByteBuffer(
            bitmap = bitmap,
            buf = byteBuffer,
            pixelBuffer = pixelBuffer,
            inputImageWidth = width,
            inScale = 1.0f / 128.0f,
            inZeroPoint = -128,
        )

        byteBuffer.rewind()
        val byteR = byteBuffer.get()
        val byteG = byteBuffer.get()
        val byteB = byteBuffer.get()

        assertEquals((-63).toByte(), byteR)
        assertEquals((-63).toByte(), byteG)
        assertEquals((-63).toByte(), byteB)
    }
}
