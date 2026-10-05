# Post-processing API

Construct with matching output layout/dimensions, class count, input width, finite thresholds,
and a positive return cap. `OutputScalingMode.NORMALIZED` is the default; `NONE` means pixel
center/size coordinates. The detector does not expose this switch: use the lower-level pipeline.
`process` synchronously scans a flattened tensor, reverses letterboxing, clamps coordinates,
drops invalid/degenerate boxes, and returns confidence-sorted class-aware NMS results.
It does not take ownership of the bitmap in `LetterboxResult` and does not infer dimensions.

Nonfinite/zero scale or nonpositive source dimensions return empty results. The processor does
not check flattened array length or all constructor values; malformed metadata may throw
indexing/allocation errors. Highest strictly positive class score wins; keep score thresholds
positive to avoid zero-score/class -1 edge cases. Values are not calibrated or clamped scores.
NMS suppresses same-class boxes when IoU > threshold. The cap limits returned boxes.
`intersectionOverUnion` is a synchronous pure overlap ratio; disjoint/degenerate area returns zero.
`YoloNms` is internal. The [synthetic demo](../recipes.md#decode-a-tensor-without-running-a-model)
provides runnable coverage.


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
// package com.apexfission.android.yolo.postprocess
class YoloPostProcessor(
    private val outLayout: InferenceEngine.OutputLayout,
    private val outBoxes: Int,
    private val outAttrs: Int,
    private val numClasses: Int,
    private val inputImageWidth: Int,
    private val scoreThreshold: Float,
    private val iouThreshold: Float,
    private val maxNmsCandidates: Int,
    private val outputScalingMode: OutputScalingMode = OutputScalingMode.NORMALIZED
) {
    enum class OutputScalingMode { NONE, NORMALIZED }
    fun process(output: FloatArray, letterboxResult: LetterboxResult): List<Detection>
}
fun intersectionOverUnion(first: Detection, second: Detection): Double
```

## Source

- [YoloPostProcessor.kt](../../../yolo/src/main/java/com/apexfission/android/yolo/postprocess/YoloPostProcessor.kt)
- [intersectionOverUnion.kt](../../../yolo/src/main/java/com/apexfission/android/yolo/postprocess/intersectionOverUnion.kt)
