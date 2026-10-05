package com.apexfission.android.yolo.engine

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy

import java.io.Closeable

/**
 * Defines the contract for a generic object detector that is lifecycle-aware.
 */
interface Detector : Closeable {
    /**
     * Controls the active state of the detector. When `false`, detection calls should
     * return empty results immediately.
     */
    var enabled: Boolean

    fun detect(bitmap: Bitmap): List<Detection>

    fun detect(imageProxy: ImageProxy): List<Detection>
}
