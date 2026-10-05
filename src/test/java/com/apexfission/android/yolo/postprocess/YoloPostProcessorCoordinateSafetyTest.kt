package com.apexfission.android.yolo.postprocess

import android.graphics.Bitmap
import com.apexfission.android.math.models.ImageBox
import com.apexfission.android.yolo.engine.LetterboxResult
import com.apexfission.android.yolo.tflite.engine.InferenceEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock

class YoloPostProcessorCoordinateSafetyTest {

    private val postProcessor = YoloPostProcessor(
        outLayout = InferenceEngine.OutputLayout.ATTRS_X_BOXES,
        outBoxes = 1,
        outAttrs = 5,
        numClasses = 1,
        inputImageWidth = 100,
        scoreThreshold = 0.5f,
        iouThreshold = 0.45f,
        maxNmsCandidates = 10,
    )

    private val letterboxResult = LetterboxResult(
        bitmap = mock(Bitmap::class.java),
        scale = 1f,
        padX = 0f,
        padY = 25f,
        sourceWidth = 100,
        sourceHeight = 50,
    )

    @Test
    fun negativeCoordinatesAreClampedBeforeIntegerConversion() {
        val detections = postProcessor.process(
            output = output(cx = 0.1f, cy = 0.5f, width = 0.4f, height = 0.4f),
            letterboxResult = letterboxResult,
        )

        assertEquals(ImageBox.from2P(0, 5, 30, 45), detections.single().box)
    }

    @Test
    fun coordinatesBeyondSourceBoundsAreClampedToTheSourceImage() {
        val detections = postProcessor.process(
            output = output(cx = 0.9f, cy = 0.8f, width = 0.4f, height = 0.4f),
            letterboxResult = letterboxResult,
        )

        assertEquals(ImageBox.from2P(70, 35, 100, 50), detections.single().box)
    }

    @Test
    fun nonFiniteBoxValuesAreDiscarded() {
        val valid = floatArrayOf(0.5f, 0.5f, 0.4f, 0.4f, 0.9f)
        val invalidValues = listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)

        invalidValues.forEach { invalidCoordinate ->
            for (attributeIndex in 0..3) {
                val invalidOutput = valid.copyOf().also { it[attributeIndex] = invalidCoordinate }
                val detections = postProcessor.process(
                    output = invalidOutput,
                    letterboxResult = letterboxResult,
                )

                assertTrue(
                    "Expected attribute $attributeIndex value $invalidCoordinate to be discarded",
                    detections.isEmpty(),
                )
            }
        }
    }

    @Test
    fun nonPositiveDimensionsAreDiscarded() {
        listOf(0f, -0.1f).forEach { invalidDimension ->
            for (dimensionIndex in 2..3) {
                val invalidOutput = output(cx = 0.5f, cy = 0.5f, width = 0.4f, height = 0.4f)
                    .also { it[dimensionIndex] = invalidDimension }
                val detections = postProcessor.process(
                    output = invalidOutput,
                    letterboxResult = letterboxResult,
                )

                assertTrue(
                    "Expected dimension $dimensionIndex value $invalidDimension to be discarded",
                    detections.isEmpty(),
                )
            }
        }
    }

    @Test
    fun nonFiniteConfidenceIsDiscarded() {
        val detections = postProcessor.process(
            output = output(
                cx = 0.5f,
                cy = 0.5f,
                width = 0.4f,
                height = 0.4f,
                confidence = Float.POSITIVE_INFINITY,
            ),
            letterboxResult = letterboxResult,
        )

        assertTrue(detections.isEmpty())
    }

    @Test
    fun boxesOutsideTheSourceImageAreDiscardedAfterClamping() {
        val detections = postProcessor.process(
            output = output(cx = -0.3f, cy = 0.5f, width = 0.2f, height = 0.4f),
            letterboxResult = letterboxResult,
        )

        assertTrue(detections.isEmpty())
    }

    private fun output(
        cx: Float,
        cy: Float,
        width: Float,
        height: Float,
        confidence: Float = 0.9f,
    ): FloatArray = floatArrayOf(cx, cy, width, height, confidence)
}
