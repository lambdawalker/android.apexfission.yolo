# API de transformación de imágenes

Estas funciones síncronas operan sobre bitmaps Android. Usa dimensiones positivas válidas y
entrada respaldada por software. `getCropRect` devuelve límites en píxeles de origen; `cropWithOffset` también devuelve
el origen del recorte. `crop` descarta los metadatos de desplazamiento. Los desplazamientos Dp usan la densidad de pantalla del sistema y
los verticales se limitan al intervalo válido. Los modos de imagen visible coinciden con la proporción del lienzo proporcionado;
no inspeccionan una vista previa CameraX activa ni reflejan imágenes.

`FullImage` conserva el fotograma, los modos de cuadrado centrado seleccionan su centro, las variantes con desplazamiento lo mueven
verticalmente y las variantes de cuadrado visible recortan en cuadrado la zona ajustada a la proporción.
`centerCropSquare(maxSize)` limita el ancho/alto en el origen del recorte seleccionado, en vez de
centrar de nuevo un cuadrado más pequeño. Los recortes y la rotación de cero grados pueden devolver el objeto de entrada;
comprueba su identidad antes de liberarlo. `Bitmap.crop(ImageBox)` limita los bordes y lanza
`IllegalArgumentException` si los límites quedan vacíos o invertidos tras limitarse.

`ImageProxy.toUprightBitmap` gira según los metadatos de rotación CameraX y recicla los bitmaps
intermedios; quien llama gestiona el bitmap devuelto y debe cerrar el proxy igualmente. Las demás funciones de bitmap
no reciclan las entradas del llamante. `LetterboxBuilder` es interno y no es una API para consumidores.


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

## Código fuente

- [PreProcessingImageTransformation.kt](../../../yolo/src/main/java/com/apexfission/android/yolo/image/PreProcessingImageTransformation.kt)
- [ImageOperations.kt](../../../yolo/src/main/java/com/apexfission/android/yolo/image/ImageOperations.kt)
- [ImageProxyExt.kt](../../../yolo/src/main/java/com/apexfission/android/yolo/image/ImageProxyExt.kt)
