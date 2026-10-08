# API de inferencia y confinamiento

Las factorías construyen un motor validado en un hilo físico de trabajo y devuelven `InferenceEngine`.
`runInference` recibe un bitmap cuadrado por software preparado y devuelve una matriz aplanada independiente
de floats, con la salida INT8 descuantizada. No ajusta con relleno ni ejecuta NMS. Las propiedades de metadatos
describen dimensiones validadas. `lastInferenceTimeMs` incluye conversión, ejecución del modelo
y extracción de salida; no es el temporizador de la demostración de fotos que incluye la inicialización.

`ThreadConfinedInferenceEngine` devuelve una matriz vacía y tiempo cero tras el cierre.
`EngineThreadDispatcher.isShutdown` refleja el apagado del ejecutor. Su `close` público inicia
el apagado; cierra primero los envoltorios. `ThreadConfinedResource.call` serializa las operaciones y usa
`ifClosed` una vez admitido el cierre; el cierre espera a la liberación del recurso. Las excepciones de inicialización
se propagan y los dispatchers propios se cierran si falla la factoría. Acceder directamente a `value`
elude el confinamiento. `closeThreadId` es de solo lectura para consumidores y comienza en -1.

Los getters de metadatos delegados por los envoltorios no son dispatchers de operaciones mutables arbitrarias.
Usa las factorías incorporadas y metadatos inmutables; las factorías personalizadas deben respetar sus propios contratos.
Consulta las [restricciones del modelo](../model-contract.md) para errores y limitaciones de GPU.


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

## Código fuente

- [InferenceEngine.kt](../../../yolo/src/main/java/com/apexfission/android/yolo/tflite/engine/InferenceEngine.kt)
- [build.kt](../../../yolo/src/main/java/com/apexfission/android/yolo/tflite/engine/build.kt)
- [ThreadConfinedInferenceEngine.kt](../../../yolo/src/main/java/com/apexfission/android/yolo/tflite/engine/ThreadConfinedInferenceEngine.kt)
- [EngineThreadDispatcher.kt](../../../yolo/src/main/java/com/apexfission/android/yolo/tflite/engine/EngineThreadDispatcher.kt)
- [ThreadConfinedResource.kt](../../../yolo/src/main/java/com/apexfission/android/yolo/tflite/engine/ThreadConfinedResource.kt)
