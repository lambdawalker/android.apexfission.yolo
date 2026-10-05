# Inference and confinement API

Factories construct a validated engine on a physical worker and return `InferenceEngine`.
`runInference` takes a prepared square software bitmap and returns an independent flattened
float array, with INT8 output dequantized. It neither letterboxes nor runs NMS. Metadata
properties describe validated dimensions. `lastInferenceTimeMs` includes conversion, model
execution, and output extraction; it is not the photo demo's initialization-inclusive timer.

`ThreadConfinedInferenceEngine` returns an empty array and zero timing after close.
`EngineThreadDispatcher.isShutdown` reflects executor shutdown. Its public `close` initiates
shutdown; close wrappers first. `ThreadConfinedResource.call` serializes operations and uses
`ifClosed` once close is admitted; close waits for resource disposal. Initialization exceptions
propagate and owned dispatchers are shut down on factory failure. Accessing `value` directly
bypasses confinement. `closeThreadId` is read-only to consumers, initially -1.

Metadata getters delegated by wrappers are not arbitrary mutable operation dispatchers.
Use built-in factories and immutable metadata; custom factories must honor their own contracts.
See [model constraints](../model-contract.md) for errors and GPU limitations.


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
// package com.apexfission.android.yolo.tflite.engine
class TfliteInitializationException(message: String, cause: Throwable? = null) : Exception
interface InferenceEngine : java.io.Closeable {
    val isInt8: Boolean
    val inputImageWidth: Int
    enum class OutputLayout { ATTRS_X_BOXES, BOXES_X_ATTRS }
    val outLayout: OutputLayout
    val outAttrs: Int
    val outBoxes: Int
    val numClasses: Int
    val lastInferenceTimeMs: Long
    fun runInference(bitmap: Bitmap): FloatArray
}
fun buildInferenceEngine(
    context: Context, modelPath: String, useGpu: Boolean,
    numThreads: NumThreads = NumThreads.Default, sharedDispatcher: EngineThreadDispatcher? = null
): InferenceEngine
fun buildThreadConfinedInferenceEngine(
    context: Context, modelPath: String, useGpu: Boolean,
    numThreads: NumThreads = NumThreads.Default, sharedDispatcher: EngineThreadDispatcher? = null
): InferenceEngine
class ThreadConfinedInferenceEngine : InferenceEngine {
    constructor(sharedDispatcher: EngineThreadDispatcher? = null, engineFactory: () -> InferenceEngine)
    override fun runInference(bitmap: Bitmap): FloatArray
    override val lastInferenceTimeMs: Long
    override fun close(): Unit
    // Other InferenceEngine metadata is delegated.
}
class EngineThreadDispatcher : java.io.Closeable {
    val isShutdown: Boolean
    override fun close(): Unit
}
class ThreadConfinedResource<T : java.io.Closeable>(
    sharedContext: EngineThreadDispatcher? = null, factory: () -> T
) : java.io.Closeable {
    var closeThreadId: Long // private setter; read-only to consumers
    val value: T
    fun <R> call(ifClosed: () -> R, operation: (T) -> R): R
    override fun close(): Unit
}
```

## Source

- [InferenceEngine.kt](../../../yolo/src/main/java/com/apexfission/android/yolo/tflite/engine/InferenceEngine.kt)
- [build.kt](../../../yolo/src/main/java/com/apexfission/android/yolo/tflite/engine/build.kt)
- [ThreadConfinedInferenceEngine.kt](../../../yolo/src/main/java/com/apexfission/android/yolo/tflite/engine/ThreadConfinedInferenceEngine.kt)
- [EngineThreadDispatcher.kt](../../../yolo/src/main/java/com/apexfission/android/yolo/tflite/engine/EngineThreadDispatcher.kt)
- [ThreadConfinedResource.kt](../../../yolo/src/main/java/com/apexfission/android/yolo/tflite/engine/ThreadConfinedResource.kt)
