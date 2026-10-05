# `com.apexfission.android.carddetectionlite.domain.tflite.detector.tflite.engine`

Source set: `main` · Module: [`:cardDetectionLite`](../../../../../../../../../../../../docs/README.md)

## Contribution

Owns the TFLite interpreter, optional GPU delegate, inference timing, and physical worker thread.

## Responsibilities and boundaries

buildInferenceEngine returns a thread-confined wrapper. EngineThreadDispatcher can be shared across pipeline wrappers for construction, calls, and teardown on one thread. Calls remain synchronous. Close borrowers before a shared dispatcher. GPU use is attempted for compatible non-INT8 inputs; interpreter initialization can still fail.

## Source files

- [EngineThreadDispatcher.kt](EngineThreadDispatcher.kt)
- [InferenceCore.kt](InferenceCore.kt)
- [InferenceEngine.kt](InferenceEngine.kt)
- [ThreadConfinedInferenceEngine.kt](ThreadConfinedInferenceEngine.kt)
- [ThreadConfinedResource.kt](ThreadConfinedResource.kt)
- [build.kt](build.kt)
