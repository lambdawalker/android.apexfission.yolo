#!/usr/bin/env bash
# Only run after release.py confirm. Atomically preserve docs/source provenance.
set -euo pipefail
: "${RELEASE_VERSION:?}" "${SOURCE_SHA:?}"
python3 - "$RELEASE_VERSION" "$SOURCE_SHA" <<'PY'
import json, re, sys
from pathlib import Path
sys.path.insert(0, 'scripts')
import release
release.version_key(sys.argv[1])
assert re.fullmatch('[0-9a-f]{40}', sys.argv[2]), 'Invalid source SHA'
r = json.loads(Path('docs/release.json').read_text())
assert (r['version'], r['source'], r['phase']) == (sys.argv[1], sys.argv[2], 'confirmed-public')
release.documentation(verify=True)
PY
saved=$(mktemp -d)
trap 'rm -rf "$saved"' EXIT
cp IMPORT.md "$saved/IMPORT.md"
cp docs/release.json "$saved/release.json"
git restore IMPORT.md docs/release.json
git fetch origin main --tags
git merge-base --is-ancestor "$SOURCE_SHA" origin/main
# Stop rather than overwrite installation/release tooling changes made in flight.
git diff --exit-code "$SOURCE_SHA" origin/main -- \
  IMPORT.md docs/release.json docs/templates/IMPORT.md.template gradle.properties \
  build.gradle.kts yolo/build.gradle.kts app/build.gradle.kts settings.gradle.kts gradle/libs.versions.toml scripts \
  .github/workflows/publish-yolo.yml .github/workflows/finalize-yolo.yml
pending="release-pending/$RELEASE_VERSION"
uploading="release-uploading/$RELEASE_VERSION"
[[ "$(git rev-parse "$pending^{commit}")" == "$SOURCE_SHA" ]]
[[ "$(git ls-remote --refs origin "refs/tags/$pending" | cut -f1)" == "$(git rev-parse "refs/tags/$pending")" ]]
git switch -C release-docs origin/main
cp "$saved/IMPORT.md" IMPORT.md
cp "$saved/release.json" docs/release.json
git config user.name 'github-actions[bot]'
git config user.email '41898282+github-actions[bot]@users.noreply.github.com'
git add IMPORT.md docs/release.json
if ! git diff --cached --quiet; then
  git commit -m "docs: record confirmed YOLO release $RELEASE_VERSION"
fi
if git show-ref --verify --quiet "refs/tags/v$RELEASE_VERSION"; then
  [[ "$(git rev-parse "v$RELEASE_VERSION^{commit}")" == "$SOURCE_SHA" ]]
else
  git tag -a "v$RELEASE_VERSION" "$SOURCE_SHA" -m "Maven Central YOLO release $RELEASE_VERSION"
fi
updates=(HEAD:refs/heads/main "refs/tags/v$RELEASE_VERSION" ":refs/tags/$pending")
if [[ -n "$(git ls-remote --refs origin "refs/tags/$uploading")" ]]; then
  [[ "$(git rev-parse "$uploading^{commit}")" == "$SOURCE_SHA" ]]
  updates+=(":refs/tags/$uploading")
fi
# No force push, no moving stable tags. A protected branch refusal preserves all refs.
git push --atomic origin "${updates[@]}"
