# Primera detección
Lee la [instalación](../../IMPORT.md). En tu aplicación Android usa minSdk 28 y JVM 17;
usa un compilador Kotlin compatible con los metadatos de la biblioteca. El AAR no incluye un modelo.

1. Coloca un modelo compatible en `app/src/main/assets/detector.tflite` (este nombre es un ejemplo).
2. Mantenlo sin comprimir, porque el cargador usa `AssetManager.openFd` y mapeo en memoria:

```kotlin
android {
    androidResources { noCompress += "tflite" }
}
```

3. Decodifica un bitmap orientado correctamente y respaldado por software; evita los bitmaps exclusivamente de hardware, porque el preprocesamiento
lee píxeles. La demostración de fotos ejecutable usa `ImageDecoder.ALLOCATOR_SOFTWARE` y limita su
lado mayor a 1600 píxeles. Consulta el [contrato del modelo](model-contract.md).
4. Usa esta función completa del módulo de demostración; toma prestado tu bitmap y devuelve detecciones en píxeles de origen:

<!-- example: quickstart -->

```kotlin
package com.apexfission.android.yolo.demo

import android.content.Context
import android.graphics.Bitmap
import com.apexfission.android.yolo.engine.Detection
import com.apexfission.android.yolo.engine.buildYoloDetector

/** Blocking one-image example. The caller owns [bitmap] and must use a worker thread. */
fun detectImage(context: Context, bitmap: Bitmap, modelAssetPath: String): List<Detection> =
    buildYoloDetector(
        context = context.applicationContext,
        modelPath = modelAssetPath,
        scoreThreshold = 0.35f,
        iouThreshold = 0.45f,
        useGpu = false,
    ).use { detector -> detector.detect(bitmap) }
```

<!-- end-example: quickstart -->

La función se compila como parte de `:app:assembleDebug`. Llámala desde un ejecutor de trabajo o
dentro de `withContext(Dispatchers.IO)` en una corrutina gestionada por tu pantalla. Las dependencias de corrutinas
y la gestión del ciclo de vida corresponden a la aplicación. Construye y cierra un detector por imagen, lo que
simplifica el uso pero añade coste de inicio; un proceso de cámara debería reutilizar un detector entre fotogramas.

Una llamada correcta devuelve cero o más valores `Detection`. La aplicación gestiona las etiquetas: asocia
`classId` con el orden exacto de las etiquetas de entrenamiento. Un valor de confianza es una puntuación del modelo, no una
probabilidad calibrada ni una prueba de identidad. Muestra las cajas usando la transformación de origen a vista
de [conceptos](concepts.md). Gestiona las excepciones en tu interfaz y permite otra imagen o un reintento en CPU.

Para ejecutar la interfaz completa, sigue el [catálogo de demostraciones](demos.md).
