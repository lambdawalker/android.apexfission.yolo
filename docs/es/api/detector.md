# API del detector

Usa `buildYoloDetector` para la integración habitual. Las dos factorías tienen parámetros idénticos
y devuelven `Detector`. `modelPath` es una ruta de asset sin comprimir; score/IoU/useGpu son obligatorios,
mientras que el límite, los hilos y el dispatcher compartido tienen los valores predeterminados mostrados abajo. `detect` toma prestado un bitmap
o ImageProxy y devuelve detecciones en píxeles de origen de forma síncrona; la aplicación cierra los proxies.
La construcción puede fallar al acceder a assets, validar tensores o inicializar recursos nativos.
Cierra al terminar; los envoltorios impiden el trabajo tras el cierre. Consulta las [responsabilidades](../concepts.md).

`YoloDetector` es público, pero es preferible usar una factoría a construirlo directamente. Los envoltorios confinados
a un hilo aceptan factorías personalizadas; estas se ejecutan en el hilo físico de trabajo seleccionado.
`Detection` y `Feature` son clases de datos (incluyen los métodos de copia, componentes e igualdad generados).
`LetterboxResult` contiene la transformación inversa del posprocesamiento y las dimensiones de origen; su
constructor de compatibilidad infiere las dimensiones y devuelve cero si la escala o el relleno no son válidos.
`NumThreads.toInt()` redondea hacia abajo los recuentos porcentuales y los limita a un mínimo de uno; los personalizados no se limitan a la CPU.


Tipos habituales de referencia: `android.content.Context`, `android.graphics.Bitmap`,
`androidx.camera.core.ImageProxy`, `androidx.compose.ui.unit.Dp`, `IntSize`, `dp`,
`java.nio.ByteBuffer`, `com.apexfission.android.math.models.ImageBox`,
`com.apexfission.android.yolo.engine.{Detection, LetterboxResult, NumThreads}`,
`com.apexfission.android.yolo.tflite.engine.{InferenceEngine, EngineThreadDispatcher}`
y `com.apexfission.android.yolo.tflite.validation.ModelTensorContract`.
Las listas entre llaves abrevian importaciones individuales; no son sintaxis de importación Kotlin.

## Declaraciones públicas

Las firmas siguientes omiten los cuerpos de métodos y las anotaciones; importa cada tipo desde el paquete indicado.
Las clases de datos también exponen los métodos de copia, componentes e igualdad generados por Kotlin.

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

## Código fuente

- [Detector.kt](../../../yolo/src/main/java/com/apexfission/android/yolo/engine/Detector.kt)
- [build.kt](../../../yolo/src/main/java/com/apexfission/android/yolo/engine/build.kt)
- [YoloDetector.kt](../../../yolo/src/main/java/com/apexfission/android/yolo/engine/YoloDetector.kt)
- [ThreadConfinedYoloDetector.kt](../../../yolo/src/main/java/com/apexfission/android/yolo/engine/ThreadConfinedYoloDetector.kt)
- [Detection.kt](../../../yolo/src/main/java/com/apexfission/android/yolo/engine/Detection.kt)
- [LetterboxResult.kt](../../../yolo/src/main/java/com/apexfission/android/yolo/engine/LetterboxResult.kt)
- [NumThreads.kt](../../../yolo/src/main/java/com/apexfission/android/yolo/engine/NumThreads.kt)
