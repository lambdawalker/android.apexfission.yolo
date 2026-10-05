# Troubleshooting
| Symptom | Likely cause and correction |
| --- | --- |
| Asset open/mapping error | Put model in app assets, check exact path/case, mark tflite uncompressed |
| Unsupported tensor contract | Inspect actual shapes, types, scales; re-export to the supported contract |
| Boxes huge, misplaced, or empty | Check normalized versus pixel output, letterbox metadata, class order |
| Boxes offset on preview | Apply crop offsets, orientation, scale, padding, and mirroring exactly once |
| UI freezes | Factories, inference and close block; call from host worker execution |
| Camera stops delivering frames | Close every ImageProxy in finally, including failure/disabled paths |
| GPU failure | Retry construction explicitly with useGpu=false; verify hardware on device |
| Empty output | Model may find nothing, threshold too high, wrong input, disabled/closed detector |
| Bitmap recycled / pixel-read error | Retain input until return; decode software bitmap; avoid alias double-recycling |
| Shutdown/rejected task exception | Shared dispatcher closed too soon; close wrappers first |
| Kotlin metadata incompatibility | Use compiler compatible with version recorded in IMPORT.md |
| Demo model download failure | Check access to pinned source URL; never bypass SHA-256 verification |

Catch failures at the host boundary and provide retry. Do not swallow cancellation in coroutine
UI code; rethrow `CancellationException`. Preserve the underlying cause when reporting
`TfliteInitializationException`. For a small model sanity check run the existing CPU device test;
then validate real images and target-device GPU behavior separately. See [demos](demos.md).
