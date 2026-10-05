package com.apexfission.android.yolo.image

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import com.apexfission.android.math.models.ImageBox

/**
 * Converts a CameraX [ImageProxy] into a correctly oriented [Bitmap].
 *
 * This extension function simplifies a common two-step process in camera applications:
 * 1.  It first calls the standard `imageProxy.toBitmap()` method to convert the raw
 *     image buffer (often in a YUV format) into an initial `Bitmap`.
 * 2.  It then reads the rotation metadata from `imageProxy.imageInfo.rotationDegrees`
 *     and applies the necessary rotation to produce a final, "upright" bitmap that
 *     matches the user's perspective.
 *
 * This is crucial for ensuring that image processing and model inference are performed
 * on an image that has the correct orientation.
 *
 * @receiver The [ImageProxy] instance, typically from a CameraX analysis use case.
 * @return A new [Bitmap] instance that is properly rotated to be upright.
 */
fun ImageProxy.toUprightBitmap(): Bitmap {
    val bitmap = this.toBitmap()
    try {
        val uprightBitmap = rotateIfNeeded(bitmap, this.imageInfo.rotationDegrees)
        if (uprightBitmap !== bitmap) {
            bitmap.recycle()
        }
        return uprightBitmap
    } catch (t: Throwable) {
        bitmap.recycle()
        throw t
    }
}

fun Bitmap.crop(box: ImageBox): Bitmap {
    val left = box.x.toInt().coerceIn(0, width)
    val top = box.y.toInt().coerceIn(0, height)
    val right = box.x2.toInt().coerceIn(0, width)
    val bottom = box.y2.toInt().coerceIn(0, height)

    require(right > left) {
        "Invalid crop box: x2 (${box.x2}) must be greater than x (${box.x})"
    }
    require(bottom > top) {
        "Invalid crop box: y2 (${box.y2}) must be greater than y (${box.y})"
    }

    return Bitmap.createBitmap(
        this,
        left,
        top,
        right - left,
        bottom - top
    )
}
