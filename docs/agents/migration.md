# Migration and version scope
This repository extracts the YOLO module from `android.card_detection_lite`. Package names
remain `com.apexfission.android.yolo`; the standalone module is `:yolo` and demo module is `:app`.
Use [IMPORT.md](../../IMPORT.md) for confirmed coordinates, rather than guessing versions.
Remove duplicated source modules when adopting Maven to avoid duplicate classes.
The geometry dependency is transitive; a sibling coordinates checkout is no longer required.

Prefer `buildYoloDetector` / `buildInferenceEngine`. Both delegate to their explicitly
thread-confined counterparts. Do not access internal `InferenceCore`, `LetterboxBuilder`,
`YoloNms`, model loader, or `Tensor.toMetadata` from consumers.

`LetterboxResult` has a six-field primary constructor carrying actual source dimensions;
the four-argument compatibility constructor infers dimensions from scale/padding. Supply actual
dimensions when known. Low-level pixel-coordinate output requires `OutputScalingMode.NONE`.

The website describes main at its displayed build commit. The confirmed release source is
listed independently in IMPORT.md. No compatibility guarantee is inferred from main's build
version or CI run number. Maintainers compare release and main source before claiming a change
is available to Maven consumers. No API or runtime behavior is changed by this documentation work.
