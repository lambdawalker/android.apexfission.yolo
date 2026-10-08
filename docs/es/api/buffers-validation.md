# API de búferes de tensores y validación

API avanzadas; la mayoría de aplicaciones deberían usar las factorías del detector. `TensorMetadata` describe un
tensor. `TensorContractValidator.validate` devuelve un `ModelTensorContract` o lanza
`IllegalArgumentException` en los casos no admitidos del [contrato del modelo](../model-contract.md).
Las formas son matrices mutables: no las cambies durante la validación. Los tipos de metadatos exponen
`org.tensorflow.lite.DataType`; los consumidores que usan directamente estos tipos de bajo nivel necesitan una dependencia de compilación
LiteRT compatible (la biblioteca declara actualmente LiteRT como implementation).

Prefiere `build(contract)` del objeto compañero del búfer después de la validación a componer manualmente
capacidades incoherentes. Ambas clases de búfer reutilizan almacenamiento interno y no son seguras para concurrencia.
Los búferes de entrada son directos y de orden nativo; las matrices de píxeles deben tener exactamente S*S entradas. El llenado lee
la región superior izquierda S*S, en orden RGB normalizado por 255; sin redimensionamiento, alfa, ajuste con relleno ni intercambio BGR.
Usa un bitmap por software de tamaño exacto. Las imágenes/búferes demasiado pequeños provocan excepciones de Android/búfer.

Los búferes FLOAT32 necesitan 3*S*S*4 bytes; INT8 necesita 3*S*S bytes. `quantizeToInt8` espera que el float
ya esté escalado: suma el punto cero, trunca y limita; no realiza la operación completa de cuantización.
`fillBitmapToByteBuffer` realiza la normalización y el escalado; usa una escala finita positiva.
`OutputTensorBuffers.extractFloats` rebobina el búfer, descuantiza cuando corresponde y devuelve una
copia para que las llamadas posteriores no modifiquen resultados anteriores. `numClasses` equivale a atributos menos cuatro.
No modifiques `floatArray`/`buffer` mientras se ejecuta la conversión. Los métodos generados de las clases de datos
están disponibles, pero la igualdad de IntArray no compara profundamente las formas.


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

## Código fuente

- [TensorContractValidator.kt](../../../yolo/src/main/java/com/apexfission/android/yolo/tflite/validation/TensorContractValidator.kt)
- [InputTensorBuffers.kt](../../../yolo/src/main/java/com/apexfission/android/yolo/tflite/buffers/InputTensorBuffers.kt)
- [OutputTensorBuffers.kt](../../../yolo/src/main/java/com/apexfission/android/yolo/tflite/buffers/OutputTensorBuffers.kt)
- [BitmapTensorConverter.kt](../../../yolo/src/main/java/com/apexfission/android/yolo/tflite/buffers/BitmapTensorConverter.kt)
