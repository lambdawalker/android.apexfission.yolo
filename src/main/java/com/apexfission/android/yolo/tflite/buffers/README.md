# `com.apexfission.android.carddetectionlite.domain.tflite.detector.tflite.buffers`

Source set: `main` · Module: [`:cardDetectionLite`](../../../../../../../../../../../../docs/README.md)

## Contribution

Converts RGB pixels to reusable interpreter input storage and decodes output storage.

## Responsibilities and boundaries

Internal input buffers normalize FLOAT32 or quantize INT8 pixels using tensor metadata. Output buffers dequantize independently and return a fresh FloatArray. Mutable buffers belong to one engine and are not shared across concurrent runs.

## Source files

- [BitmapTensorConverter.kt](BitmapTensorConverter.kt)
- [InputTensorBuffers.kt](InputTensorBuffers.kt)
- [OutputTensorBuffers.kt](OutputTensorBuffers.kt)
