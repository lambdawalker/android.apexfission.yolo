# Public API retrieval map
Scope: main source. API pages contain declaration references with actual defaults and focused
contracts. They omit bodies/annotations and are not standalone compilable Kotlin files.
Source hashes in `docs/api-excerpts.json` fail builds when references need review.

| Subsystem | Reference |
| --- | --- |
| Detector factories, overloads, result types, threading choices | [Detector](api/detector.md) |
| Inference factories, metadata, confinement and disposal | [Inference](api/inference.md) |
| Tensor decoding, NMS settings, IoU | [Post-processing](api/postprocess.md) |
| Seven image modes, crop/rotation helpers and CameraX conversion | [Images](api/image.md) |
| Input/output buffers, conversion functions, tensor validation | [Buffers and validation](api/buffers-validation.md) |

Public does not mean every entry is the preferred integration layer. Prefer factories; lower-level
helpers require explicit ownership and consistent metadata. Internal `InferenceCore`,
`LetterboxBuilder`, `YoloNms`, `loadModelFile`, `Tensor.toMetadata`, and dispatcher internals are
not consumer APIs. `ImageBox` belongs to the transitive coordinates dependency.
