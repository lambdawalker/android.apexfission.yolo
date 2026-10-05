# Runnable demos
The app consumes `implementation(project(":yolo"))`, never the released AAR. Examples are
main development examples, pinned to the website build commit when published.

```bash
./gradlew :app:assembleDebug :app:installDebug
adb shell am start -n com.apexfission.android.yolo.demo/.MainActivity
```

Requires JDK 21 for Gradle, JDK 17 for compilation, Android SDK 37, and a device/emulator
API 28+. First build downloads a pinned model and verifies its SHA-256. Open/run **app** in
Android Studio as an alternative. Full [activity source](../../app/src/main/java/com/apexfission/android/yolo/demo/MainActivity.kt).

| Route | Action and expected result | Recovery / limits |
| --- | --- | --- |
| Choose photo | Select a card image; see boxes, labels, scores and total time including initialization | Zero detections is valid; retry another photo; decode/model errors shown as status |
| Request GPU + Choose photo | Run the same image with requested acceleration | Device dependent; switch off GPU and retry; switch is not proof GPU was used |
| Test post-processing | Three synthetic candidates produce two displayed boxes | Deterministic NMS example, no model inference or accuracy claim |

The photo app uses Android's picker with no camera/storage/network runtime permissions. It
keeps photos on-device and does not establish document authenticity. Labels describe card,
photo, barcode, and MRZ regions, not arbitrary objects.

The deterministic example's [source](../../app/src/main/java/com/apexfission/android/yolo/demo/DemoExamples.kt)
and the [compiled quickstart](../../app/src/main/java/com/apexfission/android/yolo/demo/DocumentationQuickstart.kt)
are extracted into this site's guides at build time.

## Model provenance
Pinned source commit: `115571941f890c4de0324cd008cff31a2068e722` in
`lambdawalker/android.card_detection_lite`, path
`tfmodel/src/main/assets/cdl/tflite/Y11-640E197F16.tflite`.
SHA-256: `473f497ca45b4ab2f66ef59b3da1c0e27a0ac11227279ba6d89a19566c450e1e`.
Gradle's variant model-preparation task fails on missing download or checksum mismatch.
Model binaries are never shipped in the Maven AAR.

## Verification
```bash
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
./gradlew :app:connectedDebugAndroidTest
```
The device tests check deterministic NMS and CPU initialization/inference/close of the model.
These checks do not substitute for real-image accuracy, GPU-device, or camera integration tests.
No new demo screenshots have been captured; see [screenshot policy](limitations.md#screenshots).
