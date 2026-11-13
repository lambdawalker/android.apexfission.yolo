# Contributor instructions
Consumer integration contracts start at [docs/agents/index.md](docs/agents/index.md).
Read source before changing contracts. Keep runtime changes separate from documentation work.

- `IMPORT.md` is generated: edit `docs/templates/IMPORT.md.template`, then run `sh ./setup.sh module_release generate`.
- Preserve legacy `docs/release.json` as release evidence. Only provider-confirmed publication updates `docs/releases/` and `IMPORT.md`; never infer availability from a tag.
- Archive immutable installation history under `docs/releases/history/yolo/`; never replace a version's source identity.
- English guides are canonical in `docs/agents/`. Maintain Spanish guides in `docs/es/` and their English SHA-256 entries in `translations.json`; stale translations must fall back to the same revision in English.
- After documentation edits run `sh ./docs.sh build --check && node --test sites/tests/*.test.mjs`.
- API declarations are reviewed against source hashes in `docs/api-excerpts.json`; builds fail if an input changes.
- Maintain `docs/coverage.md` when public capabilities change.
- Do not commit `sites/dist`, `sites/public`, or generated site content.
- Android checks and publishing recovery commands are in `docs/maintenance.md` and `docs/releases.md`.
