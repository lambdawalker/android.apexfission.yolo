# API de posprocesamiento

Construye con disposición y dimensiones de salida coherentes, número de clases, ancho de entrada, umbrales finitos
y un límite positivo de resultados. `OutputScalingMode.NORMALIZED` es el valor predeterminado; `NONE` significa coordenadas
de centro/tamaño en píxeles. El detector no expone este ajuste: usa el proceso de menor nivel.
`process` recorre síncronamente un tensor aplanado, deshace el ajuste con relleno, limita las coordenadas,
descarta cajas no válidas o degeneradas y devuelve resultados de NMS por clase ordenados por confianza.
No asume la gestión del bitmap de `LetterboxResult` ni infiere dimensiones.

Una escala no finita/cero o dimensiones de origen no positivas producen resultados vacíos. El procesador no
comprueba la longitud de la matriz aplanada ni todos los valores del constructor; los metadatos mal formados pueden lanzar
errores de índices/asignación. Gana la puntuación de clase estrictamente positiva más alta; mantén positivos los umbrales de puntuación
para evitar los casos extremos de puntuación cero/clase -1. Los valores no son puntuaciones calibradas ni limitadas.
NMS suprime cajas de la misma clase cuando IoU > umbral. El límite restringe las cajas devueltas.
`intersectionOverUnion` es una función pura y síncrona de proporción de solapamiento; si las áreas son disjuntas o degeneradas, devuelve cero.
`YoloNms` es interno. La [demostración sintética](../recipes.md#decode-a-tensor-without-running-a-model)
proporciona cobertura ejecutable.


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

## Código fuente

- [YoloPostProcessor.kt](../../../yolo/src/main/java/com/apexfission/android/yolo/postprocess/YoloPostProcessor.kt)
- [intersectionOverUnion.kt](../../../yolo/src/main/java/com/apexfission/android/yolo/postprocess/intersectionOverUnion.kt)
