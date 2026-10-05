# Apexfission YOLO

Android YOLO detection with LiteRT/TensorFlow Lite inference, image letterboxing,
tensor validation, and NMS/IoU post-processing.

## Project structure

Like [Apexfission Permissions](https://github.com/lambdawalker/android.apexfission.permissions),
this project separates its publishable library from its runnable demo:

- **[yolo/](yolo/)** — Android library. Packages remain `com.apexfission.android.yolo`.
- **[app/](app/)** — Compose demo consuming `implementation(project(":yolo"))`.
- **[IMPORT.md](IMPORT.md)** — authoritative Maven coordinates and confirmed release.
- **[docs/releases.md](docs/releases.md)** — publishing and recovery instructions.

The intended publication is `com.apexfission.android:yolo`. No release is claimed
until public confirmation. The library exports
`com.apexfission.android.math:coordinates:0.1.0` through its public API; consumers
receive the geometry classes transitively. Its pin lives in root `gradle.properties`.

## Run the demo

Open this repository in Android Studio and run **app** on Android 9/API 28 or newer.
Select **Choose photo** to test the bundled card model, then view bounding boxes,
class labels, confidence scores, and elapsed time. CPU is the default; the GPU
switch requests acceleration on supported devices. Photos stay on the device and
no camera, storage, or network permission is needed at runtime.

**Test post-processing** passes deterministic tensor data through the library's
public post-processor: three candidates should become two boxes after NMS. It is
explicitly a synthetic example, not a model accuracy test.

The first app build downloads the existing card model from an immutable commit in
`android.card_detection_lite` and checks its SHA-256. Later up-to-date builds reuse
it. See [app/README.md](app/README.md) for model provenance and device tests.
The model and demo are never included in the Maven AAR.

## Build and test

Requires JDK 21 for the Gradle daemon, JDK 17 for compilation, Android SDK 37, and
Python 3.12 for release helper checks. No Git submodules are required.

```bash
./gradlew :yolo:testDebugUnitTest :yolo:lintRelease :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
./gradlew :yolo:assembleRelease
python3 -m unittest discover -s scripts/tests -v
```

On Windows use `gradlew.bat`. The library AAR is in `yolo/build/outputs/aar/` and the
demo APK is `app/build/outputs/apk/debug/app-debug.apk`. The manual **Verify release
tooling (no publication)** action builds both modules and uploads the demo APK.

## Library use and lifecycle

Use the released Maven dependency described in [IMPORT.md](IMPORT.md), or depend
on `project(":yolo")` in this checkout. For a source checkout nested in another
project, map its library directory explicitly with
`project(":yolo").projectDir = file("android.apexfission.yolo/yolo")` and provide
compatible plugin/catalog and publication properties in the host build. Opening
this standalone repository is the supported development setup.

The library supplies inference and image processing, not camera screens,
permission handling, tracking UI, or OCR. Supply model assets and labels in your
application. Close detectors and engines after use; see the runnable
[demo](app/src/main/java/com/apexfission/android/yolo/demo/MainActivity.kt) and
[package guides](yolo/src/main/java/com/apexfission/android/yolo/engine/README.md).

## Origin

Extracted from `android.card_detection_lite` at commit
`933b3263580b5d4831641aeaee5e2caa6477edc1`. Library source, tests, and keep rules
remain intact under `yolo/`. Licensed under Apache 2.0; see [LICENSE](LICENSE).
