<!-- GENERATED FILE. Edit docs/templates/IMPORT.md.template.
Regenerate: ./gradlew generateImportDocs
Released metadata: docs/release.json (written only after public verification).
-->
# Install Apexfission YOLO

**No Maven Central release has been confirmed yet.**

This is the authoritative installation, Maven coordinate, and released-version
reference for humans and AI agents. Read it instead of guessing a version.

Installation snippets will appear here after the first successful publication.
For now, use the [source-module instructions](README.md#use-as-a-source-module).

Add both `google()` and `mavenCentral()` to your Gradle settings repositories.
This is an Android AAR requiring minSdk 28, with JVM 17 bytecode. It uses AGP built-in
Kotlin; use a compatible Kotlin compiler. Maven consumers need
Android AAR support; Gradle with the Android plugin is the supported build path.

The published Apexfission Coordinates library is exposed transitively through
YOLO's public API. You do not need a coordinates source checkout. Supply your own
model and labels, and close detectors/engines when finished.

See [README.md](README.md) for scope and [the release runbook](docs/releases.md)
for publication and recovery.
