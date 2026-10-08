# Mapa de consulta de la API pública
Ámbito: código fuente de main. Las páginas de la API contienen referencias de declaraciones con los valores predeterminados reales y
contratos específicos. Omiten los cuerpos y las anotaciones y no son archivos Kotlin compilables por sí solos.
Los hashes del código fuente en `docs/api-excerpts.json` hacen fallar la compilación cuando es necesario revisar las referencias.

| Subsistema | Referencia |
| --- | --- |
| Factorías del detector, sobrecargas, tipos de resultado, opciones de hilos | [Detector](api/detector.md) |
| Factorías de inferencia, metadatos, confinamiento y liberación | [Inferencia](api/inference.md) |
| Decodificación de tensores, ajustes de NMS, IoU | [Posprocesamiento](api/postprocess.md) |
| Siete modos de imagen, funciones de recorte/rotación y conversión de CameraX | [Imágenes](api/image.md) |
| Búferes de entrada/salida, funciones de conversión, validación de tensores | [Búferes y validación](api/buffers-validation.md) |

Que una API sea pública no significa que siempre sea la capa de integración preferida. Usa las factorías; las funciones de menor nivel
requieren responsabilidad explícita sobre los recursos y metadatos coherentes. Los elementos internos `InferenceCore`,
`LetterboxBuilder`, `YoloNms`, `loadModelFile`, `Tensor.toMetadata` y los detalles internos del dispatcher
no son API para consumidores. `ImageBox` pertenece a la dependencia transitiva de coordenadas.
