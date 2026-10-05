# Apexfission YOLO

Android YOLO detection library with LiteRT/TensorFlow Lite inference, image
letterboxing, tensor validation, and NMS/IoU post-processing. Package names remain
`com.apexfission.android.yolo`.

## Maven installation

Read [IMPORT.md](IMPORT.md) for the authoritative released version, Maven
coordinates, and installation examples. Humans and AI agents should use that
file instead of inferring releases from source. The intended publication is
`com.apexfission.android:yolo`; no YOLO release is claimed until public confirmation.
See the [release runbook](docs/releases.md) for setup and recovery.

YOLO consumes `com.apexfission.android.math:coordinates:0.1.0` from Maven Central.
This is an `api` dependency because public detection types expose geometry types;
consumers receive those classes transitively. The pinned dependency is configured
in `gradle.properties` as `COORDINATES_DEPENDENCY`.

## Build and test

Requires JDK 21 to run Gradle (per the daemon configuration), a JDK 17 compilation
toolchain, and Android SDK 37. No Git submodule initialization is required.

```bash
git clone https://github.com/lambdawalker/android.apexfission.yolo.git
cd android.apexfission.yolo
./gradlew assembleRelease testReleaseUnitTest lintRelease
```

On Windows use `gradlew.bat`. Minimum Android API: 28. Release AARs are generated
in `build/outputs/aar/`. Models and class labels must be supplied by the consuming
application; this repository does not bundle model assets.

## Use as a source module

Add this repository as a `yolo/` source directory or Git submodule. The host must
provide Google and Maven Central repositories and the existing dependency/plugin
aliases in `gradle/libs.versions.toml`:

```kotlin
include(":yolo")
```

Consumers use `implementation(project(":yolo"))`. Coordinates resolves from Maven
Central; do not also include its sources when consuming this version of YOLO, as
that would duplicate the geometry classes. Publication settings are read from
this module's own `gradle.properties`.

## Scope and lifecycle

The library provides the detector and inference pipeline, not camera screens,
permission handling, tracking UI, or OCR. Close detectors and inference engines
when finished to release native resources. See package documentation under
`src/main/java/com/apexfission/android/yolo/` for implementation details.

## Origin

Extracted from `android.card_detection_lite` at commit
`933b3263580b5d4831641aeaee5e2caa6477edc1`. Kotlin source, tests, resources, and keep
rules are preserved. Coordinates is now consumed as a released Maven dependency.
Licensed under Apache 2.0; see [LICENSE](LICENSE).
