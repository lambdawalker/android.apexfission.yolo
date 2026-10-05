# Tensor buffers and validation API

Advanced APIs; most apps should use the detector factories. `TensorMetadata` describes one
tensor. `TensorContractValidator.validate` returns a `ModelTensorContract` or throws
`IllegalArgumentException` for the unsupported cases in [model contract](../model-contract.md).
Shapes are mutable arrays: do not change them during validation. Metadata types expose
`org.tensorflow.lite.DataType`; consumers directly using these low-level types need a compatible
LiteRT compile dependency (the library currently declares LiteRT as implementation).

Prefer buffer companion `build(contract)` after validation rather than manually composing
inconsistent capacities. Both buffer classes reuse internal storage and are not concurrency-safe.
Input buffers are direct/native-order; pixel arrays must have exactly S*S entries. Filling reads
the top-left S*S region, RGB order, normalized by 255; no resize, alpha, letterbox, or BGR swap.
Use an exact-size software bitmap. Undersized images/buffers cause Android/buffer exceptions.

FLOAT32 buffers need 3*S*S*4 bytes; INT8 needs 3*S*S bytes. `quantizeToInt8` expects its float
already scaled: it adds zero point, truncates and clamps, not a complete quantization operation.
`fillBitmapToByteBuffer` performs normalization and scaling; use a finite positive scale.
`OutputTensorBuffers.extractFloats` rewinds the buffer, dequantizes when needed, and returns a
copy so later calls do not mutate earlier results. `numClasses` equals attributes minus four.
Do not mutate `floatArray`/`buffer` while conversion is running. Data-class generated methods
are available, but IntArray equality is not a deep-shape comparison.


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
// package com.apexfission.android.yolo.tflite.validation
// DataType = org.tensorflow.lite.DataType
data class TensorMetadata(
    val shape: IntArray, val dataType: DataType,
    val quantizationScale: Float, val quantizationZeroPoint: Int
)
data class ModelTensorContract(
    val inputImageWidth: Int,
    val inputElementCount: Int,
    val inputByteCount: Int,
    val isInputInt8: Boolean,
    val inputQuantizationScale: Float,
    val inputQuantizationZeroPoint: Int,
    val outputElementCount: Int,
    val outputByteCount: Int,
    val isOutputInt8: Boolean,
    val outputQuantizationScale: Float,
    val outputQuantizationZeroPoint: Int,
    val outputLayout: InferenceEngine.OutputLayout,
    val outputAttributes: Int,
    val outputBoxes: Int
)
object TensorContractValidator {
    fun validate(input: TensorMetadata, output: TensorMetadata): ModelTensorContract
}
// package com.apexfission.android.yolo.tflite.buffers
class InputTensorBuffers(
    val imageWidth: Int, val isInt8: Boolean, val scale: Float, val zeroPoint: Int,
    val buffer: ByteBuffer, val pixelBuffer: IntArray
) {
    companion object { fun build(contract: ModelTensorContract): InputTensorBuffers }
    fun fill(bitmap: Bitmap): Unit
}
class OutputTensorBuffers(
    val count: Int, val layout: InferenceEngine.OutputLayout, val attributes: Int,
    val boxes: Int, val isInt8: Boolean, val scale: Float, val zeroPoint: Int,
    val buffer: ByteBuffer, val floatArray: FloatArray
) {
    companion object { fun build(contract: ModelTensorContract): OutputTensorBuffers }
    val numClasses: Int
    fun extractFloats(): FloatArray
}
fun quantizeToInt8(v: Float, zeroPoint: Int): Byte
fun fillBitmapToFloatBuffer(bitmap: Bitmap, buf: ByteBuffer, pixelBuffer: IntArray, inputImageWidth: Int): Unit
fun fillBitmapToByteBuffer(
    bitmap: Bitmap, buf: ByteBuffer, pixelBuffer: IntArray, inputImageWidth: Int,
    inScale: Float, inZeroPoint: Int
): Unit
```

## Source

- [TensorContractValidator.kt](../../../yolo/src/main/java/com/apexfission/android/yolo/tflite/validation/TensorContractValidator.kt)
- [InputTensorBuffers.kt](../../../yolo/src/main/java/com/apexfission/android/yolo/tflite/buffers/InputTensorBuffers.kt)
- [OutputTensorBuffers.kt](../../../yolo/src/main/java/com/apexfission/android/yolo/tflite/buffers/OutputTensorBuffers.kt)
- [BitmapTensorConverter.kt](../../../yolo/src/main/java/com/apexfission/android/yolo/tflite/buffers/BitmapTensorConverter.kt)
