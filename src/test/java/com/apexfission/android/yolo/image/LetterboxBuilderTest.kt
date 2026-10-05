package com.apexfission.android.yolo.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import com.apexfission.android.yolo.engine.YoloDetector
import java.lang.reflect.Modifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.kotlin.whenever

class LetterboxBuilderTest {
    @Test
    fun resultRecordsTheSourceImageBounds() {
        val output = bitmap(width = 640, height = 640)
        val source = bitmap(width = 320, height = 180)

        val result = builderThatCreates(output).build(source, 640)

        assertEquals(320, result.sourceWidth)
        assertEquals(180, result.sourceHeight)
    }

    @Test
    fun separateBuildersDoNotShareOutputBitmaps() {
        val firstOutput = bitmap(width = 640, height = 640)
        val secondOutput = bitmap(width = 640, height = 640)
        val firstBuilder = builderThatCreates(firstOutput)
        val secondBuilder = builderThatCreates(secondOutput)
        val source = bitmap(width = 320, height = 180)

        val firstResult = firstBuilder.build(source, 640)
        val secondResult = secondBuilder.build(source, 640)

        assertSame(firstOutput, firstResult.bitmap)
        assertSame(secondOutput, secondResult.bitmap)
        assertNotSame(firstResult.bitmap, secondResult.bitmap)
    }

    @Test
    fun changingTargetSizeRecreatesBitmapAndCanvasTogether() {
        val firstOutput = bitmap(width = 640, height = 640)
        val secondOutput = bitmap(width = 320, height = 320)
        val firstCanvas = mock(Canvas::class.java)
        val secondCanvas = mock(Canvas::class.java)
        val outputs = ArrayDeque(listOf(firstOutput, secondOutput))
        val canvases = ArrayDeque(listOf(firstCanvas, secondCanvas))
        val builder = LetterboxBuilder(
            bitmapFactory = { outputs.removeFirst() },
            canvasFactory = { canvases.removeFirst() },
        )
        val source = bitmap(width = 320, height = 180)

        assertSame(firstOutput, builder.build(source, 640).bitmap)
        assertSame(secondOutput, builder.build(source, 320).bitmap)

        verify(firstCanvas).setBitmap(null)
        verify(firstOutput).recycle()
        verify(secondCanvas).drawColor(Color.BLACK)
    }

    @Test
    fun closeReleasesOnlyTheOwningBuildersBitmapAndIsIdempotent() {
        val firstOutput = bitmap(width = 640, height = 640)
        val secondOutput = bitmap(width = 640, height = 640)
        val firstBuilder = builderThatCreates(firstOutput)
        val secondBuilder = builderThatCreates(secondOutput)
        val source = bitmap(width = 320, height = 180)
        firstBuilder.build(source, 640)
        secondBuilder.build(source, 640)

        firstBuilder.close()
        firstBuilder.close()

        verify(firstOutput, times(1)).recycle()
        verify(secondOutput, never()).recycle()
    }

    @Test
    fun yoloBitmapDetectionSerializesTheCompletePipeline() {
        val method = YoloDetector::class.java
            .getDeclaredMethod("detect", Bitmap::class.java)

        assertTrue(Modifier.isSynchronized(method.modifiers))
    }

    private fun builderThatCreates(output: Bitmap): LetterboxBuilder =
        LetterboxBuilder(
            bitmapFactory = { output },
            canvasFactory = { mock(Canvas::class.java) },
        )

    private fun bitmap(width: Int, height: Int): Bitmap =
        mock(Bitmap::class.java).also {
            whenever(it.width).thenReturn(width)
            whenever(it.height).thenReturn(height)
        }
}
