package com.apexfission.android.yolo.engine

import android.graphics.Bitmap
import kotlin.math.roundToInt

/**
 * Encapsulates the output of a letterboxing operation, providing both the resulting image
 * and the necessary metadata to reverse the transformation.
 *
 * Letterboxing is a common preprocessing step for object detection models that require a
 * fixed, square input size. It resizes an image to fit within the square while maintaining
 * its original aspect ratio, then fills the unused space (the "bars") with padding.
 *
 * The metadata (`scale`, `padX`, `padY`) is crucial for the post-processing step, where it's
 * used to accurately map the bounding box coordinates found in the letterboxed image back
 * to their correct locations in the original, pre-letterboxed image.
 *
 * @property bitmap The resulting square [Bitmap] after the letterbox transformation has been applied.
 *                  This is the image that is fed directly into the TFLite model.
 * @property scale The scaling factor that was applied to the source image to make it fit within
 *                 the letterbox dimensions. To reverse the scaling on a coordinate, you would
 *                 divide by this value.
 * @property padX The horizontal padding (in pixels) added to one side of the image to center it
 *                within the letterbox. This value must be subtracted from a coordinate when
 *                mapping it back to the original image space.
 * @property padY The vertical padding (in pixels) added to one side of the image to center it.
 * @property sourceWidth Width of the source image before letterboxing.
 * @property sourceHeight Height of the source image before letterboxing.
 */
data class LetterboxResult(
    val bitmap: Bitmap,
    val scale: Float,
    val padX: Float,
    val padY: Float,
    val sourceWidth: Int,
    val sourceHeight: Int,
) {
    /** Preserves the original four-argument API for callers constructing metadata manually. */
    constructor(bitmap: Bitmap, scale: Float, padX: Float, padY: Float) : this(
        bitmap = bitmap,
        scale = scale,
        padX = padX,
        padY = padY,
        sourceWidth = inferSourceDimension(bitmap.width, padX, scale),
        sourceHeight = inferSourceDimension(bitmap.height, padY, scale),
    )
}

private fun inferSourceDimension(letterboxSize: Int, padding: Float, scale: Float): Int {
    if (!padding.isFinite() || !scale.isFinite() || scale <= 0f) return 0
    return ((letterboxSize - 2f * padding) / scale).roundToInt().coerceAtLeast(0)
}
