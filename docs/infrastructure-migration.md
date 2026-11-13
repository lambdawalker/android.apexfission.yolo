# Shared release infrastructure migration

Use setup CLI and Gradle plugin v0.1.20. The plugin owns publication configuration and guards; the CLI owns bootstrap, workflow generation, journals, confirmation and finalization. android-repo-docs continues to own documentation data and invoke the existing Astro renderer.

The migration removes the local Python release engines and their duplicated tests. Confirmed records, historical tags, public coordinates, runtime source and toolchain versions are preserved. The root project name now matches the repository so JitPack coordinates remain unchanged.

Run `sh ./setup.sh setup check`, `sh ./setup.sh module_release verify`, and `sh ./docs.sh build --check`. Generated workflows must be changed through setup configuration and regenerated. There are no Python files in this repository; the shared CLI still generates inline Python in its update-workflows action, which needs an upstream replacement.

For pre-migration pending releases, the shared verifier requires the verification contract at the reserved source. Recover those using the original source tooling and a verified confirmation record; never re-upload or discard their journals.

The Coordinates dependency is checked exactly in `.project/setup/config.yaml`; update that contract together with `COORDINATES_DEPENDENCY`.
