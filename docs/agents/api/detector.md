# Detector API

Use `buildYoloDetector` for normal integration. The two factories have identical parameters
and return `Detector`. `modelPath` is an uncompressed asset path; score/IoU/useGpu are required,
while cap, threading, and shared dispatcher have defaults shown below. `detect` borrows a bitmap
or ImageProxy and returns source-pixel detections synchronously; the host closes proxies.
Construction can fail on asset access, tensor validation, or native initialization.
Close when finished; wrappers suppress work after closing. See [ownership](../concepts.md).

`YoloDetector` is public, but prefer a factory rather than direct construction. Thread-confined
wrappers accept custom factories; their factories execute on the selected physical worker.
`Detection` and `Feature` are data classes (generated copy/component/equality methods apply).
`LetterboxResult` carries the post-processing reverse transform and source dimensions; its
compatibility constructor infers dimensions, returning zero on invalid scale/padding.
`NumThreads.toInt()` floors percentage counts and clamps to one; custom counts are not CPU-capped.


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
// package com.apexfission.android.yolo.engine
interface Detector : java.io.Closeable {
    var enabled: Boolean
    fun detect(bitmap: android.graphics.Bitmap): List<Detection>
    fun detect(imageProxy: androidx.camera.core.ImageProxy): List<Detection>
}
fun buildYoloDetector(
    context: Context, modelPath: String, scoreThreshold: Float, iouThreshold: Float,
    useGpu: Boolean, maxNmsCandidates: Int = 150, numThreads: NumThreads = NumThreads.Default,
    sharedDispatcher: EngineThreadDispatcher? = null
): Detector
fun buildThreadConfinedYoloDetector(
    context: Context, modelPath: String, scoreThreshold: Float, iouThreshold: Float,
    useGpu: Boolean, maxNmsCandidates: Int = 150, numThreads: NumThreads = NumThreads.Default,
    sharedDispatcher: EngineThreadDispatcher? = null
): Detector
class YoloDetector(
    context: Context, modelPath: String, scoreThreshold: Float, iouThreshold: Float,
    useGpu: Boolean, maxNmsCandidates: Int = 150, numThreads: NumThreads = NumThreads.Default,
    private val sharedDispatcher: EngineThreadDispatcher? = null
) : Detector // overrides enabled, both detect overloads, and close(): Unit
class ThreadConfinedYoloDetector : Detector {
    constructor(sharedDispatcher: EngineThreadDispatcher? = null, detectorFactory: () -> Detector)
    // Overrides enabled, both detect overloads, and close(): Unit.
}
data class Detection(val box: ImageBox, val confidence: Float, val classId: Int)
data class Feature(val box: ImageBox, val confidence: Float, val classId: Int)
data class LetterboxResult(
    val bitmap: Bitmap, val scale: Float, val padX: Float, val padY: Float,
    val sourceWidth: Int, val sourceHeight: Int
) {
    constructor(bitmap: Bitmap, scale: Float, padX: Float, padY: Float)
}
sealed class NumThreads {
    object Default : NumThreads()
    object Quarter : NumThreads()
    object Half : NumThreads()
    object ThreeQuarters : NumThreads()
    data class CustomPercentage(val percentage: Float) : NumThreads()
    data class CustomCount(val count: Int) : NumThreads()
    fun toInt(): Int
    override fun toString(): String
}
```

## Source

- [Detector.kt](../../../yolo/src/main/java/com/apexfission/android/yolo/engine/Detector.kt)
- [build.kt](../../../yolo/src/main/java/com/apexfission/android/yolo/engine/build.kt)
- [YoloDetector.kt](../../../yolo/src/main/java/com/apexfission/android/yolo/engine/YoloDetector.kt)
- [ThreadConfinedYoloDetector.kt](../../../yolo/src/main/java/com/apexfission/android/yolo/engine/ThreadConfinedYoloDetector.kt)
- [Detection.kt](../../../yolo/src/main/java/com/apexfission/android/yolo/engine/Detection.kt)
- [LetterboxResult.kt](../../../yolo/src/main/java/com/apexfission/android/yolo/engine/LetterboxResult.kt)
- [NumThreads.kt](../../../yolo/src/main/java/com/apexfission/android/yolo/engine/NumThreads.kt)
