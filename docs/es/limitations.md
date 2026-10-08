# Limitaciones y objetivos excluidos
- Solo Android API 28+; no hay contrato de ejecución para escritorio JVM, iOS, JavaScript ni servidores.
- El AAR de la biblioteca no incluye un modelo entrenado ni etiquetas. Los pesos de la demostración son assets separados.
- Solo se admite el [contrato de tensores](model-contract.md); no todas las exportaciones YOLO son compatibles.
- No incluye pantalla de cámara ni gestor de permisos.
- No hay vinculación automática del ciclo de vida, cancelación asíncrona, adaptador de corrutinas ni transformación de vista previa.
- La aceleración GPU es condicional y no informa mediante un indicador del backend activo ni garantiza una alternativa
  tras un fallo de inicialización del intérprete.
- El límite NMS restringe los resultados devueltos; muchas cajas de entrada pueden seguir consumiendo bastante CPU.
- Los búferes y posprocesadores públicos de bajo nivel requieren tamaños coherentes y acceso serializado.
- Las funciones de recorte usan la densidad del sistema para desplazamientos Dp; proporciona dimensiones positivas del origen y del lienzo.
  `centerCropSquare(maxSize=...)` limita la salida en el origen del recorte seleccionado, no crea un cuadrado menor
  centrado de nuevo. Algunos resultados de recorte/rotación son alias de su entrada.
- Las comprobaciones de forma no validan etiquetas de entrenamiento, calibración, precisión, equidad ni validez de documentos.

## Capturas de pantalla
La biblioteca distribuida no tiene interfaz propia. La aplicación existente es una visualización interactiva útil, pero
no existe un conjunto revisado de capturas de dispositivo. Este cambio de documentación no fabrica imágenes
ni implica que los diagramas estáticos demuestren el comportamiento de GPU, cámara o ciclo de vida. La captura en dispositivos está bloqueada
en el entorno de autoría por la ausencia de Android SDK/emulador. El recorrido textual y el código
real siguen siendo la evidencia. Una futura galería debe capturar la aplicación real, registrar dispositivo/API,
viewport/tema/idioma/hashes del código fuente y separar la renderización candidata, las actualizaciones explícitas de referencias
y la validación. Hasta entonces, no se realiza ninguna comprobación de capturas ni se afirma disponer de capturas de lanzamiento.
