package com.apexfission.android.yolo.postprocess

import com.apexfission.android.math.models.ImageBox
import com.apexfission.android.yolo.engine.Detection
import org.junit.Assert.assertEquals
import org.junit.Test

class YoloNmsTest {

    @Test
    fun `suppresses overlapping boxes of the same class with lower confidence`() {
        val box1 = ImageBox.from2P(0, 0, 100, 100)
        val box2 = ImageBox.from2P(10, 10, 100, 100)
        val d1 = Detection(box1, 0.9f, 0)
        val d2 = Detection(box2, 0.7f, 0)

        val result = YoloNms.suppress(
            detections = arrayListOf(d1, d2),
            iouThreshold = 0.45f,
            maxNmsCandidates = 10
        )

        assertEquals(1, result.size)
        assertEquals(0.9f, result.single().confidence)
    }

    @Test
    fun `retains overlapping boxes of different classes`() {
        val box1 = ImageBox.from2P(0, 0, 100, 100)
        val box2 = ImageBox.from2P(10, 10, 100, 100)
        val d1 = Detection(box1, 0.9f, 0)
        val d2 = Detection(box2, 0.7f, 1)

        val result = YoloNms.suppress(
            detections = arrayListOf(d1, d2),
            iouThreshold = 0.45f,
            maxNmsCandidates = 10
        )

        assertEquals(2, result.size)
    }

    @Test
    fun `respects maxNmsCandidates cap`() {
        val detections = ArrayList((0 until 10).map { i ->
            Detection(ImageBox.from2P(i * 200, 0, (i + 1) * 200, 200), 0.9f - i * 0.01f, i)
        })

        val result = YoloNms.suppress(
            detections = detections,
            iouThreshold = 0.45f,
            maxNmsCandidates = 3
        )

        assertEquals(3, result.size)
    }
}
