# Publishing YOLO

[IMPORT.md](../IMPORT.md) is the authoritative latest confirmed installation reference. The exact-version website archive provides historical coordinates and matching guides. Never infer availability from a tag, proposed version, or successful local build.

## Identity and migration

The single `yolo` library retains Central coordinates `com.apexfission.android:yolo`. The app is never published. `publishing/repositories.yml` is the destination registry; run `python3 scripts/publishing_config.py generate` after editing it, then commit its generated properties and workflow dropdowns.

Central and JitPack share one semantic version and original source identity. An unchanged release reuses the highest reserved version and its original commit, even when the release branch advances with generated documentation, website or translation changes. Changed library sources, packaged English documentation, dependencies, shared build inputs or release protocol advance the global patch. An explicit higher stable version permits intentional major/minor releases. Versions use numeric ordering; `0.2.10` follows `0.2.9`.

Canonical tags are `yolo/vX.Y.Z`. Older `vX.Y.Z` and destination-scoped tags remain immutable provenance inputs; conflicting sources for one version block publication. Pending identities also participate in allocation across destinations. Never move a tag to change a build.

The original Central **0.1.0** remains confirmed at source `76abd72e4e00b2b0ed316e333aff35f6fa989a91`, with immutable tag `v0.1.0` and the hashes in `docs/release.json`. Its migration record uses `legacy_tag: v0.1.0`; the archive preserves the same publication facts. No new tag or JitPack availability is claimed. `docs/release.json` is retained as legacy evidence; new confirmations update per-destination module records. Unresolved legacy `release-pending/X.Y.Z` attempts block new releases and are recovered with **Finalize YOLO release**, choosing `maven-central`, `yolo`, and the original version. The trusted current finalizer validates the original annotated journal, immutable source, hashes and signatures without re-uploading or checking out obsolete release scripts. It preserves the original `vX.Y.Z` tag and removes only the matching legacy attempt markers in the atomic finalization push.

## Owner setup

Existing Central credentials stay in the **maven-central** GitHub environment:

- `MAVEN_CENTRAL_USERNAME` and `MAVEN_CENTRAL_PASSWORD`: Central Portal user token credentials.
- `SIGNING_IN_MEMORY_KEY`: complete armored private signing key, including newlines.
- `SIGNING_IN_MEMORY_KEY_PASSWORD`: passphrase when the key is encrypted.

The existing Central endpoint is the default; no new endpoint or credential is needed. Keep the namespace verified in Central Portal.

Create the empty **jitpack** environment for public builds; optional protection rules are a maintainer choice. Public JitPack requires no token, password, signing key, or custom endpoint. Credentials are absent from its build job.

Create **delayed-docs** with a **15-minute wait timer**, no secrets, and no required reviewers if automatic continuation is desired. Merely referencing this environment in YAML does not configure its timer. GitHub plan and repository visibility determine timer availability; verify the repository's current support in Settings. Permit `main` under deployment restrictions. The wait job holds no release lock or active runner during the environment gate.

Allow the Actions bot the necessary contents/tag writes and normal protected-branch publication process. No bypass token is introduced. Settings, timers, credentials, Central access, and the first live JitPack build have **not** been inspected or configured by these local changes.

## Ordinary release

1. Run **Publish YOLO library** on `main`.
2. Select `maven-central` or `jitpack`; leave `version` empty for reuse/next patch.
3. Review job output for the semantic version, exact source SHA, destination and eventual confirmation.

The jobs share `maven-central-yolo` concurrency with both finalization paths, without nested locks. An already-confirmed destination/version is a no-op. Preparation validates source history and public metadata before upload. Local validation tests the library, demo, documentation, and validates an unsigned complete publication in `build/verification-repository`.

Central reserves the canonical source tag plus an annotated `release-pending/yolo/X.Y.Z` journal atomically before remote upload. The journal records source, coordinates, all expected artifact SHA-256 hashes and workflow run. The Gradle upload guard creates `release-uploading/yolo/X.Y.Z` before allowing `publishAndReleaseToMavenCentral`; a second invocation is rejected. The deployment log is retained for Portal investigation. Successful upload runs the external environment delay, then public verification polls up to 40 minutes in a separate finalizer with a 50-minute timeout.

JitPack reserves `release-pending/jitpack/yolo/X.Y.Z` and the same canonical tag, then requests the public artifact to initiate a provider build. `jitpack.yml` calls `scripts/jitpack_build.py`, which accepts only a canonical stable tag resolving to HEAD and checks the provider commit. It installs only `:yolo:publishToMavenLocal`, unsigned. Gradle rejects app/sibling and remote-upload task selection. The Java 21 daemon and Java 17 library bytecode policy are preserved. JitPack bootstraps with Java 17, then explicitly installs and selects Temurin `21.0.10-tem` through SDKMAN before running Gradle. The `jdk` list is not a toolchain installation matrix; listing both versions did not install Java 21. Keep the SDKMAN selection aligned with `gradle/gradle-daemon-jvm.properties`. This avoids relying on its remote Foojay download URL in the JitPack build image.

JitPack consumers use `com.github.lambdawalker:android.apexfission.yolo:yolo~vX.Y.Z`, not Central's artifact or raw semantic version. This is the selected single-publication layout and must be confirmed in the first live build. No JitPack version is advertised before confirmation.

## Confirmation and documentation

Central requires matching POM, AAR, sources, documentation and Gradle metadata, expected hashes and detached signatures. Both POM compile scope and Gradle API metadata must export the explicit Coordinates dependency from the reserved source commit, even if the current branch has upgraded it. JitPack requires successful provider status at the exact full reserved commit, correct public coordinates, valid AAR/JVM target and public dependency, required sources/documentation, and any available Gradle metadata. Source and documentation archive-entry hashes must match the reservation; independently rebuilt ZIP hashes are recorded only after remote verification, not assumed equal to local ZIPs. Queued/building states, missing artifacts, rate limits and transient failures retry within the budget; contradictory provenance and failed builds stop.

Provider-reported provenance is trusted within that service's limits; public availability alone does not prove who performed a Central upload. Keep Portal deployment logs and association with the reserved source. Source tags remain immutable in this repository, but that is not a claim of immediate external JitPack artifact immutability or retention. Inspect current provider policy and the build log before recovery; never force a rebuild or delete a provider build automatically.

Only confirmation advances `docs/releases/yolo.json` or `docs/releases/jitpack/yolo.json`. Finalization preserves each confirmation in `docs/releases/history/yolo/X.Y.Z.json` before regenerating IMPORT. Same-version destination catch-up adds only matching source identities. Archive entries retain their immutable documentation revision independently of later translations or destination availability.

Edit `docs/templates/IMPORT.md.template`; run `./gradlew generateImportDocs verifyImportDocs` or `python3 scripts/module_release.py generate` and `verify`. There is no version override that can announce an unconfirmed release. IMPORT selects the highest confirmed version and only destinations confirming that exact source/version. History generation excludes archive files from latest-pointer and allocation discovery.

Finalization runs trusted current tooling from `main`, fetches current `main`, preserves unrelated concurrent changes and newer build inputs, and stops if its executing release protocol/renderer changed during execution. One atomic push commits intended metadata/IMPORT/archive changes and removes matching attempt markers. The canonical tag identifies the original artifact source, not the later docs commit. It never force-pushes or moves tags.

The Pages workflow watches the successful trusted top-level publication and recovery workflows, checks out latest release-branch metadata, rebuilds all cataloged versions and deploys under its existing Pages policy. `GITHUB_TOKEN` bot commits alone cannot be assumed to start ordinary push workflows. Manual documentation-only retry remains available and cannot publish a package.

## Recovery

Run **Finalize YOLO release** on `main`, choosing the original destination, `yolo`, and reserved semantic version. This action allocates no identity and performs no Maven upload. It rechecks state under the same lock, verifies original public artifacts and retries bookkeeping. Manual recovery can complete during the environment wait; the delayed automatic finalizer then safely becomes a no-op. Requests for older completed releases do not replace newer documentation.

For JitPack, reading an uncached artifact may request the same tagged provider build. Recovery does not invoke force-rebuild/delete APIs or move the source tag. Inspect `https://jitpack.io/#lambdawalker/android.apexfission.yolo` and the selected tag's build log.

- Validation failure before reservation: fix inputs and rerun; no remote attempt was made.
- Unknown upload outcome, timeouts or runner loss: retain pending/uploading tags, inspect Portal/build logs, then finalize the same version. Never blindly re-upload immutable coordinates.
- Still processing or partial artifact visibility: retain reservations, allow propagation, and retry finalization. A subset never counts as complete; there is no automatic public-artifact rollback.
- Definitive rejection: first prove the deployment/build cannot become public and reconcile every intended artifact. Only then may a maintainer remove exactly the failed attempt references. Canonical source tags remain immutable and remain version-allocation inputs.
- Render, tag, push or branch-protection failure: resolve the specific blocker, then finalize without upload. The atomic push prevents partially committed bookkeeping.
- Source/tag conflicts or changed release protocol: reconcile the recorded immutable identity and executing tooling; do not delete history to bypass validation.

## Offline verification

```bash
python3 -m pip install -r scripts/requirements-publishing.txt
python3 -m unittest discover -s scripts/tests -v
python3 scripts/publishing_config.py check
python3 scripts/module_release.py verify
python3 scripts/documentation_history.py verify
bash -n scripts/finalize-release.sh
./gradlew :yolo:testDebugUnitTest :yolo:publishAllPublicationsToVerificationRepository -PreleaseModule=yolo -PreleaseVersion=9.8.7
RELEASE_REPOSITORY=jitpack ./gradlew :yolo:publishAllPublicationsToVerificationRepository -PjitpackBuild=true -PreleaseModule=yolo -PreleaseVersion=9.8.7
```

The verification workflow checks both selected-module destination layouts without signing or remote upload. Local fixture and temporary-Git tests cover ordering, source reuse, changed inputs, pending conflicts, archive catch-up, provider provenance, partial artifacts, timeout, durable upload guards, concurrent finalization and repeated recovery. Full Android/Gradle checks require the pinned toolchain, network dependencies and Android SDK. Local validation is not evidence of a live publication or Pages deployment.
