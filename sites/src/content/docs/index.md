---
title: On-device detection, with explicit contracts
description: Integrate YOLO inference, image preprocessing, and NMS in Android.
---
Apexfission YOLO turns Android bitmaps into bounding boxes using a compatible LiteRT/TensorFlow
Lite model. It handles black-padded letterboxing, tensor conversion, inference, and class-aware
non-max suppression. Your app supplies the model, labels, UI, and lifecycle.

Start with [installation](/android.apexfission.yolo/installation/), then the
[complete first-detection helper](/android.apexfission.yolo/getting-started/).

```kotlin
// Inside worker code; context, bitmap and your model asset path are host inputs.
val found = buildYoloDetector(context, "detector.tflite", 0.35f, 0.45f, false)
    .use { detector -> detector.detect(bitmap) }
```

This is a partial illustration; the quickstart includes imports, model packaging, ownership,
and the compiled helper. Calls block even though native work is thread-confined.

## Choose your integration
| Need | Start here |
| --- | --- |
| Detect objects from a photo | [Photo demo](/android.apexfission.yolo/demos/) |
| Check whether your model works | [Tensor contract](/android.apexfission.yolo/model-contract/) |
| Integrate a camera analyzer | [Recipes](/android.apexfission.yolo/task-recipes/) |
| Control tensors and post-processing | [Public API](/android.apexfission.yolo/reference/) |
| Give an AI agent the facts | [Raw Markdown](/android.apexfission.yolo/agents/index.md) |

## Know the boundaries
This library does not ship a trained model, camera screen, permissions flow. Android API 28+ 
is required. It supports a specific YOLO tensor layout; an export named “YOLO” is not automatically
compatible. GPU use is a request. See [limitations](/android.apexfission.yolo/limitations/) before adopting it.

## Documentation scope
This legacy overview describes **main development source** at the build commit displayed above.
For immutable release guides, choose a version in the [English catalog](/android.apexfission.yolo/en/)
or the [catálogo en español](/android.apexfission.yolo/es/).
[Installation](/android.apexfission.yolo/installation/) separately identifies the last confirmed
Maven release. Source and API extracts are synchronized at build time; raw agent guides are
available without JavaScript. [Maintenance](/android.apexfission.yolo/development/) explains
how to keep all surfaces aligned.
