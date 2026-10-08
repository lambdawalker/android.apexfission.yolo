# Flujo de datos, hilos y responsabilidad sobre los recursos
## Proceso de detección
`buildYoloDetector` crea un envoltorio del detector confinado a un hilo. El bitmap se redimensiona
para encajar en un cuadrado con relleno negro reutilizado en caché, se convierte en tensores RGB, pasa por inferencia y decodificación,
se deshace el ajuste con relleno, se limitan las coordenadas a la imagen de entrada y se aplica NMS teniendo en cuenta las clases.
Las coordenadas son píxeles enteros de la imagen de origen en `ImageBox` (`x`, `y`, `x2`, `y2`), proporcionado
por la dependencia transitiva Apexfission Coordinates. Cada caja incluye confianza e identificador de clase.
`Feature` es un contenedor de datos público independiente con los mismos campos; el detector devuelve `Detection`.

## Tabla de responsabilidades
| Recurso | Responsable y regla de liberación |
| --- | --- |
| `Bitmap` de entrada | La aplicación; nunca modificar ni reciclar durante una llamada síncrona; el detector no lo recicla |
| `ImageProxy` pasado a detect | La aplicación; cerrar en `finally`; el detector solo libera su bitmap temporal orientado correctamente |
| Detector / motor de inferencia | La aplicación; cerrar al terminar; preferir factorías y `use` para tareas puntuales |
| `EngineThreadDispatcher` compartido | La aplicación; cerrar primero todos los envoltorios y al final el dispatcher |
| Caché de ajuste con relleno del detector | El detector; se reutiliza y se recicla al cerrar; no es pública |
| `FloatArray` de inferencia | Copia independiente; quien llama puede conservarla |
| Resultado de recorte/rotación | La aplicación; puede ser el mismo objeto de entrada si no hay transformación; comprobar la identidad antes de reciclar |

## Confinamiento síncrono
Las factorías gestionan la afinidad con el hilo físico durante la construcción, las llamadas y la liberación de recursos nativos/GPU.
**No** convierten la llamada en asíncrona. La construcción, la inferencia y el cierre pueden bloquear.
Ejecuta en un hilo de trabajo aunque el envoltorio tenga uno internamente. No hay callbacks,
flujos Flow, observadores del ciclo de vida ni métodos suspendidos en la biblioteca.

Una interrupción espera a que termine el trabajo nativo y restablece el indicador de interrupción. La cancelación de corrutinas
no interrumpe la inferencia nativa. Mantén los búferes vivos y termina la limpieza antes de
abandonar una imagen. Los envoltorios devuelven detecciones o matrices de salida vacías tras el cierre; la propiedad
`enabled` del detector devuelve false después de cerrar su envoltorio. No interpretes esos resultados vacíos como señal de éxito del modelo.

Para detección continua, serializa el trabajo de la aplicación y usa contrapresión en la cámara. Los envoltorios de las factorías
serializan el trabajo admitido. Evita construir `YoloDetector` directamente en procesos con un dispatcher
compartido: la interacción entre su monitor y el despacho es menos segura que el punto de entrada del envoltorio.
`ThreadConfinedResource.value` está expuesto, pero eludir `call` anula el confinamiento.

## Transformación a una vista previa
Para una imagen de origen orientada correctamente de ancho W y alto H, una vista previa centrada que muestra toda la imagen usa la escala
`min(viewWidth/W, viewHeight/H)`. Añade `(viewWidth-W*scale)/2` y `(viewHeight-H*scale)/2`
a las coordenadas escaladas. Para una vista previa con recorte centrado usa `max`. Ten en cuenta por separado
el reflejo de la cámara frontal y las transformaciones del viewport de CameraX; esta biblioteca no vincula vistas previas.

Si recortas antes de detectar, las cajas se refieren al recorte. Añade `CroppedResult.xOffset/yOffset`
antes de transformarlas a la imagen original. La sobrecarga de `ImageProxy` gira según
`imageInfo.rotationDegrees`; sus coordenadas pertenecen a ese bitmap orientado correctamente, no al sensor sin transformar.

## Ajustes
`scoreThreshold` controla la admisión, `iouThreshold` controla la supresión dentro de una misma clase solo cuando
IoU es estrictamente mayor, y `maxNmsCandidates` limita las **detecciones devueltas**, no el recorrido de
candidatos de entrada. Usa umbrales finitos en [0,1] y un límite positivo; los constructores no validan por completo
estos valores. El límite predeterminado es 150 en las factorías del detector. El número predeterminado de hilos de CPU es
`NumThreads.Default`: min(3, procesadores disponibles). Los porcentajes se redondean hacia abajo y se limitan a un mínimo de uno;
los recuentos personalizados también tienen un mínimo de uno, pero no se limitan al número de núcleos disponibles. Evita valores personalizados extremos.
