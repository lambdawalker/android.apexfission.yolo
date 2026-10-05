# Apexfission YOLO: agent entry
Scope: **main development documentation**. The site stamps its build commit. Consult
[IMPORT.md](../../IMPORT.md) for the independently confirmed published version and source.
Library API source at the initial documentation review matches that confirmed source;
future main changes are not automatically available in the published package.

Android on-device YOLO inference, preprocessing, tensor validation, and class-aware
NMS. No model in the AAR; no camera UI, permission flow, tracker, OCR, or identity verification.

| Task | API / next document |
| --- | --- |
| Detect from an image | `buildYoloDetector`, [quickstart](quickstart.md) |
| Manage worker/resource lifetime | [concepts](concepts.md), `EngineThreadDispatcher` |
| Check model compatibility | [model contract](model-contract.md) |
| Decode custom outputs or pixel coordinates | `YoloPostProcessor`, [recipes](recipes.md) |
| Crop or map coordinates | [image API](api/image.md), [concepts](concepts.md) |
| Inspect exact public signatures | [API index](api.md) |
| Diagnose failure / upgrade | [troubleshooting](troubleshooting.md), [migration](migration.md) |
| Run existing examples | [demo catalog](demos.md) |

Critical invariants: factories and inference block; call off the main thread. Keep input
bitmaps alive until the call returns. Close each detector/engine, then any shared dispatcher.
The host closes every `ImageProxy`. Output boxes are upright input-image pixels, not view
coordinates. The high-level detector assumes normalized center/size outputs. Read
[limitations](limitations.md) before choosing a model. Internal implementations are not APIs.
