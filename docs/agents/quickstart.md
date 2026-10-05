# First detection
Read [installation](../../IMPORT.md). In your Android app use minSdk 28 and JVM 17;
use a Kotlin compiler compatible with the library metadata. The AAR does not include a model.

1. Put a compatible model in `app/src/main/assets/detector.tflite` (this name is an example).
2. Keep it uncompressed, because the loader uses `AssetManager.openFd` and memory mapping:

```kotlin
android {
    androidResources { noCompress += "tflite" }
}
```

3. Decode an upright, software-backed bitmap; avoid hardware-only bitmaps since preprocessing
reads pixels. The runnable photo demo uses `ImageDecoder.ALLOCATOR_SOFTWARE` and limits its
longest edge to 1600 pixels. See the [model contract](model-contract.md).
4. Use this complete helper from the demo module; it borrows your bitmap and returns source-pixel detections:

<!-- example: quickstart -->

```kotlin
package com.apexfission.android.yolo.demo

import android.content.Context
import android.graphics.Bitmap
import com.apexfission.android.yolo.engine.Detection
import com.apexfission.android.yolo.engine.buildYoloDetector

/** Blocking one-image example. The caller owns [bitmap] and must use a worker thread. */
fun detectImage(context: Context, bitmap: Bitmap, modelAssetPath: String): List<Detection> =
    buildYoloDetector(
        context = context.applicationContext,
        modelPath = modelAssetPath,
        scoreThreshold = 0.35f,
        iouThreshold = 0.45f,
        useGpu = false,
    ).use { detector -> detector.detect(bitmap) }
```

<!-- end-example: quickstart -->

The helper is compiled as part of `:app:assembleDebug`. Call it from a worker executor or
inside `withContext(Dispatchers.IO)` in a coroutine owned by your screen. Coroutine dependencies
and lifecycle ownership belong to the host. It constructs/closes a detector per image, which
is simple but adds startup cost; a camera pipeline should reuse one detector across frames.

A successful call returns zero or more `Detection` values. Labels are host-owned: map
`classId` to the exact training label order. A confidence value is a model score, not a
calibrated probability or proof of identity. Display boxes using the source-to-view mapping
in [concepts](concepts.md). Handle exceptions in your UI and allow another image or CPU retry.

To run the complete UI, follow [the demo catalog](demos.md).
