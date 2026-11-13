# Source declaration reference

Generated from the current development sources. See [API contracts](api.md) for supported entry points, ownership, and behavior. These excerpts are a source reference, not a binary compatibility report.

## `yolo/src/main/java/com/apexfission/android/yolo/engine/Detection.kt`

[Source](../../yolo/src/main/java/com/apexfission/android/yolo/engine/Detection.kt)

```kotlin
package com.apexfission.android.yolo.engine
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.apexfission.android.math.models.ImageBox

@Immutable
@Stable
data class Detection(
    val box: ImageBox,
    val confidence: Float,
    val classId: Int
)
@Immutable
@Stable
data class Feature(
    val box: ImageBox,
    val confidence: Float,
    val classId: Int,
)
```

## `yolo/src/main/java/com/apexfission/android/yolo/engine/Detector.kt`

[Source](../../yolo/src/main/java/com/apexfission/android/yolo/engine/Detector.kt)

```kotlin
package com.apexfission.android.yolo.engine
import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import java.io.Closeable

interface Detector : Closeable {
    var enabled: Boolean
    fun detect(bitmap: Bitmap): List<Detection>
    fun detect(imageProxy: ImageProxy): List<Detection>
}
```

## `yolo/src/main/java/com/apexfission/android/yolo/engine/LetterboxResult.kt`

[Source](../../yolo/src/main/java/com/apexfission/android/yolo/engine/LetterboxResult.kt)

```kotlin
package com.apexfission.android.yolo.engine
import android.graphics.Bitmap
import kotlin.math.roundToInt

data class LetterboxResult(
    val bitmap: Bitmap,
    val scale: Float,
    val padX: Float,
    val padY: Float,
    val sourceWidth: Int,
    val sourceHeight: Int,
) {
    constructor(bitmap: Bitmap, scale: Float, padX: Float, padY: Float) : this(
        bitmap = bitmap,
        scale = scale,
        padX = padX,
        padY = padY,
        sourceWidth = inferSourceDimension(bitmap.width, padX, scale),
        sourceHeight = inferSourceDimension(bitmap.height, padY, scale),
    )
}
```

## `yolo/src/main/java/com/apexfission/android/yolo/engine/NumThreads.kt`

[Source](../../yolo/src/main/java/com/apexfission/android/yolo/engine/NumThreads.kt)

```kotlin
package com.apexfission.android.yolo.engine

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

## `yolo/src/main/java/com/apexfission/android/yolo/engine/ThreadConfinedYoloDetector.kt`

[Source](../../yolo/src/main/java/com/apexfission/android/yolo/engine/ThreadConfinedYoloDetector.kt)

```kotlin
package com.apexfission.android.yolo.engine
import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import com.apexfission.android.yolo.tflite.engine.EngineThreadDispatcher
import com.apexfission.android.yolo.tflite.engine.ThreadConfinedResource

class ThreadConfinedYoloDetector private constructor(
    private val resource: ThreadConfinedResource<Detector>,
) : Detector by resource.value {
    constructor(
        sharedDispatcher: EngineThreadDispatcher? = null,
        detectorFactory: () -> Detector,
    ) : this(ThreadConfinedResource(sharedDispatcher, detectorFactory))
    override fun detect(bitmap: Bitmap): List<Detection>
    override fun detect(imageProxy: ImageProxy): List<Detection>
    override var enabled: Boolean
        get()
        set(value)
    override fun close()
}
```

## `yolo/src/main/java/com/apexfission/android/yolo/engine/YoloDetector.kt`

[Source](../../yolo/src/main/java/com/apexfission/android/yolo/engine/YoloDetector.kt)

```kotlin
package com.apexfission.android.yolo.engine
import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import com.apexfission.android.yolo.image.LetterboxBuilder
import com.apexfission.android.yolo.image.toUprightBitmap
import com.apexfission.android.yolo.postprocess.YoloPostProcessor
import com.apexfission.android.yolo.tflite.engine.EngineThreadDispatcher
import com.apexfission.android.yolo.tflite.engine.InferenceEngine
import com.apexfission.android.yolo.tflite.engine.buildThreadConfinedInferenceEngine
import java.util.concurrent.atomic.AtomicBoolean

class YoloDetector(
    context: Context,
    modelPath: String,
    scoreThreshold: Float,
    iouThreshold: Float,
    useGpu: Boolean,
    maxNmsCandidates: Int = 150,
    numThreads: NumThreads = NumThreads.Default,
    private val sharedDispatcher: EngineThreadDispatcher? = null,
) : Detector {
    override var enabled: Boolean
        get()
        set(value)
    @Synchronized
    override fun detect(bitmap: Bitmap): List<Detection>
    override fun detect(imageProxy: ImageProxy): List<Detection>
    override fun close()
}
```

## `yolo/src/main/java/com/apexfission/android/yolo/engine/build.kt`

[Source](../../yolo/src/main/java/com/apexfission/android/yolo/engine/build.kt)

```kotlin
package com.apexfission.android.yolo.engine
import android.content.Context
import com.apexfission.android.yolo.tflite.engine.EngineThreadDispatcher

fun buildYoloDetector(
    context: Context,
    modelPath: String,
    scoreThreshold: Float,
    iouThreshold: Float,
    useGpu: Boolean,
    maxNmsCandidates: Int = 150,
    numThreads: NumThreads = NumThreads.Default,
    sharedDispatcher: EngineThreadDispatcher? = null
): Detector
fun buildThreadConfinedYoloDetector(
    context: Context,
    modelPath: String,
    scoreThreshold: Float,
    iouThreshold: Float,
    useGpu: Boolean,
    maxNmsCandidates: Int = 150,
    numThreads: NumThreads = NumThreads.Default,
    sharedDispatcher: EngineThreadDispatcher? = null
): Detector
```

## `yolo/src/main/java/com/apexfission/android/yolo/image/ImageOperations.kt`

[Source](../../yolo/src/main/java/com/apexfission/android/yolo/image/ImageOperations.kt)

```kotlin
package com.apexfission.android.yolo.image
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

fun rotateIfNeeded(bm: Bitmap, deg: Int): Bitmap
fun cropToAspectRatio(src: Bitmap, canvasWidth: Int, canvasHeight: Int, square: Boolean = false, top: Dp = 0.dp): Bitmap
fun centerCropSquare(src: Bitmap, top: Dp = 0.dp, maxSize: Int = Int.MAX_VALUE): Bitmap
data class CroppedResult(
    val bitmap: Bitmap,
    val xOffset: Int,
    val yOffset: Int
)
fun getCropRect(
    imageMode: PreProcessingImageTransformation,
    srcWidth: Int,
    srcHeight: Int,
    canvasWidth: Int = srcWidth,
    canvasHeight: Int = srcHeight
): Rect
fun cropWithOffset(
    imageMode: PreProcessingImageTransformation,
    bitmap: Bitmap,
    canvasSize: IntSize = IntSize(bitmap.width, bitmap.height)
): CroppedResult
fun crop(imageMode: PreProcessingImageTransformation, bitmap: Bitmap, canvasSize: IntSize = IntSize(bitmap.width, bitmap.height)): Bitmap
```

## `yolo/src/main/java/com/apexfission/android/yolo/image/ImageProxyExt.kt`

[Source](../../yolo/src/main/java/com/apexfission/android/yolo/image/ImageProxyExt.kt)

```kotlin
package com.apexfission.android.yolo.image
import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import com.apexfission.android.math.models.ImageBox

fun ImageProxy.toUprightBitmap(): Bitmap
fun Bitmap.crop(box: ImageBox): Bitmap
```

## `yolo/src/main/java/com/apexfission/android/yolo/image/PreProcessingImageTransformation.kt`

[Source](../../yolo/src/main/java/com/apexfission/android/yolo/image/PreProcessingImageTransformation.kt)

```kotlin
package com.apexfission.android.yolo.image
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
sealed class PreProcessingImageTransformation {
    data object FullImage : PreProcessingImageTransformation()
    data object CenterSquareCrop : PreProcessingImageTransformation()
    data class SquareCrop(val top: Dp = 0.dp) : PreProcessingImageTransformation()
    data object CenterVisibleImage : PreProcessingImageTransformation()
    data class VisibleImage(val top: Dp = 0.dp) : PreProcessingImageTransformation()
    data object CenterVisibleImageSquareCrop : PreProcessingImageTransformation()
    data class VisibleImageSquareCrop(val top: Dp = 0.dp) : PreProcessingImageTransformation()
}
```

## `yolo/src/main/java/com/apexfission/android/yolo/postprocess/YoloPostProcessor.kt`

[Source](../../yolo/src/main/java/com/apexfission/android/yolo/postprocess/YoloPostProcessor.kt)

```kotlin
package com.apexfission.android.yolo.postprocess
import com.apexfission.android.math.models.ImageBox
import com.apexfission.android.yolo.engine.Detection
import com.apexfission.android.yolo.engine.LetterboxResult
import com.apexfission.android.yolo.tflite.engine.InferenceEngine

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
    enum class OutputScalingMode {
        NONE,
        NORMALIZED,
    }
    fun process(
        output: FloatArray, letterboxResult: LetterboxResult
    ): List<Detection>
}
```

## `yolo/src/main/java/com/apexfission/android/yolo/postprocess/intersectionOverUnion.kt`

[Source](../../yolo/src/main/java/com/apexfission/android/yolo/postprocess/intersectionOverUnion.kt)

```kotlin
package com.apexfission.android.yolo.postprocess
import com.apexfission.android.yolo.engine.Detection

fun intersectionOverUnion(
    first: Detection,
    second: Detection
): Double
```

## `yolo/src/main/java/com/apexfission/android/yolo/tflite/buffers/BitmapTensorConverter.kt`

[Source](../../yolo/src/main/java/com/apexfission/android/yolo/tflite/buffers/BitmapTensorConverter.kt)

```kotlin
package com.apexfission.android.yolo.tflite.buffers
import android.graphics.Bitmap
import java.nio.ByteBuffer

fun quantizeToInt8(v: Float, zeroPoint: Int): Byte
fun fillBitmapToFloatBuffer(
    bitmap: Bitmap,
    buf: ByteBuffer,
    pixelBuffer: IntArray,
    inputImageWidth: Int
)
fun fillBitmapToByteBuffer(
    bitmap: Bitmap,
    buf: ByteBuffer,
    pixelBuffer: IntArray,
    inputImageWidth: Int,
    inScale: Float,
    inZeroPoint: Int
)
```

## `yolo/src/main/java/com/apexfission/android/yolo/tflite/buffers/InputTensorBuffers.kt`

[Source](../../yolo/src/main/java/com/apexfission/android/yolo/tflite/buffers/InputTensorBuffers.kt)

```kotlin
package com.apexfission.android.yolo.tflite.buffers
import android.graphics.Bitmap
import com.apexfission.android.yolo.tflite.validation.ModelTensorContract
import java.nio.ByteBuffer
import java.nio.ByteOrder

class InputTensorBuffers(
    val imageWidth: Int,
    val isInt8: Boolean,
    val scale: Float,
    val zeroPoint: Int,
    val buffer: ByteBuffer,
    val pixelBuffer: IntArray,
) {
    companion object {
        fun build(contract: ModelTensorContract): InputTensorBuffers
    }
    fun fill(bitmap: Bitmap)
}
```

## `yolo/src/main/java/com/apexfission/android/yolo/tflite/buffers/OutputTensorBuffers.kt`

[Source](../../yolo/src/main/java/com/apexfission/android/yolo/tflite/buffers/OutputTensorBuffers.kt)

```kotlin
package com.apexfission.android.yolo.tflite.buffers
import com.apexfission.android.yolo.tflite.engine.InferenceEngine
import com.apexfission.android.yolo.tflite.validation.ModelTensorContract
import java.nio.ByteBuffer
import java.nio.ByteOrder

class OutputTensorBuffers(
    val count: Int,
    val layout: InferenceEngine.OutputLayout,
    val attributes: Int,
    val boxes: Int,
    val isInt8: Boolean,
    val scale: Float,
    val zeroPoint: Int,
    val buffer: ByteBuffer,
    val floatArray: FloatArray,
) {
    companion object {
        fun build(contract: ModelTensorContract): OutputTensorBuffers
    }
    val numClasses: Int
        get()
    fun extractFloats(): FloatArray
}
```

## `yolo/src/main/java/com/apexfission/android/yolo/tflite/engine/EngineThreadDispatcher.kt`

[Source](../../yolo/src/main/java/com/apexfission/android/yolo/tflite/engine/EngineThreadDispatcher.kt)

```kotlin
package com.apexfission.android.yolo.tflite.engine
import java.io.Closeable
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.FutureTask

class EngineThreadDispatcher : Closeable {
    val isShutdown: Boolean
        get()
    override fun close()
}
```

## `yolo/src/main/java/com/apexfission/android/yolo/tflite/engine/InferenceEngine.kt`

[Source](../../yolo/src/main/java/com/apexfission/android/yolo/tflite/engine/InferenceEngine.kt)

```kotlin
package com.apexfission.android.yolo.tflite.engine
import android.graphics.Bitmap
import java.io.Closeable

class TfliteInitializationException(message: String, cause: Throwable? = null) : Exception(message, cause)
interface InferenceEngine : Closeable {
    val isInt8: Boolean
    val inputImageWidth: Int
    enum class OutputLayout {
        ATTRS_X_BOXES,
        BOXES_X_ATTRS,
    }
    val outLayout: OutputLayout
    val outAttrs: Int
    val outBoxes: Int
    val numClasses: Int
    val lastInferenceTimeMs: Long
    fun runInference(bitmap: Bitmap): FloatArray
}
```

## `yolo/src/main/java/com/apexfission/android/yolo/tflite/engine/ThreadConfinedInferenceEngine.kt`

[Source](../../yolo/src/main/java/com/apexfission/android/yolo/tflite/engine/ThreadConfinedInferenceEngine.kt)

```kotlin
package com.apexfission.android.yolo.tflite.engine
import android.graphics.Bitmap

class ThreadConfinedInferenceEngine private constructor(
    private val resource: ThreadConfinedResource<InferenceEngine>,
) : InferenceEngine by resource.value {
    constructor(
        sharedDispatcher: EngineThreadDispatcher? = null,
        engineFactory: () -> InferenceEngine,
    ) : this(ThreadConfinedResource(sharedDispatcher, engineFactory))
    override fun runInference(bitmap: Bitmap): FloatArray
    override val lastInferenceTimeMs: Long
        get()
    override fun close()
}
```

## `yolo/src/main/java/com/apexfission/android/yolo/tflite/engine/ThreadConfinedResource.kt`

[Source](../../yolo/src/main/java/com/apexfission/android/yolo/tflite/engine/ThreadConfinedResource.kt)

```kotlin
package com.apexfission.android.yolo.tflite.engine
import java.io.Closeable
import java.util.concurrent.Callable
import java.util.concurrent.FutureTask

class ThreadConfinedResource<T : Closeable>(
    sharedContext: EngineThreadDispatcher? = null,
    factory: () -> T,
) : Closeable {
    @Volatile
    var closeThreadId: Long
        private set
    val value: T
    fun <R> call(ifClosed: () -> R, operation: (T) -> R): R
    override fun close()
}
```

## `yolo/src/main/java/com/apexfission/android/yolo/tflite/engine/build.kt`

[Source](../../yolo/src/main/java/com/apexfission/android/yolo/tflite/engine/build.kt)

```kotlin
package com.apexfission.android.yolo.tflite.engine
import android.content.Context
import com.apexfission.android.yolo.engine.NumThreads

fun buildInferenceEngine(
    context: Context,
    modelPath: String,
    useGpu: Boolean,
    numThreads: NumThreads = NumThreads.Default,
    sharedDispatcher: EngineThreadDispatcher? = null
): InferenceEngine
fun buildThreadConfinedInferenceEngine(
    context: Context,
    modelPath: String,
    useGpu: Boolean,
    numThreads: NumThreads = NumThreads.Default,
    sharedDispatcher: EngineThreadDispatcher? = null
): InferenceEngine
```

## `yolo/src/main/java/com/apexfission/android/yolo/tflite/validation/TensorContractValidator.kt`

[Source](../../yolo/src/main/java/com/apexfission/android/yolo/tflite/validation/TensorContractValidator.kt)

```kotlin
package com.apexfission.android.yolo.tflite.validation
import com.apexfission.android.yolo.tflite.engine.InferenceEngine
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Tensor

data class TensorMetadata(
    val shape: IntArray,
    val dataType: DataType,
    val quantizationScale: Float,
    val quantizationZeroPoint: Int,
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
    val outputBoxes: Int,
)
object TensorContractValidator {
    fun validate(input: TensorMetadata, output: TensorMetadata): ModelTensorContract
}
```
