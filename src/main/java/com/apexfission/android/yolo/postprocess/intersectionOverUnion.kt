package com.apexfission.android.yolo.postprocess

import com.apexfission.android.yolo.engine.Detection


/**
 * Calculates the Intersection over Union (IoU) ratio between two [Detection] bounding boxes.
 *
 * @param first The first detection candidate.
 * @param second The second detection candidate.
 * @return The IoU score ranging from 0.0 (no overlap) to 1.0 (identical boxes).
 */
fun intersectionOverUnion(
    first: Detection,
    second: Detection
): Double {
    val firstLeft = first.box.x.toDouble()
    val firstTop = first.box.y.toDouble()
    val firstRight = first.box.x2.toDouble()
    val firstBottom = first.box.y2.toDouble()

    val secondLeft = second.box.x.toDouble()
    val secondTop = second.box.y.toDouble()
    val secondRight = second.box.x2.toDouble()
    val secondBottom = second.box.y2.toDouble()

    val intersectionLeft = maxOf(firstLeft, secondLeft)
    val intersectionTop = maxOf(firstTop, secondTop)
    val intersectionRight = minOf(firstRight, secondRight)
    val intersectionBottom = minOf(firstBottom, secondBottom)

    val intersectionWidth = (intersectionRight - intersectionLeft).coerceAtLeast(0.0)
    val intersectionHeight = (intersectionBottom - intersectionTop).coerceAtLeast(0.0)

    val intersectionArea = intersectionWidth * intersectionHeight
    if (intersectionArea <= 0.0) {
        return 0.0
    }

    val firstWidth = (firstRight - firstLeft).coerceAtLeast(0.0)
    val firstHeight = (firstBottom - firstTop).coerceAtLeast(0.0)

    val secondWidth = (secondRight - secondLeft).coerceAtLeast(0.0)
    val secondHeight = (secondBottom - secondTop).coerceAtLeast(0.0)

    val firstArea = firstWidth * firstHeight
    val secondArea = secondWidth * secondHeight

    val unionArea = firstArea + secondArea - intersectionArea
    if (unionArea <= 0.0) {
        return 0.0
    }

    return intersectionArea / unionArea
}
