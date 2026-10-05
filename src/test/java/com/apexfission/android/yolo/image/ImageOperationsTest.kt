package com.apexfission.android.yolo.image

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class ImageOperationsTest {

    @Test
    fun testInputShapeOptions() {
        val centerCrop = PreProcessingImageTransformation.CenterSquareCrop
        val offsetCrop = PreProcessingImageTransformation.SquareCrop(top = 50.dp)
        assertEquals(50.dp, offsetCrop.top)
    }
}
