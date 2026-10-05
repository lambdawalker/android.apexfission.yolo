# `com.apexfission.android.carddetectionlite.domain.tflite.detector.yolo.postprocess`

Source set: `main` · Module: [`:cardDetectionLite`](../../../../../../../../../../../../docs/README.md)

## Contribution

Decodes YOLO predictions into bounded pixel-space detections and removes duplicates.

## Responsibilities and boundaries

YoloPostProcessor filters confidence, reverses letterboxing, checks finite coordinates, and clamps before unsigned conversion. YoloNms suppresses same-class overlaps and caps retained candidates. intersectionOverUnion also supports spatial matching.

## Source files

- [YoloNms.kt](YoloNms.kt)
- [YoloPostProcessor.kt](YoloPostProcessor.kt)
- [intersectionOverUnion.kt](intersectionOverUnion.kt)
