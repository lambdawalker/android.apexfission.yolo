# Release operations

The shared `android-repo-setup` CLI and Gradle plugin own release operations. Both are pinned to v0.1.20. The `yolo` module publishes; `app` never publishes. Central coordinates remain `com.apexfission.android:yolo`; JitPack uses `com.github.lambdawalker:android.apexfission.yolo:yolo~vX.Y.Z-jitpack` after provider confirmation.

## Configuration and checks

`.project/setup/config.yaml` owns module selection, verification contracts and workflow inputs. `.project/setup/repositories.yml` owns destination environments. Use the launchers from the repository root:

```sh
sh ./setup.sh workflows --overwrite
sh ./setup.sh publishing_config generate
sh ./setup.sh bootstrap generate --overwrite
sh ./setup.sh workflows --check
sh ./setup.sh publishing_config check
sh ./setup.sh module_release verify
sh ./docs.sh history verify
```

The generated workflow manifest owns generated workflows. Change configuration or upstream templates, then regenerate. Do not recreate local release engines.

## Publishing and recovery

Use **Publish yolo** to select the destination. A new version reserves one immutable source/build-input identity across destinations. Confirmed history and legacy provider-specific identities are preserved. Uncertain attempts remain pending; only provider evidence can establish failure. Shared workflow concurrency serializes repository release changes.

Central requires the configured Maven credentials and signing key. The Gradle guard validates the reservation before upload; receipt tracking and reconciliation distinguish successful, failed and uncertain uploads. JitPack runs `sh ./setup.sh jitpack-build`, validates its source tag and commit, and publishes the selected module locally without signing. The checked-in JDK bootstrap retains Java 21 for Gradle and Java 17 library bytecode.

Use **Finalize release** for a reserved publication awaiting public confirmation. Do not upload the same Central version again, delete pending markers, or infer success from a tag. See the [shared recovery rules](https://github.com/lambdawalker/gradle.android-repo-setup/blob/main/docs/release-reconciliation.md).

Pre-migration reservations lack a committed shared verification contract. Verify those with the original tooling at their reserved source and use the current finalizer with the verified confirmation record. Never replace immutable source expectations with current configuration.

## Documentation and local verification

Only confirmed public artifacts advance installation records. Keep `docs/release.json`, per-destination records and `docs/releases/history/` unchanged during infrastructure migration. Edit `docs/templates/IMPORT.md.template`, then run `./gradlew generateImportDocs verifyImportDocs`.

```sh
./gradlew :yolo:testDebugUnitTest :yolo:publishAllPublicationsToVerificationRepository -PreleaseModule=yolo -PreleaseVersion=9.8.7
RELEASE_REPOSITORY=jitpack ./gradlew :yolo:publishAllPublicationsToVerificationRepository -PjitpackBuild=true -PjitpackScopedTag=true -PreleaseModule=yolo -PreleaseVersion=9.8.7
```

The generated release-tooling workflow verifies both destinations without uploading. Exact Coordinates dependency requirements live in setup configuration and must change together with `COORDINATES_DEPENDENCY`. No runtime library version is inferred from this tooling version.
