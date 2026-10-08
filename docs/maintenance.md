# Maintain the documentation
## Ownership and scope
- Library source and tests own runtime/API facts; `docs/api-excerpts.json` records reviewed source hashes.
- `docs/release.json` owns confirmed publication facts; existing release scripts generate `IMPORT.md`.
- `docs/agents/` owns integration contracts, complete examples, and API reference text.
- `sites/src/content/docs/index.md` owns the human landing page. Build-time transformations turn
  the integration guides into navigable human HTML and publish their canonical raw Markdown.
- Compiled app source owns quickstart and synthetic snippets. Site output is generated and ignored.
- No accepted screenshot set exists; see the explicit decision in the limitations guide.

## Commands
```bash
python3 -m pip install -r scripts/requirements-publishing.txt
python3 scripts/release.py generate
python3 scripts/release.py verify
python3 -m unittest discover -s scripts/tests -v
cd sites
npm ci
npm run check
npm run dev
```
Node 24 is used in CI. `npm run check` tests Markdown transformation, verifies release values,
checks reviewed API input hashes, extracts/synchronizes guides, builds Starlight and Pagefind,
and validates local HTML/raw links, anchors, assets, and project-subpath URLs.
External network availability is not a link-validation gate.

After editing a compiled example, intentionally update its Markdown copy with
`cd sites && node scripts/sync.mjs --update-snippets`, review the diff, and run `npm run check`.
After a library source change, review all associated declarations/contracts and update the
source hash in `docs/api-excerpts.json`; do not merely bless a hash without reviewing behavior.
A new public file must be assigned a guide in that manifest (internal-only files are explicitly excluded).

To check stale-output removal, create a temporary file under `sites/public/agents/`, rerun
`npm run sync`, and confirm it is removed. Keep source guides outside that generated directory.
`DOCS_REF` defaults to the checked-out commit; CI supplies the exact checkout SHA. Source links
are pinned to it. Legacy URLs remain visibly main development docs even if installation metadata
points to an older release. Versioned URLs identify their release source and documentation revision.

## Android checks
```bash
./gradlew :yolo:testDebugUnitTest :yolo:lintRelease :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
./gradlew :app:connectedDebugAndroidTest
```
Gradle needs JDK 21, compilation uses JDK 17, Android SDK 37 is required, and device tests need
API 28+. The first demo build downloads its verified model. Build-only CI does not prove
hardware behavior. The new quickstart helper compiles within the app.

## Hosting and release separation
In repository **Settings → Pages → Build and deployment**, choose **GitHub Actions**.
The `Documentation` workflow validates all pull requests, builds main after pushes, and can
be retried manually. Enable Pages before its first trusted deployment. Expected URL:
https://lambdawalker.github.io/android.apexfission.yolo/

Only the deployment job receives Pages/OIDC write permissions. PR jobs have read-only access.
A shared deployment concurrency group and an exact-main-head check prevent obsolete queued
builds replacing newer main documentation. The workflow also listens for completion of the
existing finalizer and publishing caller (which invokes it as a reusable workflow), because a GITHUB_TOKEN commit does not trigger a normal push workflow.
That event checks out current main and executes no artifact-upload task. Documentation can
be retried without republishing Maven. Existing finalizer locks, delays, and confirmation
behavior are preserved.

For release preflight, run documentation and Android checks against the exact selected release
SHA. Reuse CI evidence only for that SHA and matching inputs. No screenshot evidence is claimed.
Both publication destinations validate the documentation at the selected immutable source
before reserving or publishing a new release. The separate documentation workflow also validates
pull requests and rebuilds the site after confirmed publication. Follow the [release runbook](releases.md)
for publication and recovery. Never advance installation values from a proposed version.


## Versioned guides and Spanish
The catalogs at `/en/` and `/es/` list confirmed releases and explicit development docs.
Each `/en/yolo/<version>/` or `/es/yolo/<version>/` route has matching `raw/*.md` guides.
Language and version selectors preserve the guide when available; otherwise the target index
explains the fallback. Legacy human and agent URLs remain supported as development aliases.

`docs/releases/history/yolo/<version>.json` owns immutable release provenance and destination
facts. `scripts/documentation_history.py export` supplies exact installation text to the site;
old pages never read current installation pointers. Builders require full Git history, read
historical Markdown as data, and execute only current reviewed build tooling. Release 0.1.0
predates the guides: its recorded documentation correction uses the later documentation commit,
whose `yolo/src` tree was reviewed as identical to the release. Example commands use that later
checkout; source links distinguish library provenance from demo/documentation provenance.

`docs/es/` mirrors relative paths under `docs/agents/`. `docs/es/translations.json` maps each
translated path to the SHA-256 of the exact English input bytes. Missing or stale translations
show the English guide from the same release with a visible notice. Preserve code fences and
heading structure; the builder preserves English anchor aliases. Historical translations are
read only from their recorded documentation revision or retained translation tree, never from
the current working translation. Add a translation correction explicitly through archive tooling.

Run `npm run check` after editing guides, translations, navigation, or archive behavior. It retains
the reviewed API hashes and compiled-snippet checks, tests immutable history and translation
fallback, then validates HTML, raw Markdown, selector option destinations, and anchors. Generated
scoped pages, `src/versions.json`, `public/`, and `dist/` must not be committed.

Successful trusted release workflow completions build current main with full history, covering
publication and manual recovery even when a token-generated archive commit emits no push event.
Only main-branch runs from this repository qualify; historical or fork workflow artifacts are
never downloaded or executed. Documentation retries never upload packages.
