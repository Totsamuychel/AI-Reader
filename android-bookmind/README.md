# BookMind — Android

Spoiler-safe, on-device book assistant. This is the Android (Kotlin + Jetpack
Compose) port of the iOS `ios-bookmind/` app, following the plan in `../android.md`.

## iOS → Android mapping

| iOS | Android |
| --- | --- |
| Swift + SwiftUI | Kotlin + Jetpack Compose |
| GRDB + SQLite FTS5 | Room + FTS4 (`@Fts4`) |
| MediaPipe Gemma (iOS) | `com.google.mediapipe:tasks-genai` |
| Swift Package Manager | Gradle (`build.gradle.kts`) |
| custom EPUB parser | `ZipInputStream` + Jsoup |
| `AppContainer` DI | Hilt |

## Module layout (`app/src/main/java/com/bookmind/`)

- `core/model` — domain models (= iOS `SharedModels`)
- `core/parser` — `EpubParser`, `TxtParser`, `HtmlStripper` (= `ReaderCore`)
- `core` — `BookImportService`, `ReaderSession` (= `ReaderCore`)
- `persistence` — Room `AppDatabase`, entities, DAOs, stores (= `Persistence`)
- `memory` — chunker, character detector, fact indexer, recap builder,
  `IngestionService` (= `BookMemory`)
- `retrieval` — `RetrievalEngine`, search services, `PromptContextAssembler` (= `Retrieval`)
- `safety` — `SpoilerBoundaryResolver`, `HeuristicSpoilerScanner` (= `Safety`)
- `llm` — `GemmaClient`, `MediaPipeBridge`, `AnswerService`, `SystemPrompts`,
  `ModelDownloadService` (= `LLM`)
- `ui` — Compose screens + ViewModels: `library`, `reader` (with the right-hand
  assistant side panel and saved quotes), `assistant`
- `di` — Hilt modules

## Phases implemented (per `android.md`)

1. Project, Gradle, Room + FTS — ✅
2. EPUB + TXT parsers — ✅
3. `IngestionService` + fact/character/recap heuristics — ✅
4. `GemmaClient` (MediaPipe bridge, with `EchoMediaPipeBridge` fallback) — ✅
5. `SpoilerBoundaryResolver` + `HeuristicSpoilerScanner` — ✅
6. Compose UI (Library, Reader, Assistant) — ✅
7. `AnswerService` orchestration (retrieval → prompt → generate → spoiler scan) — ✅
8. Unit tests (chunker, detector, spoiler boundary, FTS query) — ✅
9. Reader extras — ✅
   - **Quotes**: select a passage in the reader → “Save quote”; saved quotes live
     in a bottom sheet (per book, deletable, “Ask AI” about any quote).
   - **Assistant side panel**: the chat opens to the right of the text
     (split view on wide screens, slide-over on phones) and is aware of the
     current chapter, so answers stay spoiler-safe.
   - **Web tool**: an opt-in “Web” chip in the chat lets the LLM pull short,
     key-less background snippets (DuckDuckGo Instant Answer → Wikipedia
     fallback). The system prompt restricts web context to real-world background
     only — never plot beyond the reader's horizon — and the spoiler scanner
     still checks every answer.

## Building

Requires **JDK 17** and the **Android SDK** (compileSdk 35). The build tooling is
not available in this checkout's environment, so build from Android Studio or a
machine with the SDK installed:

```bash
# Generate the Gradle wrapper jar once (or just open the project in Android Studio):
gradle wrapper --gradle-version 8.7
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest   # pure-logic unit tests
```

Create `local.properties` with your SDK path, e.g. `sdk.dir=C:\\Users\\you\\AppData\\Local\\Android\\Sdk`.

## On-device model

The Gemma weights (~1.5 GB) are **not** bundled in the APK. `ModelDownloadService`
downloads `gemma-2b-it-cpu-int4.bin` into private storage on first run — set
`DEFAULT_MODEL_URL` to your CDN/Firebase URL. Until the model is present the app
runs against `EchoMediaPipeBridge` (deterministic stub replies), so the full
import → read → ask flow is exercisable without the model.

You can also push a model manually for testing:

```bash
adb push gemma-2b-it-cpu-int4.bin /data/local/tmp/   # then point ModelDownloadService.modelFile there
```

## Choosing & fine-tuning the on-device model

Recommended for phone-class hardware (best quality/latency per MB first):

| Model | Size (int4) | Why |
| --- | --- | --- |
| **Gemma 3 1B** (`gemma3-1b-it-int4.task`) | ~0.5 GB | Best fit: native MediaPipe `.task` support, 32k context, fast on mid-range phones (~2.5k tok/s prefill). Needs `tasks-genai` ≥ 0.10.22. |
| Gemma 2 2B / Gemma-2B (`-it-cpu-int4.bin`) | ~1.3–1.5 GB | What the current `tasks-genai:0.10.14` pipeline ships with; safe default if you don't bump the dependency. |
| Gemma 3n E2B | ~3 GB | Noticeably smarter, multimodal; only for flagship devices (6 GB+ RAM). |
| Qwen 2.5 1.5B / Llama 3.2 1B | ~1 GB | Good multilingual (RU) quality, but you must convert to LiteRT yourself via [AI Edge Torch](https://github.com/google-ai-edge/ai-edge-torch). |

**Verdict: Gemma 3 1B int4.** Strong Russian, smallest download, and LoRA
fine-tuning is supported end-to-end by Google's tooling.

Fine-tuning pipeline (QLoRA, fits a free Colab T4):

1. Build a dataset of (context block → spoiler-safe answer) pairs in the exact
   prompt format `GemmaClient.compose()` produces (system + `## Reading state` +
   `## Character cards` + … + question). ~1–5k examples is plenty for style/format.
2. Train LoRA adapters with [Keras/Gemma](https://ai.google.dev/gemma/docs/lora_tuning)
   or HuggingFace `peft` (r=8–16, lr 2e-4, 2–3 epochs).
3. Convert for on-device use with the
   [AI Edge converter](https://ai.google.dev/edge/mediapipe/solutions/genai/llm_inference#models)
   — MediaPipe LLM Inference can load the base `.task` plus your LoRA weights
   separately (`loraPath`), so the adapter is a few MB on top of the base model.
4. Point `ModelDownloadService.DEFAULT_MODEL_URL` at the converted file.

Note: moving to Gemma 3 1B requires bumping `com.google.mediapipe:tasks-genai`
(0.10.22+) and re-checking `MediaPipeGemmaBridge` against the newer
`LlmInference`/`LlmInferenceSession` API.
