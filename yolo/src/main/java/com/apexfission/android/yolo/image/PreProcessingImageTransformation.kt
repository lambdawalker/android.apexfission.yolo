package com.apexfission.android.yolo.image

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Defines strategies for preparing the camera image input for the object detection model.
 *
 * Each option specifies how the image from the camera should be cropped and scaled before
 * inference. The choice of input shape affects the detection area (e.g., the full scene vs.
 * the center), which can in turn impact model performance and accuracy.
 */
@Immutable
sealed class PreProcessingImageTransformation {
    /**
     * Uses the full image from the camera sensor as input, without cropping.
     *
     * This is ideal for detecting objects anywhere in the camera's field of view. The image
     * is letterboxed before being fed to the model to prevent aspect ratio distortion.
     */
    data object FullImage : PreProcessingImageTransformation()

    /**
     * A square crop of the center of the image is used as input.
     *
     * This option crops the center of the image to a square shape. This is useful when the
     * objects of interest are expected to be in the center of the image.
     */
    data object CenterSquareCrop : PreProcessingImageTransformation()

    /**
     * A square crop of the image with a custom vertical offset ([top]).
     */
    data class SquareCrop(val top: Dp = 0.dp) : PreProcessingImageTransformation()

    /**
     * Uses the portion of the image visible in the camera preview as input (centered).
     *
     * This is useful for detecting objects only within the displayed area, as the camera sensor
     * often captures a larger image than what is shown on screen.
     */
    data object CenterVisibleImage : PreProcessingImageTransformation()

    /**
     * Uses the portion of the image visible in the camera preview as input with a vertical offset ([top]).
     */
    data class VisibleImage(val top: Dp = 0.dp) : PreProcessingImageTransformation()

    /**
     * A square crop of the center of the visible portion of the image is used as input.
     *
     * This option is useful when objects of interest are expected to be in the center of the
     * visible area of the camera preview.
     */
    data object CenterVisibleImageSquareCrop : PreProcessingImageTransformation()

    /**
     * A square crop of the visible portion of the image with a vertical offset ([top]).
     */
    data class VisibleImageSquareCrop(val top: Dp = 0.dp) : PreProcessingImageTransformation()
}
