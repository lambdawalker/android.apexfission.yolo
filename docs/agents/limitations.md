# Limitations and non-goals
- Android API 28+ only; no JVM desktop, iOS, JavaScript, or server runtime contract.
- No trained model or labels in the library AAR. Bundled demo weights are separate assets.
- Only the [tensor contract](model-contract.md) is supported; not every YOLO export is compatible.
- No camera screen, permission manager, tracker, OCR, passkey, or identity/authenticity verification.
- No automatic lifecycle binding, asynchronous cancellation, coroutine adapter, or preview transform.
- GPU acceleration is conditional and has no reported active-backend flag or guaranteed fallback
  after interpreter initialization failure.
- The NMS cap limits returned results; many input boxes can still cost significant CPU time.
- Public low-level buffers and post-processors require consistent sizes and serialized access.
- Crop helpers use system density for Dp offsets; provide positive source/canvas dimensions.
  `centerCropSquare(maxSize=...)` limits the output at the selected crop origin, not a new centered
  smaller square. Some crop/rotate results alias their input.
- Shape checks do not validate training labels, calibration, accuracy, fairness, or document validity.

## Screenshots
The shipped library owns no UI. The existing app is a useful interactive visualization, but
no reviewed device screenshot set exists. This documentation change does not manufacture images
or imply that static diagrams prove GPU, camera, or lifecycle behavior. Device capture is blocked
in the authoring environment by the absent Android SDK/emulator. The text walkthrough and actual
source remain the evidence. A future gallery must capture the actual app, record device/API,
viewport/theme/locale/source hashes, and separate candidate rendering, explicit baseline updates,
and validation. Until then no screenshot check or release screenshot claim is made.
