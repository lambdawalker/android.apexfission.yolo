# Resolución de problemas
| Síntoma | Causa probable y corrección |
| --- | --- |
| Error al abrir/mapear el asset | Coloca el modelo en los assets de la aplicación, comprueba la ruta exacta y mayúsculas/minúsculas, marca tflite como no comprimido |
| Contrato de tensor no admitido | Inspecciona las formas, tipos y escalas reales; vuelve a exportar con el contrato admitido |
| Cajas enormes, mal situadas o vacías | Comprueba salida normalizada frente a píxeles, metadatos de ajuste con relleno y orden de clases |
| Cajas desplazadas en la vista previa | Aplica desplazamientos de recorte, orientación, escala, relleno y reflejo exactamente una vez |
| La interfaz se congela | Las factorías, inferencia y cierre bloquean; llama desde un hilo de trabajo de la aplicación |
| La cámara deja de entregar fotogramas | Cierra cada ImageProxy en finally, incluso en rutas de fallo o desactivación |
| Fallo de GPU | Reintenta la construcción explícitamente con useGpu=false; verifica el hardware en el dispositivo |
| Salida vacía | El modelo puede no encontrar nada, el umbral ser demasiado alto, la entrada incorrecta o el detector estar desactivado/cerrado |
| Bitmap reciclado / error al leer píxeles | Conserva la entrada hasta el retorno; decodifica un bitmap por software; evita reciclar dos veces un alias |
| Excepción de apagado/tarea rechazada | El dispatcher compartido se cerró demasiado pronto; cierra primero los envoltorios |
| Incompatibilidad de metadatos Kotlin | Usa un compilador compatible con la versión registrada en IMPORT.md |
| Fallo al descargar el modelo de demostración | Comprueba el acceso a la URL de origen fijada; nunca omitas la verificación SHA-256 |

Captura los fallos en el límite de la aplicación y ofrece reintentos. No absorbas la cancelación en el código de interfaz
con corrutinas; vuelve a lanzar `CancellationException`. Conserva la causa subyacente al informar de
`TfliteInitializationException`. Para una comprobación básica del modelo, ejecuta la prueba de CPU existente en dispositivo;
después valida por separado las imágenes reales y el comportamiento GPU de los dispositivos de destino. Consulta las [demostraciones](demos.md).
