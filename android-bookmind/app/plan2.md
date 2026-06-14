# BookMind Android — Фазовый план реализации

> **Стек:** Kotlin · Jetpack Compose · Hilt · Room · DataStore · Coil · MediaPipe Gemma  
> **Файлы проекта:** `android-bookmind/app/src/main/java/com/bookmind/`  
> **Формат:** каждая фаза — независимый блок задач для code-агента

---

## ФАЗА 1 — Критические фиксы читалки
> **Цель:** устранить баги, мешающие базовому использованию  
> **Файлы:** `ui/reader/ReaderScreen.kt`, `ui/reader/ReaderViewModel.kt`

### 1.1 Кнопка «Назад»
- [ ] Добавить `BackHandler { navController.popBackStack() }` в `ReaderScreen.kt`
- [ ] В `TopAppBar` добавить `navigationIcon`:
  ```kotlin
  navigationIcon = {
      IconButton(onClick = { navController.popBackStack() }) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
      }
  }
  ```
- [ ] Убрать первую лишнюю кнопку из toolbar (найти и удалить дублирующий `IconButton`)

### 1.2 Дублирование страниц
- [ ] В `ReaderViewModel.kt` найти логику разбивки текста на страницы
- [ ] Убедиться что `split()` вызывается только один раз в pipeline
- [ ] Добавить `distinctBy { it.content }` или дедупликацию по индексу
- [ ] Покрыть юнит-тестом: `assert(pages.size == pages.distinct().size)`

### 1.3 Улучшение форматирования текста книги
- [ ] В `ReaderViewModel.kt` заменить разбивку на:
  ```kotlin
  val paragraphs = rawText
      .split(Regex("\\n\\s*\\n"))
      .filter { it.isNotBlank() }
      .map { it.trim() }
  ```
- [ ] В `ReaderScreen.kt` рендерить каждый абзац отдельно с `Spacer(Modifier.height(12.dp))` между ними
- [ ] Добавить `AnnotatedString` для цитат (`>` prefix → курсив + цвет) и кода (моноширинный шрифт)
- [ ] Проверить на EPUB, PDF, FB2 форматах

---

## ФАЗА 2 — UX читалки
> **Цель:** полноценный читательский опыт  
> **Файлы:** `ui/reader/ReaderScreen.kt`, `ui/reader/ReaderViewModel.kt`

### 2.1 Скрытие верхней панели
- [ ] Добавить `val scrollState = rememberScrollState()` (или использовать `LazyListState`)
- [ ] Добавить логику:
  ```kotlin
  val topBarVisible by remember {
      derivedStateOf { scrollState.firstVisibleItemIndex == 0 }
  }
  AnimatedVisibility(
      visible = topBarVisible,
      enter = slideInVertically(),
      exit = slideOutVertically()
  ) { TopAppBar(...) }
  ```
- [ ] Убедиться что статусбар тоже скрывается через `WindowInsetsController`

### 2.2 Свайп для перелистывания страниц
- [ ] Добавить зависимость в `build.gradle.kts`:
  ```kotlin
  implementation("androidx.compose.foundation:foundation:1.6.8")
  ```
- [ ] Заменить текущий скролл на `HorizontalPager`:
  ```kotlin
  val pagerState = rememberPagerState(pageCount = { pages.size })
  HorizontalPager(
      state = pagerState,
      flingBehavior = PagerDefaults.flingBehavior(pagerState)
  ) { page -> PageContent(pages[page]) }
  ```
- [ ] Синхронизировать `pagerState.currentPage` с `currentPage` в ViewModel
- [ ] Сохранять позицию в `DataStore` при смене страницы

### 2.3 Анимации перелистывания
- [ ] Создать enum:
  ```kotlin
  enum class FlipAnimation { SLIDE, FADE, SCALE }
  ```
- [ ] Реализовать кастомный `PagerSnapDistance` для SLIDE
- [ ] `FADE` — через `crossfade` modifier на контенте страницы
- [ ] `SCALE` — через `graphicsLayer { scaleX = lerp(0.85f, 1f, pageOffset) }`
- [ ] Сохранять выбор в `DataStore` с ключом `"flip_animation"`

### 2.4 Меню настроек (троеточие)
- [ ] Добавить `var showMenu by remember { mutableStateOf(false) }` в `ReaderScreen`
- [ ] В `TopAppBar` добавить:
  ```kotlin
  actions = {
      IconButton(onClick = { showMenu = true }) {
          Icon(Icons.Default.MoreVert, contentDescription = "Меню")
      }
      DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
          DropdownMenuItem(text = { Text("Шрифт") }, onClick = { /* открыть настройки */ })
          DropdownMenuItem(text = { Text("Фон") }, onClick = { })
          DropdownMenuItem(text = { Text("Анимация") }, onClick = { })
          DropdownMenuItem(text = { Text("Статистика") }, onClick = { navController.navigate("stats/$bookId") })
      }
  }
  ```

---

## ФАЗА 3 — Ночной режим и кастомизация
> **Цель:** персонализация читалки  
> **Файлы:** `ui/reader/ReaderScreen.kt`, `ui/theme/`, новый `ui/settings/ReaderSettingsScreen.kt`

### 3.1 Ночной режим с желтизной
- [ ] Добавить в `DataStore` ключи: `"warmth_level"` (Float, 0..1), `"night_auto_start"` (String HH:mm), `"night_auto_end"` (String HH:mm)
- [ ] В `ReaderScreen` обернуть контент:
  ```kotlin
  Box(modifier = Modifier
      .fillMaxSize()
      .drawWithContent {
          drawContent()
          if (warmth > 0f)
              drawRect(Color(1f, 0.85f, 0.5f, warmth * 0.4f))
      }
  )
  ```
- [ ] Создать `NightModeScheduler` — `BroadcastReceiver` + `AlarmManager` для авто-включения
- [ ] На главном экране добавить виджет выбора времени через `TimePickerDialog`

### 3.2 Экран настроек читалки
- [ ] Создать `ui/settings/ReaderSettingsScreen.kt`
- [ ] Секция **Шрифт**: `RadioGroup` с вариантами `Serif`, `SansSerif`, `Monospace` + слайдер размера
- [ ] Секция **Фон страницы**: цветовые чипы (Белый, Бежевый, Тёмно-серый, Чёрный)
- [ ] Секция **Анимация**: `RadioGroup` из `FlipAnimation` enum
- [ ] Секция **Ночной режим**: слайдер желтизны + TimePicker для расписания
- [ ] Все настройки сохранять в `DataStore`, читать через `SettingsRepository`

---

## ФАЗА 4 — Библиотека (полки)
> **Цель:** красивый 3D вид библиотеки с полками  
> **Файлы:** `ui/library/LibraryScreen.kt`, `ui/library/LibraryViewModel.kt`

### 4.1 3D полки с фоном комнаты
- [ ] Добавить фоновое изображение `room_bg.webp` в `res/drawable/`
- [ ] В `LibraryScreen.kt` обернуть в `Box`:
  ```kotlin
  Box(modifier = Modifier.fillMaxSize()) {
      Image(
          painter = painterResource(R.drawable.room_bg),
          contentDescription = null,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize()
      )
      LazyColumn { /* полки */ }
  }
  ```
- [ ] Создать composable `ShelfRow(books: List<Book>)` с:
  ```kotlin
  Box(modifier = Modifier
      .fillMaxWidth()
      .graphicsLayer {
          rotationX = -3f
          shadowElevation = 12f
          shape = RoundedCornerShape(4.dp)
      }
      .background(Color(0xFF8B5E3C)) // цвет деревянной полки
  ) { /* книги */ }
  ```
- [ ] Разбивать список книг по `chunked(5)` → каждый chunk = одна `ShelfRow`

### 4.2 Обложки книг от пользователя
- [ ] Добавить поле в `BookEntity`:
  ```kotlin
  @ColumnInfo(name = "cover_uri") val coverUri: String? = null
  ```
- [ ] Создать миграцию Room `MIGRATION_X_Y`
- [ ] В карточке книги добавить кнопку «Изменить обложку» (иконка карандаша)
- [ ] Использовать `rememberLauncherForActivityResult(ActivityResultContracts.GetContent())`:
  ```kotlin
  val launcher = rememberLauncherForActivityResult(
      contract = ActivityResultContracts.GetContent()
  ) { uri -> viewModel.updateCover(bookId, uri.toString()) }
  ```
- [ ] Отображать через `AsyncImage(model = coverUri ?: extractedCover, ...)`

---

## ФАЗА 5 — Статистика
> **Цель:** трекинг чтения по книге  
> **Файлы:** новый `ui/stats/BookStatsScreen.kt`, `persistence/`

### 5.1 Модель данных статистики
- [ ] Создать `ReadingSessionEntity(bookId, startTime, endTime, pagesRead)` в Room
- [ ] Создать `ReadingStatsDao` с запросами:
    - `getTotalPagesRead(bookId)`
    - `getAvgTimePerPage(bookId)`
    - `getSessionsByBook(bookId): Flow<List<ReadingSessionEntity>>`
- [ ] В `ReaderViewModel` записывать сессию при открытии/закрытии книги

### 5.2 Экран статистики
- [ ] Создать `ui/stats/BookStatsScreen.kt`
- [ ] Показывать: прочитано страниц / всего, % завершения, среднее время на страницу, дата последнего чтения
- [ ] График прогресса — Canvas Compose `DrawScope`:
  ```kotlin
  drawArc(
      color = MaterialTheme.colorScheme.primary,
      startAngle = -90f,
      sweepAngle = 360f * progress,
      useCenter = false,
      style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
  )
  ```
- [ ] Добавить навигацию `"stats/{bookId}"` в `BookMindNavHost.kt`

---

## ФАЗА 6 — Авторизация и облако
> **Цель:** аккаунт, синхронизация, подписка  
> **Новые зависимости:** Firebase Auth, Firestore, RevenueCat

### 6.1 Добавить зависимости
```kotlin
// Firebase
implementation(platform("com.google.firebase:firebase-bom:33.0.0"))
implementation("com.google.firebase:firebase-auth-ktx")
implementation("com.google.firebase:firebase-firestore-ktx")

// RevenueCat
implementation("com.revenuecat.purchases:purchases:7.10.0")
implementation("com.revenuecat.purchases:purchases-ui:7.10.0")
```

### 6.2 Авторизация
- [ ] Создать `ui/auth/AuthScreen.kt` — email/пароль + Google Sign-In
- [ ] Создать `AuthRepository` с методами `signIn()`, `signUp()`, `signOut()`, `currentUser`
- [ ] Добавить `rememberMe` флаг в `DataStore` — если `true`, пропускать `AuthScreen`
- [ ] В `BookMindNavHost.kt` добавить стартовую проверку: если не залогинен → `AuthScreen`

### 6.3 Синхронизация с Firestore
- [ ] Создать `CloudSyncRepository`:
    - `uploadBook(book: BookEntity)` → `Firestore /users/{uid}/books/{bookId}`
    - `uploadProgress(bookId, page)` → `Firestore /users/{uid}/progress/{bookId}`
    - `downloadUserLibrary(uid): Flow<List<BookEntity>>`
- [ ] Автосинхронизация при изменении книги через `WorkManager` (фоновая задача)

### 6.4 Подписка RevenueCat
- [ ] Инициализировать в `App.kt`: `Purchases.configure(this, "YOUR_API_KEY")`
- [ ] Создать `SubscriptionRepository` с методами `isPremium(): Flow<Boolean>`, `purchase()`
- [ ] Paywall-экран через `RevenueCatUI.presentPaywallIfNeeded()`
- [ ] Premium-фичи: синхронизация облака, расширенная статистика, все анимации

---

## ФАЗА 7 — AI-ассистент: сохранение чатов
> **Цель:** история чатов привязана к книге  
> **Файлы:** `memory/`, `ui/assistant/`

### 7.1 Модель данных чата
- [ ] Создать `ChatMessageEntity(id, bookId, role: String, content: String, timestamp: Long)` в Room
- [ ] Создать `ChatHistoryDao`:
    - `getHistory(bookId): Flow<List<ChatMessageEntity>>`
    - `insertMessage(msg: ChatMessageEntity)`
    - `clearHistory(bookId)`
    - `getAllBooks(): List<String>` (для списка книг с чатами)

### 7.2 Интеграция в AssistantScreen
- [ ] В `AssistantViewModel` при старте загружать историю: `chatDao.getHistory(bookId).collect { ... }`
- [ ] При каждом сообщении сохранять в Room сразу (user message → сохранить → отправить в LLM → сохранить response)
- [ ] Добавить в меню (троеточие) пункт «Очистить историю» → `chatDao.clearHistory(bookId)`
- [ ] Показывать список книг у которых есть сохранённые чаты на главном экране

---

## ФАЗА 8 — Локальная LLM модель
> **Цель:** завершить интеграцию MediaPipe Gemma  
> **Файлы:** `llm/`

### 8.1 Доделать модель
- [ ] Проверить `llm/` модуль — найти незавершённые TODO/FIXME
- [ ] Убедиться что модель загружается асинхронно с показом прогресс-бара
- [ ] Добавить fallback на API (Gemini/OpenAI) если локальная модель не загружена
- [ ] Кэшировать модель между сессиями — не перезагружать при каждом открытии

### 8.2 Контекст книги в LLM
- [ ] Передавать текущий абзац/главу как context в промпт
- [ ] Добавить системный промпт: `"Ты AI-ассистент читателя. Книга: {title}. Текущая глава: {chapter}."`
- [ ] Ограничивать контекст до 2048 токенов (обрезать историю по `maxTokens`)

---

## Технические зависимости между фазами

```
Фаза 1 (фиксы)
    ↓
Фаза 2 (UX читалки) → Фаза 3 (кастомизация)
    ↓
Фаза 4 (библиотека)
    ↓
Фаза 5 (статистика)
    ↓
Фаза 6 (авторизация) ← требует Room schema стабильной из Фаз 1-5
    ↓
Фаза 7 (чаты) ← параллельно с Фазой 6
    ↓
Фаза 8 (LLM) ← параллельно в любой момент
```

---

## Ключевые файлы для агента

| Фаза | Файлы для изменения | Новые файлы |
|------|---------------------|-------------|
| 1 | `ReaderScreen.kt`, `ReaderViewModel.kt` | — |
| 2 | `ReaderScreen.kt`, `ReaderViewModel.kt`, `build.gradle.kts` | — |
| 3 | `ReaderScreen.kt` | `ui/settings/ReaderSettingsScreen.kt`, `NightModeScheduler.kt` |
| 4 | `LibraryScreen.kt`, `LibraryViewModel.kt` | `ShelfRow.kt`, `room_bg.webp` |
| 5 | `BookMindNavHost.kt`, `persistence/` | `ui/stats/BookStatsScreen.kt`, `ReadingSessionEntity.kt` |
| 6 | `App.kt`, `BookMindNavHost.kt`, `build.gradle.kts` | `ui/auth/AuthScreen.kt`, `CloudSyncRepository.kt` |
| 7 | `ui/assistant/`, `memory/` | `ChatMessageEntity.kt`, `ChatHistoryDao.kt` |
| 8 | `llm/` | — |

---

*Создано автоматически на основе анализа репозитория [AI-Reader](https://github.com/Totsamuychel/AI-Reader)*