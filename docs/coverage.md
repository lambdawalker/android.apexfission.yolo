# Documentation coverage
All guides describe main; the install page separately identifies the confirmed release.
Site pages are generated from these canonical guides, so human and agent contracts share facts.

| Public feature | Human guide | Agent guide | Runnable evidence | Screenshot |
| --- | --- | --- | --- | --- |
| Bitmap detection, factories, GPU request | First detection / model contract | [Quickstart](agents/quickstart.md), [detector API](agents/api/detector.md) | `DocumentationQuickstart.kt`, photo demo, CPU smoke test | Device capture unavailable |
| CameraX proxy overload / upright conversion | Recipes / image API | [Recipes](agents/recipes.md), [images](agents/api/image.md) | No camera demo; host snippet and conversion source | Not claimed |
| Thread-confined engine and shared resource | Threads & ownership / inference API | [Concepts](agents/concepts.md), [inference](agents/api/inference.md) | ThreadConfinedResourceTest, SharedEngineDispatcherTest | Not useful |
| CPU thread choices / result data | Detector API | [Detector](agents/api/detector.md) | NumThreadsTest, photo demo | Not useful |
| Normalized/pixel decoding, NMS, IoU | Recipes / post-processing API | [Post-processing](agents/api/postprocess.md) | DemoExamples, NMS/IoU/coordinate-safety tests | Device capture unavailable |
| Seven crop modes, crop offsets, rotation | Image API | [Images](agents/api/image.md) | ImageOperationsTest, helper source | Not captured |
| Tensor validation, buffer conversion | Model contract / buffers API | [Model contract](agents/model-contract.md), [buffers](agents/api/buffers-validation.md) | TensorContractValidatorTest, buffer/converter tests | Not useful |
| Cleanup, pause, failures | Concepts / troubleshooting | [Concepts](agents/concepts.md), [troubleshooting](agents/troubleshooting.md) | Photo demo use block, resource tests | Not useful |
| Packaging, model provenance, releases | Installation / demos / release runbook | [IMPORT](../IMPORT.md), [demos](agents/demos.md) | Existing release verification workflow | Not useful |

These entries identify existing evidence, not claims that every test ran in the authoring
environment. The photo and synthetic demos cover distinct paths; no redundant screen was added.
Camera integration, individual crop modes, and shared workers have source/test coverage rather
than a dedicated interactive demo. See maintenance commands and the PR validation report.
