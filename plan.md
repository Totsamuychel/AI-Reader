# План и задания для код‑агента по реализации iOS AI‑читалки

Этот файл — техническое ТЗ для код‑агента.  
Цель: поэтапно реализовать iOS‑приложение‑читалку с локальной памятью книги и on‑device LLM (Gemma 4 E2B), с фокусом на антиспойлерах.

---

## 0. Общая информация

- Платформа: iOS 17+
- Язык: Swift 5.10+, SwiftUI
- Архитектурный стиль: feature‑oriented, MVVM + сервисные слои, модульность
- Хранение данных: SQLite + GRDB, FTS5, векторы в SQLite (или BLOB + in‑memory cosine)
- LLM: Gemma 4 (E2B для MVP), локальный inference через MediaPipe LLM Inference (iOS SDK)
- IDE: код можно писать в VS Code / Cursor, сборка и симулятор — через Xcode

Ключевые принципы:
- Сначала **ReaderCore без AI**, затем память книги (BookMemory), затем Retrieval и только потом подключаем LLM.
- Память книги — это **структуры + события + recap + индексы**, а не один summary на всю книгу.
- Антиспойлеры реализуются на уровне схемы БД, retrieval и постобработки, а не только промптом.

---

## 1. Структура проекта

Создать репозиторий со структурой:

```text
ios-bookmind/
├── App/
│   ├── BookMindApp.swift
│   └── AppContainer.swift
├── Features/
│   ├── Library/
│   ├── Reader/
│   ├── Assistant/
│   └── Settings/
├── Core/
│   ├── ReaderCore/
│   ├── BookMemory/
│   ├── Retrieval/
│   ├── LLM/
│   ├── Safety/
│   └── Persistence/
├── Data/
│   ├── Migrations/
│   ├── Repositories/
│   └── Models/
├── Services/
│   ├── Import/
│   ├── Embeddings/
│   ├── Inference/
│   └── Analytics/
├── UI/
│   ├── Components/
│   ├── Theme/
│   └── Shared/
└── Tests/
    ├── Unit/
    ├── Integration/
    └── Snapshot/
```

Требования к структуре:

- `Core/*` можно оформить как Swift Packages (желательно минимум для `ReaderCore`, `BookMemory`, `Retrieval`, `LLM`, `Persistence`).
- `Features/*` — UI‑фичи, собирающие Core‑логику.
- `Data/*` — модели БД и репозитории над GRDB.
- `Services/*` — кросс‑фичевые сервисы (импорт, эмбеддинги, inference).
- `Tests/*` — обязательные автотесты.

---

## 2. Фаза 1 — Базовый ReaderCore (без AI)

**Цель:** получить стабильную офлайн‑читалку EPUB/TXT.

### Задачи

1. Создать iOS‑проект со SwiftUI (`BookMindApp`).
2. Создать модуль `Core/ReaderCore` (может быть Swift Package).

3. Определить базовые модели (пока без БД):

   ```swift
   struct BookID: Hashable, Codable {
       let rawValue: String
   }

   enum BookFormat: String, Codable {
       case epub
       case txt
   }

   struct Book: Identifiable, Codable {
       let id: BookID
       var title: String
       var author: String?
       var format: BookFormat
       var fileURL: URL
   }

   struct ChapterID: Hashable, Codable {
       let rawValue: String
   }

   struct Chapter: Identifiable, Codable {
       let id: ChapterID
       let bookID: BookID
       let index: Int
       var title: String?
       var contentRef: URL?   // или иной идентификатор ресурса
   }

   struct ReadingPosition: Codable {
       let bookID: BookID
       var chapterID: ChapterID?
       var progressFraction: Double
       var updatedAt: Date
   }

   enum ReaderTheme: String, Codable {
       case light
       case dark
       case sepia
   }

   struct ReaderSettings: Codable {
       var fontSize: Double
       var lineSpacing: Double
       var theme: ReaderTheme
   }
   ```

4. Определить протоколы:

   ```swift
   protocol BookImporting {
       func importBook(at url: URL) async throws -> Book
   }

   protocol BookParsing {
       func parseChapters(for book: Book) async throws -> [Chapter]
   }

   protocol ReaderSessionControlling {
       func open(book: Book) async
       func goToNextChapter() async
       func goToPreviousChapter() async
       func updateProgress(_ position: ReadingPosition) async
   }

   protocol ReadingProgressStoring {
       func loadPosition(for bookID: BookID) async throws -> ReadingPosition?
       func savePosition(_ position: ReadingPosition) async throws
   }
   ```

5. Реализовать сервисы (пока без реальной БД):

   - `BookImportService` — принимает URL (файл в sandbox), определяет формат (`epub`/`txt`), создает `Book`.
   - `EPUBParser` — минимум: разбирает manifest/spine и создает список `Chapter`.
   - `TXTParser` — делит plain‑текст по простому правилу (например, по маркерам глав).
   - `ReaderSessionController` — держит текущую книгу, главу и вызывает `ReadingProgressStoring`.
   - `InMemoryReadingProgressStore` — временная in‑memory реализация `ReadingProgressStoring`.

6. UI (SwiftUI):

   - `LibraryView` — список книг (`Book`), кнопка “добавить” (пока можно подставлять заглушки).
   - `ReaderView` — показывает текущую главу:
     - для EPUB — SwiftUI‑обертка над `WKWebView`;
     - для TXT — `ScrollView { Text(...) }`.
   - `ReaderToolbar` — навигация (следующая/предыдущая глава, настройки).
   - `ChapterListView` — модальный или боковой список глав.

7. Интегрировать:

   - при выборе книги — грузить главы через `BookParsing`;
   - восстанавливать позицию чтения через `ReadingProgressStoring`;
   - обновлять позицию при смене главы / закрытии reader‑экрана.

**Важно:** на этой фазе не использовать LLM, embeddings, SQLite. Все заглушки могут быть in‑memory.

---

## 3. Фаза 2 — Persistence и БД‑схема для книг/глав/прогресса

**Цель:** перенести хранение книг, глав и прогресса в SQLite + GRDB.

### Задачи

1. Создать модуль `Core/Persistence`, добавить зависимость GRDB.
2. Определить DAO/Repository слои:

   ```swift
   protocol BookRepository {
       func fetchAll() throws -> [Book]
       func save(_ book: Book) throws
   }

   protocol ChapterRepository {
       func fetchChapters(for bookID: BookID) throws -> [Chapter]
       func saveChapters(_ chapters: [Chapter]) throws
   }

   protocol ReadingProgressRepository: ReadingProgressStoring { }
   ```

3. Ввести минимальные таблицы:

   - `books`
   - `chapters`
   - `reading_progress`

   (схему можно брать из предыдущих сообщений или адаптировать под GRDB).

4. Реализовать миграции и инициализацию БД при старте приложения.
5. Перевести ReaderCore на использование репозиториев вместо in‑memory хранения.

---

## 4. Фаза 3 — BookMemory (структурированная память книги, без LLM)

**Цель:** добавить гибридную память книги: chunks, персонажи, события, связи, safe‑recap.

### Задачи

1. Создать модуль `Core/BookMemory`.

2. Определить модели памяти:

   ```swift
   struct ChunkID: Hashable, Codable { let rawValue: String }

   struct Chunk: Identifiable, Codable {
       let id: ChunkID
       let bookID: BookID
       let chapterID: ChapterID
       let index: Int
       let text: String
       let tokenCount: Int
       let spoilerLevel: Int
   }

   struct Character: Identifiable, Codable {
       let id: String
       let bookID: BookID
       let canonicalName: String
       let aliases: [String]
       let firstChapterIndex: Int
       let lastSafeChapterIndex: Int
       let safeSummary: String?
   }

   struct Event: Identifiable, Codable {
       let id: String
       let bookID: BookID
       let chapterID: ChapterID
       let eventType: String
       let shortDescription: String
       let longDescriptionSafe: String?
       let spoilerLevel: Int
   }

   struct Relation: Identifiable, Codable {
       let id: String
       let bookID: BookID
       let sourceCharacterID: String
       let targetCharacterID: String
       let relationType: String
       let confidence: Double
       let safeUntilChapterIndex: Int
   }

   struct Recap: Identifiable, Codable {
       let id: String
       let bookID: BookID
       let chapterID: ChapterID
       let recapText: String
       let style: String
   }

   struct Fact: Identifiable, Codable {
       let id: String
       let bookID: BookID
       let chapterID: ChapterID
       let factType: String
       let subjectID: String?
       let objectID: String?
       let text: String
       let spoilerLevel: Int
   }
   ```

3. Добавить таблицы БД для этих сущностей, включая FTS‑таблицу `chunks_fts` и желательно FTS по `facts`.

4. Реализовать сервисы:

   ```swift
   protocol ChapterChunking {
       func makeChunks(for chapter: Chapter, rawText: String) -> [Chunk]
   }

   protocol SafeRecapBuilding {
       func buildRecap(for chapter: Chapter, chunks: [Chunk]) -> Recap
   }

   protocol FactIndexing {
       func extractFacts(from chapter: Chapter, rawText: String) -> [Fact]
   }
   ```

   На этой фазе алгоритмы могут быть простыми (regex, эвристики, фиксированная длина chunk).

5. Добавить ingestion‑pipeline после импорта книги:

   - вызывать парсер -> главы;
   - для каждой главы:
     - получать сырой текст;
     - делать `Chunks`;
     - заполнять FTS;
     - строить `Recap`;
     - базовые `Fact` (напр. “персонаж встретился в этой главе”).

---

## 5. Фаза 4 — RetrievalEngine (поиск безопасного контекста)

**Цель:** по вопросу пользователя собирать релевантный, **спойлер‑безопасный** контекст из памяти книги.

### Задачи

1. Создать модуль `Core/Retrieval`.

2. Определить интерфейсы:

   ```swift
   struct RetrievalContext {
       var safeRecap: Recap?
       var characterCards: [Character]
       var recentEvents: [Event]
       var quotes: [Chunk]
       var facts: [Fact]
   }

   protocol ContextRetrieving {
       func context(
           for question: String,
           bookID: BookID,
           currentChapterIndex: Int
       ) async throws -> RetrievalContext
   }

   protocol CharacterLookupService {
       func findCharacters(
           matching query: String,
           bookID: BookID,
           currentChapterIndex: Int
       ) throws -> [Character]
   }

   protocol FactSearchService {
       func searchFacts(
           query: String,
           bookID: BookID,
           maxChapterIndex: Int
       ) throws -> [Fact]
   }

   protocol ChunkSearchService {
       func searchChunks(
           query: String,
           bookID: BookID,
           maxChapterIndex: Int
       ) throws -> [Chunk]
   }
   ```

3. Реализовать:

   - exact‑поиск по персонажам (имя / алиасы);
   - FTS‑поиск по `chunks` и `facts`;
   - фильтрацию по `spoilerLevel` и `chapterIndex` (ничего “из будущего”);
   - простой reranking: предпочитать сущности ближе к текущей главе и с меньшим spoiler‑уровнем.

4. Реализовать `PromptContextAssembler`:

   ```swift
   protocol PromptContextAssembling {
       func makePromptContext(
           from retrievalContext: RetrievalContext,
           question: String,
           currentChapterIndex: Int
       ) -> String  // или структура, если будет JSON‑prompt
   }
   ```

   На этой фазе можно просто возвращать `RetrievalContext` в debug‑UI без LLM.

---

## 6. Фаза 5 — LLMService (интеграция Gemma 4 E2B)

**Цель:** подключить локальную LLM как “формулировщик ответа” поверх RetrievalEngine.

### Задачи

1. Создать модуль `Core/LLM`.

2. Определить минимальные протоколы:

   ```swift
   protocol LLMClient {
       func generate(
           systemPrompt: String,
           userPrompt: String,
           maxTokens: Int
       ) async throws -> String
   }

   protocol ConversationSession {
       func send(
           userMessage: String,
           context: String
       ) async throws -> String
   }
   ```

3. Реализовать адаптер под Gemma 4 E2B через MediaPipe LLM Inference:

   - инициализация модели;
   - конфиг генерации;
   - вызов генерации текста;
   - обертка в `LLMClient`.

4. Определить system prompt для книжного ассистента:

   - роль: “ты ассистент по чтению книг”;
   - правило: “не раскрывай события после главы N; используй только предоставленный контекст”;
   - тон: кратко, без спойлеров, понятным языком.

5. Реализовать `AnswerService`:

   ```swift
   protocol AnswerProviding {
       func answer(
           question: String,
           bookID: BookID,
           currentChapterIndex: Int
       ) async throws -> String
   }
   ```

   Логика:
   - получить `RetrievalContext` из RetrievalEngine;
   - собрать `promptContext` через `PromptContextAssembler`;
   - сформировать user/system‑prompt;
   - вызвать `LLMClient`;
   - вернуть текст ответа.

---

## 7. Фаза 6 — SafetyLayer (anti‑spoiler усиление)

**Цель:** добавить отдельный слой безопасной логики поверх Retrieval и LLM‑ответа.

### Задачи

1. Создать модуль `Core/Safety`.

2. Определить:

   ```swift
   enum AnswerMode {
       case safe      // без спойлеров
       case hint      // мягкие намеки
       case full      // спойлеры разрешены
   }

   protocol SpoilerBoundaryResolving {
       func allowedChapterIndex(
           for mode: AnswerMode,
           currentChapterIndex: Int
       ) -> Int
   }

   protocol ResponseSpoilerScanning {
       func containsObviousSpoiler(
           _ answer: String,
           currentChapterIndex: Int
       ) -> Bool
   }
   ```

3. Встроить в pipeline:

   - При retrieval — фильтровать по `allowedChapterIndex`.
   - При генерации — в system prompt передавать актуальный `allowedChapterIndex`.
   - После ответа — запускать простую проверку (эвристики/регулярки/классификатор); при срабатывании — либо смягчать ответ, либо перегенерировать в `safe`‑режиме.

4. Добавить в настройки пользователю выбор режима (`safe` по умолчанию).

---

## 8. Фаза 7 — Assistant UI

**Цель:** сделать удобный UI‑слой для общения с ассистентом поверх ReaderView.

### Задачи

1. В `Features/Assistant` создать:

   - `AssistantViewModel` (использует `AnswerProviding`);
   - `AssistantView` — bottom sheet поверх `ReaderView`:
     - поле ввода вопроса;
     - список Q&A;
     - кнопки‑пресеты: 
       - “Что было в прошлой главе?”  
       - “Кто это?” (по выделенному имени)  
       - “Кратко напомни сюжет”.

2. Интегрировать:

   - из `ReaderView` открывать `AssistantView` свайпом/кнопкой;
   - при выделении текста (имени персонажа) предзаполнять вопрос “Кто такой <имя>?”.

---

## 9. Тестирование

Код‑агент должен добавлять тесты на каждом шаге:

- Unit‑тесты:
  - парсинг EPUB/TXT;
  - прогресс чтения (сохранение/загрузка);
  - chunking и recaps;
  - retrieval без LLM.
- Integration‑тесты:
  - импорт книги → память заполняется;
  - вопрос “кто это?” до/после определенной главы.
- UI/snapshot‑тесты:
  - ReaderView в разных темах и размерах шрифта;
  - AssistantView поверх ReaderView.

---

## 10. Правила для код‑агента

- Не добавлять облачные LLM по умолчанию.
- Не смешивать UI‑логику и persistence/LLM‑логику сверху вниз.
- Стараться делать каждый модуль (ReaderCore, BookMemory, Retrieval, LLM, Safety) переиспользуемым и тестируемым.
- При расширении ТЗ вносить изменения сначала в этот файл, затем в код.
