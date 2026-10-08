# Apexfission YOLO

Android YOLO detection with LiteRT/TensorFlow Lite inference, image letterboxing,
tensor validation, and NMS/IoU post-processing.

## Documentation

- **[Human documentation website](https://lambdawalker.github.io/android.apexfission.yolo/)** — installation, integration, model contract, API reference, and troubleshooting. Deployment requires GitHub Pages to use Actions.
- **[AI integration entry](AI_INTEGRATION_GUIDE.md)** — small retrieval map and dedicated Markdown contracts.
- **[IMPORT.md](IMPORT.md)** — generated, confirmed published coordinates; never infer a version from main.
- **[Runnable demo](app/README.md)** — photo inference and deterministic post-processing.
- **[Maintenance](docs/maintenance.md)** and **[coverage](docs/coverage.md)** — validation commands and evidence.

The website separates **main development** from archived release documentation. English and
Spanish guides include a version selector; unavailable or stale translations fall back to
English for the same revision. [IMPORT.md](IMPORT.md) identifies the newest confirmed release
and shows Maven Central, JitPack, or both only when that destination has that version. The public packages remain `com.apexfission.android.yolo`.
The published coordinates dependency is transitive; no sibling checkout is required.

## Project structure

`yolo/` is the published library; `app/` is the runnable Compose demo. `docs/agents/` holds
canonical English integration guides, `docs/es/` holds reviewed Spanish translations,
`docs/releases/history/` preserves release installation records, and `sites/` builds
the versioned human website and raw Markdown. `scripts/` maintains confirmed release metadata. Contributor instructions are in `AGENTS.md`.

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
Python 3.12 with `scripts/requirements-publishing.txt` for release helper checks. No Git submodules are required.

```bash
python3 -m pip install -r scripts/requirements-publishing.txt
./gradlew :yolo:testDebugUnitTest :yolo:lintRelease :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
./gradlew :yolo:assembleRelease
python3 -m unittest discover -s scripts/tests -v
```

On Windows use `gradlew.bat`. The library AAR is in `yolo/build/outputs/aar/` and the
demo APK is `app/build/outputs/apk/debug/app-debug.apk`. The manual **Verify release
tooling (no publication)** action builds both modules and uploads the demo APK.

## Publishing

Run **Publish YOLO library** from Actions on `main`, selecting **maven-central** or
**jitpack**. Both destinations share the library version and immutable source tag;
unchanged release inputs reuse the version when adding another destination. The
demo app is never published. See [release operations](docs/releases.md) for
credentials, provider confirmation, recovery, and automatic documentation updates.

## Library use and lifecycle

Use the released Maven dependency described in [IMPORT.md](IMPORT.md), or depend
on `project(":yolo")` in this checkout. For a source checkout nested in another
project, map its library directory explicitly with
`project(":yolo").projectDir = file("android.apexfission.yolo/yolo")` and provide
compatible plugin/catalog and publication properties in the host build. Opening
this standalone repository is the supported development setup.

The library supplies inference and image processing, not camera screens,
permission handling, tracking UI. Supply model assets and labels in your
application. Close detectors and engines after use; see the runnable
[demo](app/src/main/java/com/apexfission/android/yolo/demo/MainActivity.kt) and
[package guides](yolo/src/main/java/com/apexfission/android/yolo/engine/README.md).

## Origin

Extracted from `android.card_detection_lite` at commit
`933b3263580b5d4831641aeaee5e2caa6477edc1`. Library source, tests, and keep rules
remain intact under `yolo/`. Licensed under Apache 2.0; see [LICENSE](LICENSE).
