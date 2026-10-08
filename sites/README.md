# YOLO documentation site
Astro/Starlight, independent of Android runtime dependencies.

From this `sites/` directory (Node 24 and Python 3.12):

```bash
python3 -m pip install -r ../scripts/requirements-publishing.txt
npm ci
npm run check
npm run dev
```
Preview at `http://localhost:4321/android.apexfission.yolo/`. Production output is `dist/`.
Raw files: `agents/index.md`, nested `agents/api/*.md`, `IMPORT.md`, and `llms.txt`.
See [maintenance](../docs/maintenance.md) for ownership, regeneration, Pages setup and CI.

Versioned HTML lives at `{en,es}/yolo/{version}/`; the explicit `development` version
uses current guides. Every scope publishes matching `raw/*.md` files and an installation page.
`versions.json` records the available guide routes and provenance. Archived guides are read
from immutable Git revisions by current tooling; historical build scripts never execute.
Full Git history is required. See the maintenance guide for translation and archive rules.
