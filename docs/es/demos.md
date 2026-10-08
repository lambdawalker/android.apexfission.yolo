# Demostraciones ejecutables
La aplicación consume `implementation(project(":yolo"))`, nunca el AAR publicado. Los ejemplos son
de desarrollo de main y, al publicarse, quedan fijados al commit de compilación del sitio web.

```bash
./gradlew :app:assembleDebug :app:installDebug
adb shell am start -n com.apexfission.android.yolo.demo/.MainActivity
```

Requiere JDK 21 para Gradle, JDK 17 para compilar, Android SDK 37 y un dispositivo/emulador
API 28+. La primera compilación descarga un modelo fijado y verifica su SHA-256. Como alternativa, abre/ejecuta **app** en
Android Studio. [Código completo de la Activity](../../app/src/main/java/com/apexfission/android/yolo/demo/MainActivity.kt).

| Ruta | Acción y resultado esperado | Recuperación / límites |
| --- | --- | --- |
| Choose photo | Selecciona una imagen de tarjeta; verás cajas, etiquetas, puntuaciones y tiempo total incluida la inicialización | Cero detecciones es válido; prueba otra foto; los errores de decodificación/modelo se muestran como estado |
| Request GPU + Choose photo | Ejecuta la misma imagen solicitando aceleración | Depende del dispositivo; desactiva GPU y reintenta; el interruptor no demuestra que se haya usado GPU |
| Test post-processing | Tres candidatos sintéticos producen dos cajas visibles | Ejemplo determinista de NMS, sin inferencia de modelo ni afirmación de precisión |

La aplicación de fotos usa el selector de Android sin permisos de cámara/almacenamiento/red en tiempo de ejecución. Mantiene
las fotos en el dispositivo y no determina la autenticidad de documentos. Las etiquetas describen regiones de tarjeta,
foto, código de barras y MRZ, no objetos arbitrarios.

El [código](../../app/src/main/java/com/apexfission/android/yolo/demo/DemoExamples.kt) del ejemplo determinista
y el [inicio rápido compilado](../../app/src/main/java/com/apexfission/android/yolo/demo/DocumentationQuickstart.kt)
se extraen en las guías de este sitio durante la compilación.

## Procedencia del modelo
Commit de origen fijado: `115571941f890c4de0324cd008cff31a2068e722` en
`lambdawalker/android.card_detection_lite`, ruta
`tfmodel/src/main/assets/cdl/tflite/Y11-640E197F16.tflite`.
SHA-256: `473f497ca45b4ab2f66ef59b3da1c0e27a0ac11227279ba6d89a19566c450e1e`.
La tarea de preparación de modelo por variante de Gradle falla si falta la descarga o no coincide la suma de comprobación.
Los binarios del modelo nunca se distribuyen en el AAR de Maven.

## Verificación
```bash
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
./gradlew :app:connectedDebugAndroidTest
```
Las pruebas en dispositivo comprueban NMS determinista y la inicialización/inferencia/cierre del modelo en CPU.
Estas comprobaciones no sustituyen las pruebas de precisión con imágenes reales, dispositivos GPU ni integración de cámara.
No se han capturado nuevas pantallas de la demostración; consulta la [política de capturas](limitations.md#screenshots).
