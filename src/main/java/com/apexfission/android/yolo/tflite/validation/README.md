# `com.apexfission.android.carddetectionlite.domain.tflite.detector.tflite.validation`

Source set: `main` · Module: [`:cardDetectionLite`](../../../../../../../../../../../../docs/README.md)

## Contribution

Rejects incompatible tensor contracts before reusable buffers are allocated.

## Responsibilities and boundaries

The model loader reads assets into a direct ByteBuffer. TensorContractValidator requires square RGB input and rank-three YOLO output with distinct attribute/box dimensions. FLOAT32 and INT8 are supported with checked quantization and allocation sizes. The engine separately checks input/output tensor counts.

## Source files

- [TensorContractValidator.kt](TensorContractValidator.kt)
- [TfliteModelLoader.kt](TfliteModelLoader.kt)
