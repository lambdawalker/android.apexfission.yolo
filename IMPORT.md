<!-- GENERATED FILE. Edit docs/templates/IMPORT.md.template.
Regenerate: ./gradlew generateImportDocs
Released metadata: docs/release.json (written only after public verification).
-->
# Install Apexfission YOLO

Confirmed release: **0.1.0** · Maven coordinates: `com.apexfission.android:yolo:0.1.0`.

This is the authoritative installation, Maven coordinate, and released-version
reference for humans and AI agents. Read it instead of guessing a version.

## Gradle Kotlin DSL

Add `mavenCentral()` to your settings repositories, then:

```kotlin
dependencies {
    implementation("com.apexfission.android:yolo:0.1.0")
}
```

## Gradle Groovy DSL

```groovy
dependencies {
    implementation 'com.apexfission.android:yolo:0.1.0'
}
```

## Version catalog

```toml
[versions]
apexfission-yolo = "0.1.0"

[libraries]
apexfission-yolo = { module = "com.apexfission.android:yolo", version.ref = "apexfission-yolo" }
```

```kotlin
implementation(libs.apexfission.yolo)
```

## Maven

```xml
<dependency>
    <groupId>com.apexfission.android</groupId>
    <artifactId>yolo</artifactId>
    <version>0.1.0</version>
    <type>aar</type>
</dependency>
```

Built from source commit [`76abd72e4e00b2b0ed316e333aff35f6fa989a91`](https://github.com/lambdawalker/android.apexfission.yolo/commit/76abd72e4e00b2b0ed316e333aff35f6fa989a91).

Add both `google()` and `mavenCentral()` to your Gradle settings repositories.
This is an Android AAR requiring minSdk 28, with JVM 17 bytecode. It is built
with Kotlin 2.4.20; use a compatible Kotlin compiler. Maven consumers need
Android AAR support; Gradle with the Android plugin is the supported build path.

The published Apexfission Coordinates library is exposed transitively through
YOLO's public API. You do not need a coordinates source checkout. Supply your own
model and labels, and close detectors/engines when finished.

See [README.md](README.md) for scope and [the release runbook](docs/releases.md)
for publication and recovery.
