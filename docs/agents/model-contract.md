# Supported model and tensor contract
A YOLO name or file extension alone does not establish compatibility. Initialization validates
one input and one output; model weights, label order, and output semantics remain your responsibility.

| Property | Required behavior |
| --- | --- |
| Input shape | `[1, S, S, 3]`, positive square S, RGB channels last |
| Output shape | `[1, attributes, boxes]` or `[1, boxes, attributes]` |
| Output axis inference | Smaller dimension is attributes; dimensions must differ; attributes >= 5 |
| Attributes | Center X, center Y, width, height, then class scores (no separate objectness channel) |
| Types | FLOAT32 or signed INT8 input/output; independently supported |
| Quantization | INT8 finite positive scale and zero point in [-128,127] |
| Input values | RGB / 255; INT8 additionally divides by scale, adds zero point, truncates, clamps |
| High-level coordinates | Normalized center/size values; scaled by model input width |
| Low-level pixel coordinates | Select `YoloPostProcessor.OutputScalingMode.NONE` explicitly |

Dynamic shapes, non-square inputs, UINT8, multiple outputs, segmentation masks, pose keypoints,
separate objectness, and embedded-NMS output formats are unsupported. A semantically wrong model
may still pass shape checks. Output axis inference requires more boxes than attributes.

The validator rejects invalid dimensions, unsupported types, invalid quantization, ambiguous
axes, and element/byte counts exceeding Int capacity with `IllegalArgumentException`.
The inference factory wraps tensor/interpreter setup errors in `TfliteInitializationException`;
asset loading errors can propagate directly because loading precedes that wrapper.

`useGpu` is a request, not a guarantee. INT8 inputs skip GPU. Unsupported hardware or delegate
creation failure may proceed on CPU. Failure during interpreter construction with a delegate
is not guaranteed to retry on CPU; close failed work and explicitly reconstruct with `useGpu=false`.
The library exposes no active-backend query. Check accuracy and performance on target devices.

For direct `InferenceEngine.runInference`, supply an already-prepared square software bitmap
matching `inputImageWidth`. The engine does not letterbox or apply NMS. Oversized images are not
a substitute for explicit preprocessing; buffer filling reads the top-left input-size region.
Use `buildYoloDetector` when you want the complete bitmap-to-box pipeline.
