# Integration recipes
## CameraX analysis (partial host snippet)
Create one detector on a worker before binding analysis; keep it alive across frames. Within
your analyzer, use the following pattern (the host supplies `detector`, `image`, and UI delivery):

```kotlin
try {
    val detections = detector.detect(image)
    // Marshal results to the UI using your application's lifecycle scope.
} finally {
    image.close()
}
```

Use `STRATEGY_KEEP_ONLY_LATEST`, a worker executor, and host-owned camera permissions/binding.
Stop analysis and drain active calls before closing the detector. This is an integration pattern,
not a bundled camera demo. The [photo demo](demos.md) is the complete runnable UI integration.

## Share a native worker (partial configuration)
Import `com.apexfission.android.yolo.tflite.engine.EngineThreadDispatcher` and pass the same
instance as `sharedDispatcher` to the detector/engine factories. Initialize and use those
wrappers from worker code. In `finally`, close wrappers before the shared dispatcher.
Do not substitute an arbitrary executor/coroutine dispatcher: physical-thread affinity matters.

## Decode a tensor without running a model
The complete synthetic example compiled in the app is:

<!-- example: postprocess -->

```kotlin
package com.apexfission.android.yolo.demo

import android.graphics.Bitmap
import com.apexfission.android.yolo.engine.Detection
import com.apexfission.android.yolo.engine.LetterboxResult
import com.apexfission.android.yolo.postprocess.YoloPostProcessor
import com.apexfission.android.yolo.tflite.engine.InferenceEngine

/** Real public post-processing API with deterministic tensor data, not model inference. */
object DemoExamples {
    fun postProcess(bitmap: Bitmap): List<Detection> = YoloPostProcessor(
        outLayout = InferenceEngine.OutputLayout.BOXES_X_ATTRS,
        outBoxes = 3,
        outAttrs = 5,
        numClasses = 1,
        inputImageWidth = 640,
        scoreThreshold = 0.35f,
        iouThreshold = 0.45f,
        maxNmsCandidates = 150,
    ).process(
        floatArrayOf(
            0.30f, 0.30f, 0.40f, 0.25f, 0.90f,
            0.31f, 0.31f, 0.40f, 0.25f, 0.80f,
            0.80f, 0.80f, 0.20f, 0.20f, 0.70f,
        ),
        LetterboxResult(bitmap, 1f, 0f, 0f, 640, 640),
    )

    val classLabels = listOf(
        "Horizontal card", "Vertical card", "Card back", "Photo", "Slim barcode",
        "PDF417", "MRZ text", "Barcode", "QR code",
    )
}
```

<!-- end-example: postprocess -->

Three candidates become two detections: the overlapping lower-score candidate is suppressed.
This checks post-processing, not accuracy. For raw pixel-coordinate tensors select
`outputScalingMode = YoloPostProcessor.OutputScalingMode.NONE`. Pass exact tensor dimensions,
class count, and matching `LetterboxResult`; the post-processor does not validate array length
or metadata consistency and can throw indexing exceptions for malformed input.

## Crop before detection
Use `cropWithOffset(PreProcessingImageTransformation.CenterSquareCrop, bitmap)` and detect
its bitmap. Add the returned offsets to map detections to the original image. Recycle a
new crop only after inference and display consumers finish; avoid recycling an alias of the source.
See [image API](api/image.md) for all seven modes and crop overloads.

## Pause detection
Set `detector.enabled = false` to return empty detections without inference. This is not a
lifecycle binding and does not release native resources. Set true to resume; close when finished.
