# Image transformation API

These synchronous helpers operate on Android bitmaps. Use positive valid dimensions and
software-backed input. `getCropRect` returns source-pixel bounds; `cropWithOffset` also returns
the source origin. `crop` drops offset metadata. Dp offsets use system display density and
vertical offsets clamp to valid bounds. Visible-image modes match the supplied canvas ratio;
they do not inspect a live CameraX preview or mirror images.

`FullImage` keeps the frame, center-square modes choose its middle, offset variants shift
vertically, and visible-square variants square-crop the aspect-matched area.
`centerCropSquare(maxSize)` limits width/height at the selected crop origin, rather than
recentering a smaller square. Crops and zero-degree rotation can return the input object;
check identity before disposal. `Bitmap.crop(ImageBox)` clamps edges and throws
`IllegalArgumentException` for empty/inverted bounds after clamping.

`ImageProxy.toUprightBitmap` rotates by CameraX rotation metadata and recycles intermediate
bitmaps; caller owns returned bitmap and still must close the proxy. Other bitmap helpers
do not recycle caller inputs. `LetterboxBuilder` is internal and not a consumer API.


Common referenced types: `android.content.Context`, `android.graphics.Bitmap`,
`androidx.camera.core.ImageProxy`, `androidx.compose.ui.unit.Dp`, `IntSize`, `dp`,
`java.nio.ByteBuffer`, `com.apexfission.android.math.models.ImageBox`,
`com.apexfission.android.yolo.engine.{Detection, LetterboxResult, NumThreads}`,
`com.apexfission.android.yolo.tflite.engine.{InferenceEngine, EngineThreadDispatcher}`,
and `com.apexfission.android.yolo.tflite.validation.ModelTensorContract`.
Brace lists here are shorthand for individual imports, not Kotlin import syntax.

## Public declarations

Signatures below omit method bodies and annotations; import each type from its indicated package.
Data classes also expose Kotlin-generated copy/component/equality methods.

```kotlin
// package com.apexfission.android.yolo.image
sealed class PreProcessingImageTransformation {
    data object FullImage : PreProcessingImageTransformation()
    data object CenterSquareCrop : PreProcessingImageTransformation()
    data class SquareCrop(val top: Dp = 0.dp) : PreProcessingImageTransformation()
    data object CenterVisibleImage : PreProcessingImageTransformation()
    data class VisibleImage(val top: Dp = 0.dp) : PreProcessingImageTransformation()
    data object CenterVisibleImageSquareCrop : PreProcessingImageTransformation()
    data class VisibleImageSquareCrop(val top: Dp = 0.dp) : PreProcessingImageTransformation()
}
fun rotateIfNeeded(bm: Bitmap, deg: Int): Bitmap
fun cropToAspectRatio(src: Bitmap, canvasWidth: Int, canvasHeight: Int, square: Boolean = false, top: Dp = 0.dp): Bitmap
fun centerCropSquare(src: Bitmap, top: Dp = 0.dp, maxSize: Int = Int.MAX_VALUE): Bitmap
data class CroppedResult(val bitmap: Bitmap, val xOffset: Int, val yOffset: Int)
fun getCropRect(
    imageMode: PreProcessingImageTransformation, srcWidth: Int, srcHeight: Int,
    canvasWidth: Int = srcWidth, canvasHeight: Int = srcHeight
): android.graphics.Rect
fun cropWithOffset(
    imageMode: PreProcessingImageTransformation, bitmap: Bitmap,
    canvasSize: IntSize = IntSize(bitmap.width, bitmap.height)
): CroppedResult
fun crop(
    imageMode: PreProcessingImageTransformation, bitmap: Bitmap,
    canvasSize: IntSize = IntSize(bitmap.width, bitmap.height)
): Bitmap
fun ImageProxy.toUprightBitmap(): Bitmap
fun Bitmap.crop(box: ImageBox): Bitmap
```

## Source

- [PreProcessingImageTransformation.kt](../../../yolo/src/main/java/com/apexfission/android/yolo/image/PreProcessingImageTransformation.kt)
- [ImageOperations.kt](../../../yolo/src/main/java/com/apexfission/android/yolo/image/ImageOperations.kt)
- [ImageProxyExt.kt](../../../yolo/src/main/java/com/apexfission/android/yolo/image/ImageProxyExt.kt)
