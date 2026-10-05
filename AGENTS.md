# Contributor instructions
Consumer integration contracts start at [docs/agents/index.md](docs/agents/index.md).
Read source before changing contracts. Keep runtime changes separate from documentation work.

- `IMPORT.md` is generated: edit `docs/templates/IMPORT.md.template`, then run `python3 scripts/release.py generate`.
- Never advance `docs/release.json` without the existing public-registry confirmation workflow.
- After documentation edits run `cd sites && npm ci && npm run check`.
- API declarations are reviewed against source hashes in `docs/api-excerpts.json`; builds fail if an input changes.
- Maintain `docs/coverage.md` when public capabilities change.
- Do not commit `sites/dist`, `sites/public`, or generated site content.
- Android checks and publishing recovery commands are in `docs/maintenance.md` and `docs/releases.md`.
