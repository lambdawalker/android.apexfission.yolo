# YOLO demo app

This app consumes the local `:yolo` module. It is not a Maven publication.

## Inference demo

Choose a photo using Android's document picker. ImageDecoder handles orientation
and downsamples the longest edge to 1600 pixels to bound memory. The demo uses
`buildYoloDetector` with score threshold 0.35, IoU threshold 0.45, and CPU by default.
GPU is optional. The detector's factory handles native thread confinement; model
creation, inference, and close run off the UI thread. Each operation uses `use`
to close the detector even on failure. Activity destruction cancels UI work;
a synchronous native call finishes and closes before its worker exits.

The model is designed for cards, photos, barcodes, and MRZ regions. Arbitrary
photos may produce no detections. It does not establish document authenticity.
Photos are neither saved nor uploaded by this app. Displayed images are owned by
the activity and released by Android garbage collection when no longer referenced.

## Model provenance

- Source repository: `lambdawalker/android.card_detection_lite`
- Source commit: `115571941f890c4de0324cd008cff31a2068e722`
- Source path: `tfmodel/src/main/assets/cdl/tflite/Y11-640E197F16.tflite`
- SHA-256: `473f497ca45b4ab2f66ef59b3da1c0e27a0ac11227279ba6d89a19566c450e1e`
- Labels: copied from that commit's `tfmodel/.../add.kt` catalog.

`:app:prepareDemoModel` downloads and verifies the pinned model into
`app/build/generated/demoAssets/demo.tflite`. It runs before app builds, requires
network access the first time (and after clean), and fails if download or checksum
verification fails. Gradle tracks the generated output for incremental builds.
The asset is stored uncompressed so the library can memory-map it. No binary
model is added to library sources or the Maven artifact.

## Post-processing example and device checks

**Test post-processing** calls `YoloPostProcessor` with three known candidate
boxes. Two overlap; NMS should retain two final boxes. The example is labeled
synthetic and does not invoke the model.

With an attached emulator/device:

```bash
./gradlew :app:connectedDebugAndroidTest
```

Device tests check the deterministic NMS result and initialize/run/close the
bundled model on CPU. They require a device and are separate from the build-only
release checks. Use real sample photos for model accuracy and GPU validation.
