package com.apexfission.android.yolo.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.graphics.createBitmap
import com.apexfission.android.yolo.engine.LetterboxResult
import java.io.Closeable
import kotlin.math.min

internal class LetterboxBuilder(
    private val bitmapFactory: (targetSize: Int) -> Bitmap = { targetSize ->
        createBitmap(targetSize, targetSize)
    },
    private val canvasFactory: (bitmap: Bitmap) -> Canvas = { bitmap -> Canvas(bitmap) },
) : Closeable {

    private var lbOut: Bitmap? = null
    private var lbCanvas: Canvas? = null
    private val lbPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val black = Color.BLACK

    @Synchronized
    fun build(src: Bitmap, targetSize: Int): LetterboxResult {
        val outputMatchesTarget = lbOut?.let {
            it.width == targetSize && it.height == targetSize
        } == true
        if (!outputMatchesTarget) {
            releaseCachedOutput()
            val newOutput = bitmapFactory(targetSize)
            val newCanvas = try {
                canvasFactory(newOutput)
            } catch (t: Throwable) {
                newOutput.recycle()
                throw t
            }
            lbOut = newOutput
            lbCanvas = newCanvas
        }

        val out = checkNotNull(lbOut)
        val canvas = checkNotNull(lbCanvas)
        val scale = min(targetSize / src.width.toFloat(), targetSize / src.height.toFloat())

        val newW = (src.width * scale).toInt()
        val newH = (src.height * scale).toInt()

        val padX = (targetSize - newW) / 2f
        val padY = (targetSize - newH) / 2f

        canvas.drawColor(black)
        canvas.drawBitmap(src, null, RectF(padX, padY, padX + newW, padY + newH), lbPaint)

        return LetterboxResult(
            bitmap = out,
            scale = scale,
            padX = padX,
            padY = padY,
            sourceWidth = src.width,
            sourceHeight = src.height,
        )
    }

    @Synchronized
    override fun close() {
        releaseCachedOutput()
    }

    private fun releaseCachedOutput() {
        lbCanvas?.setBitmap(null)
        lbOut?.let { bitmap ->
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
        lbOut = null
        lbCanvas = null
    }
}
