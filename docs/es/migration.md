# Migración y alcance de versiones
Este repositorio extrae el módulo YOLO de `android.card_detection_lite`. Los nombres de paquete
siguen siendo `com.apexfission.android.yolo`; el módulo independiente es `:yolo` y el de demostración es `:app`.
Usa [IMPORT.md](../../IMPORT.md) para las coordenadas confirmadas, en lugar de adivinar versiones.
Elimina los módulos de código duplicados al adoptar Maven para evitar clases duplicadas.
La dependencia geométrica es transitiva; ya no se necesita una copia hermana del repositorio de coordenadas.

Prefiere `buildYoloDetector` / `buildInferenceEngine`. Ambas delegan en sus equivalentes con
confinamiento explícito a un hilo. No accedas a los elementos internos `InferenceCore`, `LetterboxBuilder`,
`YoloNms`, el cargador de modelos ni `Tensor.toMetadata` desde aplicaciones consumidoras.

`LetterboxResult` tiene un constructor primario de seis campos que contiene las dimensiones reales del origen;
el constructor de compatibilidad de cuatro argumentos infiere las dimensiones a partir de la escala y el relleno. Proporciona las dimensiones
reales cuando las conozcas. La salida de bajo nivel con coordenadas en píxeles requiere `OutputScalingMode.NONE`.

El sitio web describe main en el commit de compilación que muestra. El código fuente del lanzamiento confirmado se
indica de forma independiente en IMPORT.md. No se deduce ninguna garantía de compatibilidad de la versión de compilación
de main ni del número de ejecución de CI. Quienes mantienen el proyecto comparan el código del lanzamiento y main antes de afirmar que un cambio
está disponible para consumidores de Maven. Este trabajo de documentación no cambia ninguna API ni comportamiento en ejecución.
