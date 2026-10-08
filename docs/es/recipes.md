# Recetas de integración
## Análisis con CameraX (fragmento parcial de la aplicación)
Crea un detector en un hilo de trabajo antes de vincular el análisis; mantenlo vivo entre fotogramas. Dentro
de tu analizador, usa este patrón (la aplicación proporciona `detector`, `image` y la entrega a la interfaz):

```kotlin
try {
    val detections = detector.detect(image)
    // Marshal results to the UI using your application's lifecycle scope.
} finally {
    image.close()
}
```

Usa `STRATEGY_KEEP_ONLY_LATEST`, un ejecutor de trabajo y permisos/vinculación de cámara gestionados por la aplicación.
Detén el análisis y espera a que terminen las llamadas activas antes de cerrar el detector. Esto es un patrón de integración,
no una demostración de cámara incluida. La [demostración de fotos](demos.md) es la integración completa de interfaz ejecutable.

## Compartir un hilo nativo de trabajo (configuración parcial)
Importa `com.apexfission.android.yolo.tflite.engine.EngineThreadDispatcher` y pasa la misma
instancia como `sharedDispatcher` a las factorías del detector/motor. Inicializa y usa esos
envoltorios desde código de trabajo. En `finally`, cierra los envoltorios antes del dispatcher compartido.
No lo sustituyas por un ejecutor o dispatcher de corrutinas arbitrario: importa la afinidad con el hilo físico.

## Decodificar un tensor sin ejecutar un modelo
El ejemplo sintético completo compilado en la aplicación es:

<!-- example: postprocess -->

```kotlin
package com.apexfission.android.yolo.demo

import android.graphics.Bitmap
import com.apexfission.android.yolo.engine.Detection
import com.apexfission.android.yolo.engine.LetterboxResult
import com.apexfission.android.yolo.postprocess.YoloPostProcessor
import com.apexfission.android.yolo.tflite.engine.InferenceEngine

/** Real public post-processing API with deterministic tensor data, not model inference. */
object DemoExamples {
    fun postProcess(bitmap: Bitmap): List<Detection> = YoloPostProcessor(
        outLayout = InferenceEngine.OutputLayout.BOXES_X_ATTRS,
        outBoxes = 3,
        outAttrs = 5,
        numClasses = 1,
        inputImageWidth = 640,
        scoreThreshold = 0.35f,
        iouThreshold = 0.45f,
        maxNmsCandidates = 150,
    ).process(
        floatArrayOf(
            0.30f, 0.30f, 0.40f, 0.25f, 0.90f,
            0.31f, 0.31f, 0.40f, 0.25f, 0.80f,
            0.80f, 0.80f, 0.20f, 0.20f, 0.70f,
        ),
        LetterboxResult(bitmap, 1f, 0f, 0f, 640, 640),
    )

    val classLabels = listOf(
        "Horizontal card", "Vertical card", "Card back", "Photo", "Slim barcode",
        "PDF417", "MRZ text", "Barcode", "QR code",
    )
}
```

<!-- end-example: postprocess -->

Tres candidatos se convierten en dos detecciones: se suprime el candidato solapado de menor puntuación.
Esto comprueba el posprocesamiento, no la precisión. Para tensores de coordenadas en píxeles sin normalizar, selecciona
`outputScalingMode = YoloPostProcessor.OutputScalingMode.NONE`. Pasa las dimensiones exactas del tensor,
el número de clases y un `LetterboxResult` coherente; el posprocesador no valida la longitud de la matriz
ni la coherencia de los metadatos y puede lanzar excepciones de índices con entradas mal formadas.

## Recortar antes de detectar
Usa `cropWithOffset(PreProcessingImageTransformation.CenterSquareCrop, bitmap)` y detecta sobre
su bitmap. Añade los desplazamientos devueltos para transformar las detecciones a la imagen original. Recicla un
recorte nuevo solo después de que terminen la inferencia y los consumidores de visualización; evita reciclar un alias del origen.
Consulta la [API de imágenes](api/image.md) para los siete modos y las sobrecargas de recorte.

## Pausar la detección
Establece `detector.enabled = false` para devolver detecciones vacías sin inferencia. Esto no
vincula el ciclo de vida ni libera recursos nativos. Establécelo en true para reanudar; cierra al terminar.
