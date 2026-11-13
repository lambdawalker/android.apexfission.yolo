# Yolo documentation site

The pinned `android-repo-docs` CLI owns Astro/Starlight, Markdown preparation,
immutable archives, selectors, search, raw mirrors and validation. Node 24 is required.
The CLI installs its own locked frontend in a temporary application; this repository
has no frontend npm dependency manifest.

From the repository root:

```sh
sh ./docs.sh bootstrap check
sh ./docs.sh check
sh ./docs.sh history verify
sh ./docs.sh build --check
node --test sites/tests/*.test.mjs
sh ./docs.sh serve
```

Output is `sites/dist/`, hosted at `/android.apexfission.yolo/`. Full Git history is required.
The project overview is retained with a legacy `/overview/` alias.
Trusted current presentation lives in `sites/astro/`: project styles, assets
and legacy route redirects. Operational legacy
pages redirect to their configured shared documentation scopes. Never execute historical site code.
No accepted screenshot evidence is claimed by this migration.

Canonical English remains in `docs/agents/`; translations and reviewed hashes remain
in `docs/es/`. Release source, documentation revision, retained translation tree and
current builder identities remain separate. Exact historical installation facts come
from `docs/releases/history/yolo/`, never from current installation pointers.

## Migration acceptance

The CLI is pinned to corrective source commit `1da53392b262c7c3129d2bd645dfd7b34ead81e1`, beyond the official v0.1.21 release. Launchers build and cache that full SHA. Manual documentation-tool installation uses the same source pin:

```sh
cargo install --locked --git https://github.com/lambdawalker/gradle.android-repo-setup.git --rev 1da53392b262c7c3129d2bd645dfd7b34ead81e1 android-repo-docs
```

The corrective commit preserves
postrelease documentation/example source links, bilingual correction notices,
current root agent discovery, and clean builds across filesystem boundaries.
`sites/tests/` exercises real HTML/raw output, scoped language selectors, legacy
aliases, exact installation records and both source identities. The shared CLI
owns generic historical staging and Markdown transformation tests.
