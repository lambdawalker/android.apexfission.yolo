# `com.apexfission.android.carddetectionlite.domain.tflite.detector.yolo.engine`

Source set: `main` · Module: [`:cardDetectionLite`](../../../../../../../../../../../../docs/README.md)

## Contribution

Turns upright images into generic object detections through letterboxing, inference, and decoding.

## Responsibilities and boundaries

Detector exposes bitmap and ImageProxy entry points. YoloDetector serializes the complete reusable-buffer pipeline; builders support thread confinement and a shared dispatcher. Results describe the supplied image before UI preprocessing offsets are restored. Close releases engine and letterbox resources.

## Source files

- [Detector.kt](Detector.kt)
- [ThreadConfinedYoloDetector.kt](ThreadConfinedYoloDetector.kt)
- [YoloDetector.kt](YoloDetector.kt)
- [build.kt](build.kt)
