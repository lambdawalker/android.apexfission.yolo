# Apexfission YOLO: punto de entrada para agentes
Ámbito: **documentación de desarrollo de main**. El sitio muestra el commit de su compilación. Consulta
[IMPORT.md](../../IMPORT.md) para conocer la versión publicada y su código fuente, confirmados de forma independiente.
El código fuente de la API de la biblioteca en la revisión inicial de la documentación coincide con ese código confirmado;
los cambios futuros de main no están disponibles automáticamente en el paquete publicado.

Inferencia YOLO en dispositivos Android, preprocesamiento, validación de tensores y
NMS que tiene en cuenta las clases. El AAR no incluye modelo; tampoco interfaz de cámara, flujo de permisos ni seguimiento de objetos.

| Tarea | API / documento siguiente |
| --- | --- |
| Detectar en una imagen | `buildYoloDetector`, [inicio rápido](quickstart.md) |
| Gestionar la vida útil de los hilos de trabajo y recursos | [conceptos](concepts.md), `EngineThreadDispatcher` |
| Comprobar la compatibilidad del modelo | [contrato del modelo](model-contract.md) |
| Decodificar salidas personalizadas o coordenadas en píxeles | `YoloPostProcessor`, [recetas](recipes.md) |
| Recortar o transformar coordenadas | [API de imágenes](api/image.md), [conceptos](concepts.md) |
| Consultar firmas públicas exactas | [índice de la API](api.md) |
| Diagnosticar fallos / actualizar | [resolución de problemas](troubleshooting.md), [migración](migration.md) |
| Ejecutar los ejemplos existentes | [catálogo de demostraciones](demos.md) |

Reglas esenciales: las factorías y la inferencia bloquean; ejecútalas fuera del hilo principal. Mantén los
bitmaps de entrada vivos hasta que termine la llamada. Cierra cada detector o motor y, después, cualquier dispatcher compartido.
La aplicación cierra todos los `ImageProxy`. Las cajas de salida usan píxeles de la imagen de entrada orientada correctamente, no coordenadas
de la vista. El detector de alto nivel presupone salidas con centro y tamaño normalizados. Lee las
[limitaciones](limitations.md) antes de elegir un modelo. Las implementaciones internas no son API públicas.
