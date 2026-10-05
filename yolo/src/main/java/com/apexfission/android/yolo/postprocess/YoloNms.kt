package com.apexfission.android.yolo.postprocess

import com.apexfission.android.math.models.ImageBox
import com.apexfission.android.yolo.engine.Detection
import kotlin.math.max
import kotlin.math.min

/**
 * Handles Non-Max Suppression (NMS) for filtering overlapping candidate bounding boxes.
 */
internal object YoloNms {

    /**
     * Filters [detections] using Non-Max Suppression based on confidence scores and [iouThreshold].
     */
    fun suppress(
        detections: ArrayList<Detection>,
        iouThreshold: Float,
        maxNmsCandidates: Int
    ): List<Detection> {
        if (detections.isEmpty()) return emptyList()
        val sorted = detections.sortedByDescending { it.confidence }
        val size = sorted.size

        val areas = FloatArray(size) { i ->
            val box = sorted[i].box
            ((box.x2 - box.x) * (box.y2 - box.y)).toFloat()
        }

        val suppressed = BooleanArray(size)
        val keep = ArrayList<Detection>(min(size, maxNmsCandidates))

        for (i in 0 until size) {
            if (suppressed[i]) continue
            val best = sorted[i]
            keep.add(best)
            if (keep.size >= maxNmsCandidates) break

            for (j in i + 1 until size) {
                if (suppressed[j]) continue
                val next = sorted[j]

                val isSameClass = next.classId == best.classId
                if (!isSameClass) continue

                val iou = calculateIoU(best.box, areas[i], next.box, areas[j])
                if (iou > iouThreshold) {
                    suppressed[j] = true
                }
            }
        }

        return keep
    }

    /** Calculates the Intersection over Union of two bounding boxes using pre-computed areas. */
    private fun calculateIoU(a: ImageBox, areaA: Float, b: ImageBox, areaB: Float): Float {
        if (a.x > b.x2 || a.x2 < b.x || a.y > b.y2 || a.y2 < b.y) return 0f

        val interW = max(0u, min(a.x2, b.x2) - max(a.x, b.x)).toFloat()
        val interH = max(0u, min(a.y2, b.y2) - max(a.y, b.y)).toFloat()
        val inter = interW * interH
        return (inter / (areaA + areaB - inter + 1e-6f))
    }
}
