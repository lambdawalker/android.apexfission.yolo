# Contrato de modelos y tensores admitidos
El nombre YOLO o la extensión del archivo no bastan para determinar la compatibilidad. La inicialización valida
una entrada y una salida; los pesos del modelo, el orden de las etiquetas y la semántica de salida siguen siendo responsabilidad tuya.

| Propiedad | Comportamiento requerido |
| --- | --- |
| Forma de entrada | `[1, S, S, 3]`, S positivo y cuadrado, canales RGB al final |
| Forma de salida | `[1, attributes, boxes]` o `[1, boxes, attributes]` |
| Inferencia de ejes de salida | La dimensión menor corresponde a los atributos; las dimensiones deben diferir; attributes >= 5 |
| Atributos | Centro X, centro Y, ancho, alto y después puntuaciones de clase (sin canal de presencia de objeto independiente) |
| Tipos | Entrada/salida FLOAT32 o INT8 con signo; admitidos independientemente |
| Cuantización | INT8 con escala finita positiva y punto cero en [-128,127] |
| Valores de entrada | RGB / 255; INT8 además divide por la escala, suma el punto cero, trunca y limita |
| Coordenadas de alto nivel | Centro y tamaño normalizados; se escalan por el ancho de entrada del modelo |
| Coordenadas en píxeles de bajo nivel | Seleccionar `YoloPostProcessor.OutputScalingMode.NONE` explícitamente |

No se admiten formas dinámicas, entradas no cuadradas, UINT8, varias salidas, máscaras de segmentación, puntos clave de pose,
un canal independiente de presencia de objeto ni formatos de salida con NMS integrado. Un modelo con semántica incorrecta
puede superar las comprobaciones de forma. La inferencia de ejes de salida requiere más cajas que atributos.

El validador rechaza dimensiones no válidas, tipos no admitidos, cuantización no válida, ejes
ambiguos y recuentos de elementos/bytes que superen la capacidad de Int con `IllegalArgumentException`.
La factoría de inferencia envuelve los errores de configuración de tensores/intérprete en `TfliteInitializationException`;
los errores de carga de assets pueden propagarse directamente porque la carga precede a ese envoltorio.

`useGpu` es una solicitud, no una garantía. Las entradas INT8 omiten la GPU. Un hardware no compatible o un fallo
al crear el delegado pueden permitir continuar en CPU. Si la construcción del intérprete con un delegado falla,
no se garantiza un reintento en CPU; cierra los recursos fallidos y reconstruye explícitamente con `useGpu=false`.
La biblioteca no expone una consulta del backend activo. Comprueba precisión y rendimiento en los dispositivos de destino.

Para llamar directamente a `InferenceEngine.runInference`, proporciona un bitmap cuadrado por software ya preparado
que coincida con `inputImageWidth`. El motor no ajusta con relleno ni aplica NMS. Las imágenes sobredimensionadas no
sustituyen el preprocesamiento explícito; el llenado del búfer lee la región superior izquierda del tamaño de entrada.
Usa `buildYoloDetector` si necesitas el proceso completo desde bitmap hasta cajas.
