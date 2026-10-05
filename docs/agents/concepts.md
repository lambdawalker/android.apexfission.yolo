# Data flow, threads, and ownership
## Detection pipeline
`buildYoloDetector` creates a thread-confined wrapper around a detector. A bitmap is resized
into a cached square black-padded letterbox, converted to RGB tensors, inferred, decoded,
unletterboxed, clamped to input bounds, then filtered with class-aware NMS.
Coordinates are integer source-image pixels in `ImageBox` (`x`, `y`, `x2`, `y2`), supplied
by the transitive Apexfission Coordinates dependency. Confidence and class ID accompany each box.
`Feature` is a separate public data holder with the same fields; the detector returns `Detection`.

## Ownership table
| Resource | Owner and release rule |
| --- | --- |
| Input `Bitmap` | Host; never mutate/recycle during a synchronous call; detector does not recycle it |
| `ImageProxy` passed to detect | Host; close in `finally`; detector only disposes its temporary upright bitmap |
| Detector / inference engine | Host; close on completion; prefer factories and `use` for one-shot work |
| Shared `EngineThreadDispatcher` | Host; close all wrappers first, dispatcher last |
| Detector letterbox cache | Detector; reused and recycled on close; not public |
| `FloatArray` from inference | Independent copy; caller may retain it |
| Crop/rotate output | Host; may alias input for no-op transformations; check identity before recycling |

## Synchronous confinement
The factories handle physical-thread affinity for native/GPU construction, calls, and teardown.
They do **not** make the caller asynchronous. Construction, inference, and close can block.
Use worker execution even though the wrapper has a worker internally. There are no callbacks,
Flow streams, lifecycle observers, or suspending library methods.

Interruption waits for native work to finish and restores the interrupted flag. Coroutine
cancellation does not preempt native inference. Keep buffers alive and finish cleanup before
abandoning an image. Wrappers return empty detection/output arrays after close; detector
`enabled` reads false after its wrapper is closed. Do not use these empty results as a model-success signal.

For continuous detection, serialize host work and use camera backpressure. Factory wrappers
serialize admitted work. Avoid directly constructing `YoloDetector` for shared-dispatcher
pipelines: its monitor/dispatch interaction is less safe than the wrapper entry point.
`ThreadConfinedResource.value` is exposed but bypassing `call` defeats confinement.

## Mapping to a preview
For an upright source of width W and height H, a fit-centered preview has scale
`min(viewWidth/W, viewHeight/H)`. Add `(viewWidth-W*scale)/2` and `(viewHeight-H*scale)/2`
to the scaled coordinates. For a center-crop preview use `max` instead. Account for front-camera
mirroring and any CameraX viewport transforms separately; this library does not bind a preview.

If you crop before detecting, boxes refer to the crop. Add `CroppedResult.xOffset/yOffset`
before mapping them into the original image. The `ImageProxy` overload rotates by
`imageInfo.rotationDegrees`; its coordinates belong to that upright bitmap, not the raw sensor.

## Tuning
`scoreThreshold` controls admission, `iouThreshold` controls same-class suppression only when
IoU is strictly greater, and `maxNmsCandidates` caps **returned detections**, not the input
candidate scan. Use finite thresholds in [0,1] and a positive cap; constructors do not fully
validate these values. Default cap is 150 in detector factories. Defaults for CPU threading are
`NumThreads.Default`: min(3, available processors). Percentages floor and clamp to at least one;
custom counts clamp to one but are not capped to available cores. Avoid extreme custom values.
