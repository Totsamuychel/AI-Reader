Маппинг iOS → Android
iOS (твой проект)	Android аналог	Технология
Swift + SwiftUI	Kotlin + Jetpack Compose	Android Studio
MediaPipe Gemma (on-device LLM)	MediaPipe Gemma (Android SDK)	com.google.mediapipe:tasks-genai
GRDB + SQLite FTS5	Room + FTS5	androidx.room
Swift Package Manager	Gradle + Maven	build.gradle.kts
EPUB parser (custom)	EpubLib	nl.siegmann.epublib
SwiftUI Views	Composables	Jetpack Compose
Структура Android проекта
text
android-bookmind/
├── app/
│   ├── src/main/
│   │   ├── java/com/bookmind/
│   │   │   ├── App.kt                      ← Application class
│   │   │   ├── di/                         ← Dependency Injection (Hilt)
│   │   │   │
│   │   │   ├── core/                       ← = ReaderCore (iOS)
│   │   │   │   ├── parser/
│   │   │   │   │   ├── EpubParser.kt
│   │   │   │   │   └── TxtParser.kt
│   │   │   │   └── model/
│   │   │   │       ├── Book.kt
│   │   │   │       ├── Chapter.kt
│   │   │   │       └── Chunk.kt
│   │   │   │
│   │   │   ├── memory/                     ← = BookMemory (iOS)
│   │   │   │   ├── IngestionService.kt
│   │   │   │   ├── ChunkIndexer.kt
│   │   │   │   ├── FactExtractor.kt
│   │   │   │   └── CharacterDetector.kt
│   │   │   │
│   │   │   ├── persistence/                ← = Persistence (iOS)
│   │   │   │   ├── AppDatabase.kt          ← Room DB
│   │   │   │   ├── dao/
│   │   │   │   │   ├── ChunkDao.kt         ← FTS5 queries
│   │   │   │   │   ├── FactDao.kt
│   │   │   │   │   └── BookDao.kt
│   │   │   │   └── entity/
│   │   │   │
│   │   │   ├── retrieval/                  ← = Retrieval (iOS)
│   │   │   │   └── RetrievalEngine.kt
│   │   │   │
│   │   │   ├── llm/                        ← = LLM (iOS)
│   │   │   │   ├── GemmaClient.kt          ← MediaPipe Android
│   │   │   │   └── PromptBuilder.kt
│   │   │   │
│   │   │   ├── safety/                     ← = Safety (iOS)
│   │   │   │   ├── SpoilerResolver.kt
│   │   │   │   └── SpoilerScanner.kt
│   │   │   │
│   │   │   └── ui/                         ← = Features (iOS)
│   │   │       ├── library/
│   │   │       │   └── LibraryScreen.kt    ← список книг
│   │   │       ├── reader/
│   │   │       │   └── ReaderScreen.kt     ← читалка
│   │   │       └── assistant/
│   │   │           └── AssistantScreen.kt  ← AI чат
│   │   │
│   │   └── assets/
│   │       └── gemma-2b-it-cpu-int4.bin    ← веса модели
│   │
│   └── build.gradle.kts
└── gradle/
Фазовый план разработки
Фаза 1 — Проект и база данных (1–2 дня)
Инициализация проекта:

bash
# Android Studio → New Project → Empty Activity (Kotlin + Compose)
# minSdk 26, targetSdk 35
build.gradle.kts — все зависимости:

kotlin
dependencies {
    // Jetpack Compose
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.0")
    
    // Room + FTS5 (= GRDB на iOS)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    
    // MediaPipe Gemma on-device LLM (= MediaPipe iOS)
    implementation("com.google.mediapipe:tasks-genai:0.10.14")
    
    // EPUB Parser
    implementation("nl.siegmann.epublib:epublib-core:3.1")
    
    // DI
    implementation("com.google.dagger:hilt-android:2.51")
    kapt("com.google.dagger:hilt-compiler:2.51")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    
    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
}
Room Database с FTS5 (аналог SQLite FTS5 + GRDB):

kotlin
// ChunkEntity.kt
@Entity(tableName = "chunks")
data class ChunkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val chapterIndex: Int,
    val text: String,
    val type: String  // "fact", "recap", "raw"
)

// FTS виртуальная таблица для быстрого поиска — аналог FTS5 в iOS
@Fts4(contentEntity = ChunkEntity::class)
@Entity(tableName = "chunks_fts")
data class ChunkFts(val text: String)

// ChunkDao.kt
@Dao
interface ChunkDao {
    @Insert
    suspend fun insertAll(chunks: List<ChunkEntity>)
    
    // FTS поиск только до текущей главы — ключевая spoiler-safe логика
    @Query("""
        SELECT c.* FROM chunks c
        JOIN chunks_fts fts ON c.rowid = fts.rowid
        WHERE chunks_fts MATCH :query
        AND c.bookId = :bookId
        AND c.chapterIndex <= :currentChapter
        ORDER BY rank
        LIMIT 10
    """)
    suspend fun searchSafe(query: String, bookId: Long, currentChapter: Int): List<ChunkEntity>
}
Фаза 2 — Парсеры книг (2–3 дня)
EPUB Parser (аналог твоего iOS EPUB парсера):

kotlin
class EpubParser @Inject constructor() {
    
    fun parse(uri: Uri, context: Context): ParsedBook {
        val inputStream = context.contentResolver.openInputStream(uri)
        val book = EpubReader().readEpub(inputStream)
        
        val chapters = book.spine.spineReferences.mapIndexed { index, ref ->
            val rawHtml = ref.resource.data.toString(Charsets.UTF_8)
            val text = Jsoup.parse(rawHtml).text()  // убираем HTML теги
            
            Chapter(
                index = index,
                title = ref.resource.title ?: "Chapter ${index + 1}",
                rawText = text
            )
        }
        
        return ParsedBook(
            title = book.title,
            author = book.metadata.authors.firstOrNull()?.toString() ?: "Unknown",
            chapters = chapters
        )
    }
}
Фаза 3 — Book Ingestion Service (3–4 дня)
Точный аналог твоего BookMemory модуля:

kotlin
class IngestionService @Inject constructor(
    private val chunkDao: ChunkDao,
    private val factExtractor: FactExtractor,
    private val characterDetector: CharacterDetector
) {
    // Flow для отображения прогресса в UI
    suspend fun ingestBook(book: ParsedBook, bookId: Long): Flow<IngestionProgress> = flow {
        
        book.chapters.forEachIndexed { index, chapter ->
            emit(IngestionProgress(index, book.chapters.size, chapter.title))
            
            // 1. Разбить главу на чанки по ~500 токенов
            val chunks = chunkText(chapter.rawText, chapterIndex = index, bookId = bookId)
            
            // 2. Извлечь факты и персонажей
            val facts = factExtractor.extract(chapter.rawText, index)
            val characters = characterDetector.detect(chapter.rawText)
            
            // 3. Создать safe recap главы (без спойлеров)
            val recap = buildSafeRecap(chapter, characters)
            
            // 4. Сохранить всё в Room DB
            chunkDao.insertAll(chunks + facts + listOf(recap))
        }
        
        emit(IngestionProgress.complete())
    }
    
    private fun chunkText(text: String, chapterIndex: Int, bookId: Long): List<ChunkEntity> {
        val words = text.split(" ")
        return words.chunked(400).map { wordChunk ->  // ~500 токенов
            ChunkEntity(
                bookId = bookId,
                chapterIndex = chapterIndex,
                text = wordChunk.joinToString(" "),
                type = "raw"
            )
        }
    }
}
Фаза 4 — MediaPipe Gemma Client (2–3 дня)
Прямой Android-эквивалент твоего LLMClient на iOS:

kotlin
class GemmaClient @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var inference: LlmInference? = null
    
    suspend fun initialize() = withContext(Dispatchers.IO) {
        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath("/data/local/tmp/gemma-2b-it-cpu-int4.bin")
            .setMaxTokens(1024)
            .setTopK(40)
            .setTemperature(0.8f)
            .setRandomSeed(101)
            .build()
        
        inference = LlmInference.createFromOptions(context, options)
    }
    
    // Стриминг ответа — токен за токеном (как в iOS версии)
    fun generateStream(prompt: String): Flow<String> = callbackFlow {
        inference?.generateResponseAsync(prompt) { partialResult, done ->
            trySend(partialResult)
            if (done) close()
        }
        awaitClose()
    }
}
Фаза 5 — Safety Layer (1–2 дня)
kotlin
class SpoilerResolver @Inject constructor() {
    
    fun buildSafeSystemPrompt(currentChapter: Int, totalChapters: Int): String = """
        You are a literary assistant. The user is currently reading chapter $currentChapter of $totalChapters.
        CRITICAL RULES:
        1. NEVER reveal events from chapters after chapter $currentChapter
        2. If context contains future events, ignore that information completely
        3. Only discuss what has happened UP TO and INCLUDING chapter $currentChapter
        4. If you cannot answer without spoilers, say "I cannot answer without spoiling future chapters"
    """.trimIndent()
}

class SpoilerScanner @Inject constructor() {
    
    // Сканируем ответ LLM на признаки будущих событий
    fun scan(response: String, safeChunks: List<ChunkEntity>): ScanResult {
        val safeTextPool = safeChunks.joinToString(" ") { it.text }.lowercase()
        
        // Ищем фразы, которые упоминают события не из safe контекста
        val sentences = response.split(".")
        val suspiciousSentences = sentences.filter { sentence ->
            val keyPhrases = extractKeyPhrases(sentence)
            keyPhrases.any { phrase -> !safeTextPool.contains(phrase.lowercase()) }
        }
        
        return if (suspiciousSentences.isEmpty()) {
            ScanResult.Safe(response)
        } else {
            ScanResult.PotentialSpoiler(response, suspiciousSentences)
        }
    }
}
Фаза 6 — UI в Jetpack Compose (4–5 дней)
AssistantScreen — главный экран чата:

kotlin
@Composable
fun AssistantScreen(
    viewModel: AssistantViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.bookTitle) },
                subtitle = { Text("Chapter ${uiState.currentChapter} • Spoiler-Safe") }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            
            // История сообщений
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f)
            ) {
                items(uiState.messages) { message ->
                    MessageBubble(message)
                }
                
                // Стриминг текущего ответа
                if (uiState.isGenerating) {
                    item {
                        StreamingBubble(uiState.streamingText)
                    }
                }
            }
            
            // Поле ввода
            MessageInput(
                value = uiState.inputText,
                onValueChange = viewModel::onInputChange,
                onSend = viewModel::sendMessage,
                enabled = !uiState.isGenerating
            )
        }
    }
}
Фаза 7 — AnswerService (2 дня)
Оркестратор всей логики — аналог AnswerService из iOS:

kotlin
class AnswerService @Inject constructor(
    private val retrievalEngine: RetrievalEngine,
    private val gemmaClient: GemmaClient,
    private val spoilerResolver: SpoilerResolver,
    private val spoilerScanner: SpoilerScanner
) {
    fun answerQuestion(
        question: String,
        bookId: Long,
        currentChapter: Int
    ): Flow<AnswerEvent> = flow {
        
        // 1. Получить safe контекст (только до текущей главы)
        val safeChunks = retrievalEngine.retrieve(question, bookId, currentChapter)
        emit(AnswerEvent.ContextRetrieved(safeChunks.size))
        
        // 2. Собрать промпт
        val systemPrompt = spoilerResolver.buildSafeSystemPrompt(currentChapter, bookId)
        val context = safeChunks.joinToString("\n---\n") { it.text }
        val fullPrompt = "$systemPrompt\n\nContext:\n$context\n\nQuestion: $question\nAnswer:"
        
        // 3. Стримить ответ
        var fullResponse = ""
        gemmaClient.generateStream(fullPrompt).collect { token ->
            fullResponse += token
            emit(AnswerEvent.Token(token))
        }
        
        // 4. Проверить на спойлеры
        val scanResult = spoilerScanner.scan(fullResponse, safeChunks)
        emit(AnswerEvent.Complete(scanResult))
    }
}
Timeline
Фаза	Задача	Дней
1	Проект, Gradle, Room + FTS5	2
2	EPUB + TXT парсеры	2
3	IngestionService + FactExtractor	3
4	GemmaClient (MediaPipe Android)	2
5	SpoilerResolver + SpoilerScanner	2
6	Весь UI (Compose: Library, Reader, Assistant)	5
7	AnswerService + интеграция всего	2
8	Тесты + полировка	2
Итого		~20 дней
Критическое отличие от iOS: загрузка модели
На Android модель (.bin файл ~1.5GB) нельзя бандлить в APK — нужна отдельная загрузка:

kotlin
// Показываем onboarding экран с прогрессом загрузки модели
class ModelDownloadService @Inject constructor() {
    fun downloadModel(): Flow<DownloadProgress> = flow {
        // Скачать из Firebase Storage или собственного CDN
        // Сохранить в context.filesDir (приватное хранилище)
        // Показать прогресс-бар пользователю
    }