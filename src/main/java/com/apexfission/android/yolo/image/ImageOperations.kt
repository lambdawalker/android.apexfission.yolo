package com.apexfission.android.yolo.image

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

import kotlin.math.roundToInt

/**
 * Rotates a [Bitmap] by a specified angle if necessary.
 *
 * If the provided degree is `0`, this function efficiently returns the original bitmap
 * without performing any operations or creating a new object.
 *
 * @param bm The source [Bitmap] to be rotated.
 * @param deg The angle of rotation in degrees (e.g., 90, 180, 270).
 * @return A new, rotated [Bitmap], or the original `bm` instance if `deg` is 0.
 */
fun rotateIfNeeded(bm: Bitmap, deg: Int): Bitmap {
    if (deg == 0) return bm
    val m = Matrix().apply { postRotate(deg.toFloat()) }
    return Bitmap.createBitmap(bm, 0, 0, bm.width, bm.height, m, true)
}

/**
 * Performs a center crop on a source bitmap to match a target aspect ratio.
 *
 * This function is useful for matching the aspect ratio of a camera sensor's output image
 * to the aspect ratio of the UI view it's displayed on. It calculates the largest
 * possible rectangle within the source image that has the target aspect ratio,
 * effectively cropping from the sides (if the image is too wide) or from the top and
 * bottom (if the image is too tall).
 *
 * @param src The source [Bitmap] to be cropped.
 * @param canvasWidth The width of the target area (e.g., a Composable's width).
 * @param canvasHeight The height of the target area (e.g., a Composable's height).
 * @param square If `true`, the resulting bitmap will be further cropped into the largest
 *               possible square from the center of the aspect-ratio-corrected rectangle.
 * @param top Optional vertical offset ([Dp]) to shift the crop area vertically.
 * @return A new [Bitmap] cropped to the target aspect ratio.
 */
fun cropToAspectRatio(src: Bitmap, canvasWidth: Int, canvasHeight: Int, square: Boolean = false, top: Dp = 0.dp): Bitmap {
    val rect = getAspectRatioRect(
        src.width,
        src.height,
        canvasWidth,
        canvasHeight,
        square,
        top,
        android.content.res.Resources.getSystem().displayMetrics.density
    )
    return Bitmap.createBitmap(src, rect.left, rect.top, rect.width(), rect.height())
}

/**
 * Crops the largest possible square from a source [Bitmap].
 *
 * The side length of the resulting square is determined by the smaller of the source
 * bitmap's width and height. An optional `maxSize` can be provided to limit the
 * output dimensions further.
 *
 * @param src The source [Bitmap].
 * @param top Optional vertical offset ([Dp]) to shift the crop area vertically.
 * @param maxSize An optional upper limit for the width and height of the resulting square.
 *                The final size will be `min(src.width, src.height, maxSize)`.
 * @return A new, square [Bitmap] cropped from the source.
 */
fun centerCropSquare(src: Bitmap, top: Dp = 0.dp, maxSize: Int = Int.MAX_VALUE): Bitmap {
    val size = minOf(src.width, src.height, maxSize)
    val rect = getCropRect(PreProcessingImageTransformation.SquareCrop(top), src.width, src.height)
    val w = minOf(rect.width(), size)
    val h = minOf(rect.height(), size)
    return Bitmap.createBitmap(src, rect.left, rect.top, w, h)
}

/**
 * Result container holding a cropped [Bitmap] and its offset coordinates in the source space.
 */
data class CroppedResult(
    val bitmap: Bitmap,
    val xOffset: Int,
    val yOffset: Int
)

fun getCropRect(
    imageMode: PreProcessingImageTransformation,
    srcWidth: Int,
    srcHeight: Int,
    canvasWidth: Int = srcWidth,
    canvasHeight: Int = srcHeight
): Rect {
    val density = android.content.res.Resources.getSystem().displayMetrics.density
    return when (imageMode) {
        is PreProcessingImageTransformation.FullImage -> {
            Rect(0, 0, srcWidth, srcHeight)
        }

        is PreProcessingImageTransformation.CenterSquareCrop -> {
            val size = minOf(srcWidth, srcHeight)
            val left = (srcWidth - size) / 2
            val top = (srcHeight - size) / 2
            Rect(left, top, left + size, top + size)
        }

        is PreProcessingImageTransformation.SquareCrop -> {
            val size = minOf(srcWidth, srcHeight)
            val left = (srcWidth - size) / 2
            val defaultTop = (srcHeight - size) / 2
            val topPx = (imageMode.top.value * density).roundToInt()
            val finalTop = (defaultTop + topPx).coerceIn(0, (srcHeight - size).coerceAtLeast(0))
            Rect(left, finalTop, left + size, finalTop + size)
        }

        is PreProcessingImageTransformation.CenterVisibleImage -> {
            getAspectRatioRect(srcWidth, srcHeight, canvasWidth, canvasHeight, false, 0.dp, density)
        }

        is PreProcessingImageTransformation.VisibleImage -> {
            getAspectRatioRect(srcWidth, srcHeight, canvasWidth, canvasHeight, false, imageMode.top, density)
        }

        is PreProcessingImageTransformation.CenterVisibleImageSquareCrop -> {
            getAspectRatioRect(srcWidth, srcHeight, canvasWidth, canvasHeight, true, 0.dp, density)
        }

        is PreProcessingImageTransformation.VisibleImageSquareCrop -> {
            getAspectRatioRect(srcWidth, srcHeight, canvasWidth, canvasHeight, true, imageMode.top, density)
        }
    }
}

private fun getAspectRatioRect(
    srcWidth: Int,
    srcHeight: Int,
    canvasWidth: Int,
    canvasHeight: Int,
    square: Boolean,
    top: Dp,
    density: Float
): Rect {
    val fSrcWidth = srcWidth.toFloat()
    val fSrcHeight = srcHeight.toFloat()
    val targetAspectRatio = if (canvasHeight > 0) canvasWidth.toFloat() / canvasHeight.toFloat() else 1f
    val srcAspectRatio = fSrcWidth / fSrcHeight

    var finalCropWidth = fSrcWidth
    var finalCropHeight = fSrcHeight

    if (srcAspectRatio > targetAspectRatio) {
        finalCropWidth = fSrcHeight * targetAspectRatio
    } else if (srcAspectRatio < targetAspectRatio) {
        finalCropWidth = fSrcWidth / targetAspectRatio
    }

    if (square) {
        val size = minOf(finalCropWidth, finalCropHeight)
        finalCropWidth = size
        finalCropHeight = size
    }

    val xOffset = ((fSrcWidth - finalCropWidth) / 2).roundToInt()
    val defaultYOffset = ((fSrcHeight - finalCropHeight) / 2).roundToInt()
    val topPx = (top.value * density).roundToInt()
    val yOffset = (defaultYOffset + topPx).coerceIn(0, (fSrcHeight - finalCropHeight).roundToInt().coerceAtLeast(0))
    val w = finalCropWidth.roundToInt().coerceAtMost(srcWidth - xOffset)
    val h = finalCropHeight.roundToInt().coerceAtMost(srcHeight - yOffset)

    return Rect(xOffset, yOffset, xOffset + w, yOffset + h)
}

/**
 * Crops a [Bitmap] according to the specified [PreProcessingImageTransformation] strategy, returning both the cropped bitmap and its offset.
 */
fun cropWithOffset(
    imageMode: PreProcessingImageTransformation,
    bitmap: Bitmap,
    canvasSize: IntSize = IntSize(bitmap.width, bitmap.height)
): CroppedResult {
    val rect = getCropRect(imageMode, bitmap.width, bitmap.height, canvasSize.width, canvasSize.height)
    val croppedBitmap = Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width(), rect.height())
    return CroppedResult(croppedBitmap, rect.left, rect.top)
}

/**
 * Crops a [Bitmap] according to the specified [PreProcessingImageTransformation] strategy.
 *
 * @param imageMode The cropping strategy ([PreProcessingImageTransformation.FullImage], [PreProcessingImageTransformation.SquareCrop], etc.).
 * @param bitmap The source image bitmap.
 * @param canvasSize Target composable dimensions for aspect ratio matching.
 * @return The cropped [Bitmap].
 */
fun crop(imageMode: PreProcessingImageTransformation, bitmap: Bitmap, canvasSize: IntSize = IntSize(bitmap.width, bitmap.height)): Bitmap {
    return cropWithOffset(imageMode, bitmap, canvasSize).bitmap
}
