package com.apexfission.android.yolo.demo

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.apexfission.android.yolo.engine.buildYoloDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DemoSmokeTest {
    @Test fun postProcessingSuppressesOverlappingCandidates() {
        val bitmap = Bitmap.createBitmap(640, 640, Bitmap.Config.ARGB_8888)
        try {
            val result = DemoExamples.postProcess(bitmap)
            assertEquals(2, result.size)
            assertEquals(0.9f, result.first().confidence, 0.001f)
            assertEquals(0.7f, result.last().confidence, 0.001f)
        } finally { bitmap.recycle() }
    }

    @Test fun bundledModelInitializesAndRunsOnCpu() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val bitmap = Bitmap.createBitmap(640, 640, Bitmap.Config.ARGB_8888)
        try {
            buildYoloDetector(context, "demo.tflite", 0.35f, 0.45f, false).use { detector ->
                val detections = detector.detect(bitmap)
                assertTrue(detections.all { it.confidence.isFinite() && it.box.x2 <= 640u && it.box.y2 <= 640u })
            }
        } finally { bitmap.recycle() }
    }
}
