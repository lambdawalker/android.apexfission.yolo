# Apexfission YOLO

Android YOLO detection library with LiteRT/TensorFlow Lite inference, image letterboxing, tensor validation, and NMS/IoU post-processing. Package names remain `com.apexfission.android.yolo`.

## Build and test

Requires JDK 21 to run Gradle (per the daemon configuration), a JDK 17 compilation toolchain, and Android SDK 37. Clone with dependencies:

```bash
git clone --recurse-submodules https://github.com/lambdawalker/android.apexfission.yolo.git
cd android.apexfission.yolo
./gradlew assembleDebug testDebugUnitTest
```

On Windows use `gradlew.bat`. Minimum Android API: 28. The AAR is generated in `build/outputs/aar/`. Models and class labels must be supplied by the consuming application; this repository does not bundle model assets.

## Use as a source module

Add this repository as a `yolo/` Git submodule. The host must include both `:yolo` and `:coordinates`, provide Google and Maven Central repositories, and supply the aliases in `gradle/libs.versions.toml`. If the host does not already have coordinates, map it to the nested checkout:

```kotlin
include(":yolo", ":coordinates")
project(":coordinates").projectDir = file("yolo/coordinates")
```

Consumers use `implementation(project(":yolo"))`. Consumers using geometry types directly should also depend on `project(":coordinates")`. Initialize dependencies with `git submodule update --init --recursive`.

## Scope and lifecycle

The library provides the detector and inference pipeline, not camera screens, permission handling, tracking UI, or OCR. Close detectors and inference engines when finished to release native resources. See the package documentation under `src/main/java/com/apexfission/android/yolo/` for implementation details.

## Origin

Extracted from `android.card_detection_lite` at commit `933b3263580b5d4831641aeaee5e2caa6477edc1`. Kotlin source, tests, resources, and keep rules are preserved unchanged. Coordinates is pinned to the same revision used by that source repository. Licensed under Apache 2.0; see [LICENSE](LICENSE).
