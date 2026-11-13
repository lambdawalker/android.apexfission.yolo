# On-device detection, with explicit contracts

Apexfission YOLO turns Android bitmaps into bounding boxes using a compatible LiteRT/TensorFlow
Lite model. It handles black-padded letterboxing, tensor conversion, inference, and class-aware
non-max suppression. Your app supplies the model, labels, UI, and lifecycle.

Start with [installation](../../IMPORT.md), then the
[complete first-detection helper](quickstart.md).

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
| Detect objects from a photo | [Photo demo](demos.md) |
| Check whether your model works | [Tensor contract](model-contract.md) |
| Integrate a camera analyzer | [Recipes](recipes.md) |
| Control tensors and post-processing | [Public API](api.md) |
| Give an AI agent the facts | [Raw Markdown](index.md) |

## Know the boundaries
This library does not ship a trained model, camera screen, permissions flow. Android API 28+ 
is required. It supports a specific YOLO tensor layout; an export named “YOLO” is not automatically
compatible. GPU use is a request. See [limitations](limitations.md) before adopting it.

## Documentation scope
This legacy overview describes **main development source** at the build commit displayed above.
For immutable release guides, choose a version in the [English catalog](/android.apexfission.yolo/en/)
or the [catálogo en español](/android.apexfission.yolo/es/).
[Installation](../../IMPORT.md) separately identifies the last confirmed
Maven release. Source and API extracts are synchronized at build time; raw agent guides are
available without JavaScript. [Maintenance](../maintenance.md) explains
how to keep all surfaces aligned.
