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
- `ui` — Compose screens + ViewModels: `library`, `reader`, `assistant`
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
