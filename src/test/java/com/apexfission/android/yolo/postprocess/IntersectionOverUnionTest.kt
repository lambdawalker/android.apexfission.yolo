package com.apexfission.android.yolo.postprocess

import com.apexfission.android.math.models.ImageBox
import com.apexfission.android.yolo.engine.Detection
import org.junit.Assert.assertEquals
import org.junit.Test

class IntersectionOverUnionTest {

    @Test
    fun testIdenticalBoxesReturnIoUOne() {
        val box = ImageBox.from2P(10U, 10U, 100U, 100U)
        val d1 = Detection(box = box, confidence = 0.9f, classId = 0)
        val d2 = Detection(box = box, confidence = 0.8f, classId = 0)

        val iou = intersectionOverUnion(d1, d2)
        assertEquals(1.0, iou, 0.0001)
    }

    @Test
    fun testNonOverlappingBoxesReturnIoUZero() {
        val box1 = ImageBox.from2P(0U, 0U, 50U, 50U)
        val box2 = ImageBox.from2P(100U, 100U, 150U, 150U)
        val d1 = Detection(box = box1, confidence = 0.9f, classId = 0)
        val d2 = Detection(box = box2, confidence = 0.8f, classId = 0)

        val iou = intersectionOverUnion(d1, d2)
        assertEquals(0.0, iou, 0.0001)
    }

    @Test
    fun testPartialOverlapIoU() {
        val box1 = ImageBox.from2P(0U, 0U, 100U, 100U)
        val box2 = ImageBox.from2P(50U, 0U, 150U, 100U)
        val d1 = Detection(box = box1, confidence = 0.9f, classId = 0)
        val d2 = Detection(box = box2, confidence = 0.8f, classId = 0)

        val iou = intersectionOverUnion(d1, d2)
        assertEquals(0.3333333333333333, iou, 0.0001)
    }

    @Test
    fun testOneBoxInsideAnotherIoU() {
        val outerBox = ImageBox.from2P(0U, 0U, 100U, 100U)
        val innerBox = ImageBox.from2P(25U, 25U, 75U, 75U)
        val d1 = Detection(box = outerBox, confidence = 0.9f, classId = 0)
        val d2 = Detection(box = innerBox, confidence = 0.8f, classId = 0)

        val iou = intersectionOverUnion(d1, d2)
        assertEquals(0.25, iou, 0.0001)
    }

    @Test
    fun testTouchingEdgeBoxesReturnIoUZero() {
        val box1 = ImageBox.from2P(0U, 0U, 50U, 50U)
        val box2 = ImageBox.from2P(50U, 0U, 100U, 50U)
        val d1 = Detection(box = box1, confidence = 0.9f, classId = 0)
        val d2 = Detection(box = box2, confidence = 0.8f, classId = 0)

        val iou = intersectionOverUnion(d1, d2)
        assertEquals(0.0, iou, 0.0001)
    }
}
