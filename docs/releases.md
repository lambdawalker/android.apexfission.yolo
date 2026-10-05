# Publishing Apexfission YOLO

## Artifact and owner setup

This repository publishes only the Android **release AAR**, with sources,
documentation JAR, POM, Gradle module metadata, and detached signatures. Debug
variants and application/model assets are not published. The documentation JAR
contains README, license, and package guides, not generated Dokka API docs.

`GROUP` and `POM_ARTIFACT_ID` in `gradle.properties` are authoritative:
`com.apexfission.android:yolo`. The same file pins the public Maven coordinates
dependency. It is exported with compile/API scope, not bundled into the AAR.
The release validator checks that exact transitive dependency.

Before publishing:

1. Create **maven-central** in **lambdawalker/android.apexfission.yolo**.
   Environments and secrets in the coordinates repository do not apply here.
2. Verify ownership of the namespace covering `com.apexfission.android` in
   Central Portal. Configure a Portal user token and a signing key whose public
   key is available to Central.
3. Add these environment secrets:

   | Secret | Value |
   | --- | --- |
   | `MAVEN_CENTRAL_USERNAME` | Portal token username |
   | `MAVEN_CENTRAL_PASSWORD` | Portal token password |
   | `SIGNING_IN_MEMORY_KEY` | Complete ASCII-armored private signing key |
   | `SIGNING_IN_MEMORY_KEY_PASSWORD` | Passphrase for an encrypted key; otherwise omit |

4. Configure desired environment approvals and main-branch restrictions. Use
   existing repository permissions/rules for bot documentation writes and release
   tags. Do not introduce bypass credentials or weaken protection.
5. Choose the first stable version; `initial_version` is required when there is
   no stable history (for example `0.1.0`). No initial release is assumed.

Secrets are checked before reserving a version and again at upload. No values
are printed. The release job disables Gradle caching and exposes credentials
only to the presence-check and publication steps.

## Verification without publishing

The manual **Verify release tooling (no publication)** workflow also runs on
pull requests and pushes to main. Only verification runs automatically;
publishing remains manual. Committing this setup does not start a release. Use JDK 21 for the daemon, JDK 17 for compilation, Android
SDK/platform 37 and Python 3.12. AGP 9.4.1 follows the permissions reference; Gradle remains 9.6.0 and
publishing uses Vanniktech 0.37.0. Both Kotlin Gradle and Compose compiler plugins
are explicitly 2.4.20 to read the published coordinates library metadata. AGP
built-in Kotlin remains enabled; the root buildscript upgrades its compiler. Like the
permissions workflow, CI uses the hosted runner's Android SDK and Gradle setup.
It does not replace command-line tools or explicitly request SDK packages with
sdkmanager; Gradle/AGP resolves required SDK components.

```bash
python3 -m unittest discover -s scripts/tests -v
python3 scripts/release.py verify
bash -n scripts/finalize-release.sh
./gradlew generateImportDocs verifyImportDocs :yolo:testDebugUnitTest :yolo:lintRelease :yolo:assembleRelease :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug :yolo:publishAllPublicationsToVerificationRepository -PreleaseVersion=9.8.7
python3 scripts/release.py check-local --version 9.8.7 --source "$(git rev-parse HEAD)"
```

This writes only a local Maven repository under `build/verification-repository`.
Use a clean tracked worktree. The sentinel version is test data, not a release.
Validation checks POM identity and metadata, the public coordinates dependency,
AAR manifest and nested classes.jar, JVM 17 YOLO classes, sources, documentation,
and Gradle metadata. Instrumented tests still require a device/emulator and are
not run by the release workflow; JVM tests/lint are not proof of device inference.

## Release policy and workflow

Manually run **Publish YOLO library** on main. On the first run enter
`initial_version`; afterwards leave both inputs empty for stable patch increments.
`resume_version` is recovery only and never uploads. Major/minor changes and
prereleases require a reviewed policy change; initial_version cannot override
existing stable history.

The workflow uses the dispatch event's immutable source SHA. It fetches complete
Git history/tags and confirms main ancestry. Stable versions are ordered
numerically. All stable vX.Y.Z tags must exist in Central metadata; Central ahead
of an existing tagged stream stops for provenance reconciliation. Existing Central
history without tags can seed the next patch without fabricated historical tags.
Only an HTTP 404 means absent metadata; network/format errors stop allocation.

After library tests/lint, release assembly, demo APK build/lint, and local publication validation, the workflow
checks secret presence and checks that the proposed version has no public artifacts.
It reserves the version with an annotated **release-pending/X.Y.Z** tag at the
source commit. Its JSON journal records coordinates, version, source, all artifact
SHA-256 hashes, phase, and workflow run ID. A fixed concurrency group serializes
release jobs. Remote markers also guard against manual/rerun duplication.

Before any Central task can upload, a Gradle guard creates
**release-uploading/X.Y.Z**. A second upload attempt is rejected. The actual task
is `:yolo:publishAndReleaseToMavenCentral -PreleaseVersion=X.Y.Z`, not staging-only
publication. Development builds use `0.0.0-SNAPSHOT` and do not require secrets;
Central tasks require a valid stable version, matching reservation, and credentials.
Use the workflow instead of bypassing its journal with manual generic publish tasks.

After plugin completion, bounded polling (up to 40 minutes) verifies the complete
public artifact set against reserved hashes and checks detached signature files.
Central validates signatures during deployment; the helper checks public format
and availability. Partial publication is not success. Plugin output is retained
as an Actions log artifact for 30 days; find the emitted deployment ID there or
inspect Central Portal if the runner loses its connection before capturing it.
The Git journal survives runner/log loss.

Only after public confirmation does the helper write `docs/release.json` and
regenerate `IMPORT.md`. Finalization atomically updates main's generated docs,
tags the original artifact source as vX.Y.Z, and removes both attempt markers.
It never force-pushes main or moves stable tags. No GitHub Release is created. The demo debug APK is uploaded as a workflow artifact
after validation; only the library is published to Maven Central.

## Installation documentation

Edit `docs/templates/IMPORT.md.template`; regenerate with `generateImportDocs`
and check drift with `verifyImportDocs`. These tasks use the last confirmed
`docs/release.json`, not a proposed releaseVersion. Before the first release it
contains null and IMPORT.md explicitly says nothing has been confirmed.
Unknown placeholders fail rendering. Development coordinate changes do not
silently rewrite published identity; verification flags the discrepancy.
Humans and agents must read IMPORT.md for released-version/installation questions.

## Recovery and concurrent changes

Inspect the original workflow log and Central Portal before any retry:

```bash
git fetch origin main --tags
git show release-pending/X.Y.Z
git ls-remote --refs origin 'refs/tags/release-*'
```

- Failure before reservation: fix the cause and start a fresh normal run.
- Reserved but definitely no upload: confirm no deployment/public artifacts exist,
  then clear only that attempt's marker as described below.
- Upload marker exists, timeout, unknown result, or deployment still processing:
  retain both markers. Inspect Portal and wait; never blindly re-upload.
- Partial public artifacts/signatures: retain markers and reconcile the complete
  set with Sonatype. Immutable public artifacts have no automatic rollback.
- Published but docs/tag push failed: use `resume_version=X.Y.Z`, initial_version
  empty. Recovery checks out the recorded source, skips builds/reservation/upload,
  verifies the original hashes, and retries finalization only.
- Already finalized current release: repeated recovery verifies it and exits
  without new commits or uploads. Conflicting stable-tag provenance stops recovery.

For a **definitively failed** attempt, establish that no deployment can still
publish and no artifacts are public. Only then delete the exact failed markers:

```bash
git push --atomic origin :refs/tags/release-pending/X.Y.Z :refs/tags/release-uploading/X.Y.Z
```

Omit the uploading deletion if that marker was never created. Remove matching
local tags if present before retrying locally. Never delete stable tags, use a
wildcard, or reuse already published coordinates.

Finalization fetches latest main and preserves unrelated work. Concurrent changes
to build/installation/release inputs stop finalization for reconciliation.
Fast-forward races or branch-protection refusal fail the atomic push, retaining
pending state. Do not force main back to the artifact source.

If protection requires PRs, an authorized maintainer can run confirmation at the
recorded source and submit only IMPORT.md and docs/release.json through the
normal PR process. After merge, verify those files match the confirmed journal,
then atomically create the stable tag at the original source and delete the exact
attempt markers without updating main. Automated finalization deliberately stops
on these manual installation edits; finish bookkeeping under existing rules.
No new upload is involved.

There is no site workflow. If added later, use a trusted successful workflow_run
or another explicit trigger: GITHUB_TOKEN docs commits do not normally trigger
push workflows.

## References

- [Vanniktech publication configuration](https://vanniktech.github.io/gradle-maven-publish-plugin/what/)
- [Vanniktech Central publishing](https://vanniktech.github.io/gradle-maven-publish-plugin/central/)
- [Central requirements](https://central.sonatype.org/publish/requirements/)
- [Coordinates installation reference](https://github.com/lambdawalker/android.apexfission.math.coordinates/blob/main/IMPORT.md)
