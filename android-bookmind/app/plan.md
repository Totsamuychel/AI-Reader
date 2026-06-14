# 🎨 UI/UX Improvement Prompt — BookMind Android (AI Reader)

> **Для code-агента**: Полный план качественного улучшения интерфейса Android-приложения читалки BookMind.  
> Проект находится в папке `android-bookmind/`. Стек: Kotlin + Jetpack Compose + Material 3.

---

## 🎯 Общая цель

Переработать UI/UX приложения BookMind до уровня премиум-читалок (Moon+ Reader Pro, Readera, KOReader), добавив:
- Иммерсивную 3D-полку книг на главном экране
- Умную панель AI-ассистента, не перекрывающую контент
- Полную систему тем и персонализации
- Все стандартные функции качественной читалки на Android

---

## 📁 Затрагиваемые файлы (android-bookmind/app/src/)
main/java/.../
├── ui/
│ ├── screens/
│ │ ├── LibraryScreen.kt ← полностью переработать (3D-полка)
│ │ ├── ReaderScreen.kt ← добавить split-panel для AI
│ │ ├── HomeScreen.kt ← новый дизайн
│ │ └── SettingsScreen.kt ← расширить (темы, шрифты, жесты)
│ ├── components/
│ │ ├── BookShelf3D.kt ← НОВЫЙ компонент
│ │ ├── AiAssistantPanel.kt ← НОВЫЙ компонент (боковая панель)
│ │ ├── ResizableSplitPane.kt ← НОВЫЙ компонент
│ │ ├── BookCoverCard.kt ← переработать
│ │ └── ThemeSelector.kt ← НОВЫЙ компонент
│ └── theme/
│ ├── Theme.kt ← расширить (10+ тем)
│ ├── Color.kt ← полная палитра
│ └── Typography.kt ← расширить шрифты

text

---

## 🏛️ ЗАДАЧА 1 — 3D Библиотека с книжными полками

### Файл: `ui/components/BookShelf3D.kt`

Реализовать визуальную 3D-полку с помощью **Jetpack Compose + Canvas API + перспективных трансформаций**.

```kotlin
// Использовать androidx.compose.ui.graphics.drawscope и Matrix для 3D-эффекта
// НЕ использовать OpenGL/SceneView — только Compose Canvas для простоты
```

**Требования к реализации:**

1. **Перспектива и глубина**
    - Книги на полке рендерятся с наклоном ~15° (perspective transform via `graphicsLayer`)
    - Тени под каждой книгой через `BlurMaskFilter`
    - Деревянная текстура полки (drawable ресурс или Compose Canvas градиент)
    - Поддержка горизонтального скролла полки с momentum (fling)

2. **Книжные обложки**
   ```kotlin
   @Composable
   fun Book3DCard(
       book: Book,
       isSelected: Boolean,
       onTap: () -> Unit,
       modifier: Modifier
   ) {
       // Обложка: AsyncImage (Coil) + fallback с gradient + title
       // При тапе: анимация "вытягивания" книги вперёд (translationZ + scaleX/Y)
       // Толщина корешка книги слева — 8-12dp, цвет темнее обложки
       // Бейдж прогресса чтения (% внизу обложки, полупрозрачная полоска)
       // Долгое нажатие → контекстное меню (удалить, переместить полку, инфо)
   }
   ```

3. **Система полок**
   ```kotlin
   data class Shelf(
       val id: String,
       val name: String,           // "Читаю сейчас", "Хочу прочитать", etc.
       val color: Color,           // цвет подсветки полки
       val sortOrder: SortOrder,   // by title / author / date / progress
       val books: List<Book>
   )
   ```
    - До 10 именованных полок, drag & drop между ними
    - Кнопка `+` для создания новой полки с выбором цвета
    - Горизонтальный список полок вверху экрана (chip-style)
    - Анимация перехода между полками (slide + fade)

4. **Сортировка и фильтрация**
    - BottomSheet с вариантами: по названию A-Z, автору, дате добавления, прогрессу, жанру
    - Поиск книг по названию/автору в реальном времени (SearchBar в AppBar)
    - Grid / List / Shelf режим — переключатель в TopAppBar (иконка)
    - Фильтр по формату (EPUB/FB2/PDF/TXT)

5. **Анимации библиотеки**
   ```kotlin
   // При запуске: книги "падают" на полку по одной с easeOut (staggered 50ms)
   // При добавлении новой книги: анимация "вставки" с bounceEffect
   // Parallax эффект при скролле: полка движется медленнее чем список
   ```

---

## 🤖 ЗАДАЧА 2 — AI-панель, не перекрывающая книгу

### Файл: `ui/components/ResizableSplitPane.kt` + `AiAssistantPanel.kt`

**Концепт:** При открытии AI-ассистента экран делится на 2 части (книга слева, AI справа). Пользователь может перетаскивать разделитель.

```kotlin
@Composable
fun ResizableSplitPane(
    modifier: Modifier = Modifier,
    initialReaderWeight: Float = 0.65f,   // 65% книга по умолчанию
    minReaderWeight: Float = 0.3f,         // минимум 30% под книгу
    maxReaderWeight: Float = 0.85f,        // максимум 85% под книгу
    readerContent: @Composable () -> Unit,
    aiContent: @Composable () -> Unit,
    isAiVisible: Boolean
) {
    // Использовать AnimatedVisibility для плавного появления панели AI
    // Разделитель: вертикальная линия 4dp с DragHandle иконкой по центру
    // При drag: обновлять readerWeight через rememberSaveable
    // Haptic feedback при достижении min/max границ
    // Двойной тап по разделителю → сброс к 65/35
}
```

**Панель AI-ассистента:**

```kotlin
@Composable
fun AiAssistantPanel(
    selectedText: String?,           // выделенный текст из книги
    bookContext: BookContext,
    onClose: () -> Unit,
    modifier: Modifier
) {
    Column {
        // 1. Header: "AI Ассистент" + кнопки закрыть/развернуть/свернуть
        // 2. Если есть selectedText → автоматически показать чип с текстом
        //    + быстрые кнопки: "Объяснить", "Перевести", "Краткое содержание"
        // 3. Chat список (LazyColumn, reversed) с MessageBubble компонентами
        // 4. TextField внизу с кнопкой отправки + иконка микрофона (STT)
        // 5. QuickActions (горизонтальный скролл чипов):
        //    "Кто этот персонаж?", "Темп главы", "Факт-чек", "Синонимы"
    }
}
```

**Поведение на разных ориентациях:**
- **Портрет:** панель AI снизу (bottom sheet expandable, 40% высоты)
- **Ландшафт:** панель AI справа (split pane, 35% ширины)
- **Авто-переключение** при повороте с сохранением диалога

---

## 📖 ЗАДАЧА 3 — Экран чтения (ReaderScreen.kt)

**Полный список улучшений:**

### 3.1 Отображение текста
```kotlin
// Кастомный рендерер текста с поддержкой:
// - Межстрочный интервал: 1.0 — 3.0 (шаг 0.1)
// - Межбуквенный интервал (letterSpacing)
// - Отступы страницы: маленький/средний/большой/кастом
// - Выравнивание: По левому краю / По ширине / По центру
// - Колонки: 1 или 2 (для планшетов)
// - Режим пагинации: полистывание или прокрутка
```

### 3.2 Анимации страниц
```kotlin
sealed class PageTurnAnimation {
    object Curl       // 3D загиб угла страницы (Canvas Path)
    object Slide      // горизонтальный слайд
    object Fade       // растворение
    object Flip       // вертикальный флип
    object None       // без анимации (для скорости)
}
// Реализовать PageCurlEffect через Custom Canvas Composable
```

### 3.3 Навигация по книге
- **MiniMap:** выдвижная боковая панель с миниатюрой оглавления
- **Chapter Progress Bar:** тонкая линия в топе (как в Medium)
- **Bookmark Timeline:** визуальная шкала с отметками закладок
- **Swipe вниз:** показать/скрыть панели управления (иммерсивный режим)

### 3.4 Выделение и аннотации
```kotlin
data class Highlight(
    val text: String,
    val color: HighlightColor,   // Yellow, Green, Blue, Pink, Purple
    val note: String?,
    val bookmarkId: String,
    val createdAt: Long
)
// Контекстное меню при выделении:
// [Выделить цветом] [Заметка] [Поиск] [Поделиться] [AI ▶]
// "AI ▶" открывает AiAssistantPanel с этим текстом
```

### 3.5 TTS (Text-to-Speech)
```kotlin
// Floating контрол TTS поверх текста (не перекрывает, прикреплён к низу):
// [◀◀ Предыдущее предложение] [⏸ Пауза] [▶▶ Следующее] [1x скорость]
// Подсветка текущего читаемого предложения (highlight color = Blue)
// Настройки голоса: системный TTS или подключение ElevenLabs API
```

---

## 🎨 ЗАДАЧА 4 — Система тем

### Файл: `ui/theme/Theme.kt`

Реализовать **10 встроенных тем** + возможность создать кастомную:

```kotlin
enum class AppTheme(
    val displayName: String,
    val isDark: Boolean,
    val primaryColor: Color,
    val backgroundColor: Color,
    val surfaceColor: Color,
    val textColor: Color
) {
    DARK_AMOLED("Dark AMOLED", true, Color(0xFF6200EE), Color(0xFF000000), ...),
    DARK_MATERIAL("Dark Material", true, Color(0xFFBB86FC), Color(0xFF121212), ...),
    LIGHT_PAPER("Бумага", false, Color(0xFF795548), Color(0xFFF5F0E8), ...),
    SEPIA("Сепия", false, Color(0xFF6D4C41), Color(0xFFFBF0D9), ...),
    FOREST("Лесной", true, Color(0xFF4CAF50), Color(0xFF0D1B0E), ...),
    OCEAN("Океан", true, Color(0xFF0288D1), Color(0xFF0A1929), ...),
    SUNSET("Закат", false, Color(0xFFE64A19), Color(0xFFFFF8F0), ...),
    MIDNIGHT("Полночь", true, Color(0xFF3F51B5), Color(0xFF0D0D1A), ...),
    ROSE("Розовый", false, Color(0xFFE91E63), Color(0xFFFCE4EC), ...),
    CUSTOM("Кастомная", false, ...) // пользователь выбирает цвета
}
```

**ThemeSelector компонент:**
```kotlin
@Composable
fun ThemeSelector() {
    // Горизонтальная прокрутка карточек тем
    // Каждая карточка: цветной круг + превью текста + название
    // Live preview: при наведении (долгий тап) → предпросмотр темы на 2 сек
    // При выборе: анимированный переход (crossfade 300ms)
    // Кнопка "Создать тему" → ColorPicker диалог (hue slider + brightness)
}
```

---

## ⚙️ ЗАДАЧА 5 — Расширенные настройки (SettingsScreen.kt)

Разбить на секции с expandable cards:

### 5.1 Внешний вид
- [ ] Тема приложения (ThemeSelector)
- [ ] Шрифт для чтения: 8 вариантов (Georgia, Lato, Merriweather, OpenDyslexic, Roboto, SourceSerif, PT Serif, Montserrat)
- [ ] Размер шрифта: слайдер 10-36sp
- [ ] Яркость экрана: слайдер (независимо от системной)
- [ ] Синяя подсветка: фильтр тёплого цвета (overlay с alpha)
- [ ] Режим E-Ink (отключить анимации, чёрно-белое)

### 5.2 Чтение
- [ ] Направление листания: горизонтальное / вертикальное
- [ ] Анимация страниц (PageTurnAnimation selector)
- [ ] Тап-зоны: настройка зон экрана для перелистывания
- [ ] Жесты: свайп вверх/вниз = яркость, свайп лево/право = страница
- [ ] Автолистание: включить + скорость (секунды на страницу)
- [ ] Запоминать позицию чтения (включено по умолчанию)

### 5.3 AI Ассистент
- [ ] Выбор модели: Gemini / Claude / GPT-4o / локальная (Ollama URL)
- [ ] API Key поле (зашифровано в EncryptedSharedPreferences)
- [ ] Язык ответов AI (авто / русский / украинский / английский)
- [ ] Автоматически анализировать выделенный текст (toggle)
- [ ] История чатов: хранить / не хранить

### 5.4 Библиотека
- [ ] Папки сканирования для книг
- [ ] Обложки: загружать из сети / только локальные / генерировать (заглушка)
- [ ] Формат резервной копии: JSON экспорт/импорт прогресса и закладок
- [ ] Облачная синхронизация (Dropbox / Google Drive интеграция)

### 5.5 Статистика чтения
```kotlin
// Экран статистики (новый):
// - Часов прочитано эту неделю (bar chart — Vico библиотека)
// - Книг завершено за год
// - Streak: дней подряд чтения (calendar heatmap)
// - Средняя скорость чтения (слов/минуту)
// - Топ-5 авторов по времени
```

---

## 🔔 ЗАДАЧА 6 — UX детали и микроанимации

```kotlin
// 1. Splash screen: анимированный логотип (книга открывается)
//    Использовать SplashScreen API (Android 12+) + Lottie анимацию

// 2. Empty states:
//    - Пустая библиотека: иллюстрация + "Добавьте первую книгу" + CTA кнопка
//    - Нет результатов поиска: иллюстрация + совет

// 3. Onboarding (3 экрана при первом запуске):
//    Экран 1: "Ваша библиотека" — показ полки
//    Экран 2: "AI Ассистент" — демо split-panel
//    Экран 3: "Настройте под себя" — выбор темы

// 4. Loading states:
//    - Скелетон книжных обложек при загрузке библиотеки
//    - Shimmer эффект (аналог Facebook skeleton)
//    - AI думает → анимированные точки + "Анализирую..."

// 5. Haptic feedback:
//    - При перелистывании страницы: лёгкий тик (HapticFeedbackType.TextHandleMove)
//    - При открытии AI: средний (HapticFeedbackType.LongPress)
//    - При достижении конца книги: паттерн "завершение"

// 6. Floating Action Button:
//    - На экране библиотеки: FAB "Добавить книгу" с expandable меню
//      (Из файлов / Из облака / По ссылке / Поиск онлайн)
//    - Анимация раскрытия FAB меню (SpeedDial pattern)
```

---

## 📦 Зависимости для добавления в build.gradle.kts

```kotlin
dependencies {
    // Изображения
    implementation("io.coil-kt:coil-compose:2.6.0")
    
    // Lottie анимации (splash, empty states)
    implementation("com.airbnb.android:lottie-compose:6.4.0")
    
    // Графики статистики
    implementation("com.patrykandpatrick.vico:compose-m3:1.15.0")
    
    // Выбор цвета для кастомных тем  
    implementation("com.github.skydoves:colorpicker-compose:1.0.7")
    
    // Drag & drop (для полок)
    implementation("org.burnoutcrew.composereorderable:reorderable:0.9.6")
    
    // Шифрование API ключей
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
}
```

---

## 🏗️ Архитектурные требования

- **MVVM** с `ViewModel` + `StateFlow` для каждого экрана
- **UiState** sealed class для каждого экрана (Loading / Success / Error)
- `ResizableSplitPane` — хранить пропорцию в `DataStore`, переживает перезапуск
- Тема — хранить в `DataStore<AppTheme>`, применять через `CompositionLocalProvider`
- `BookShelf3D` — использовать `LazyRow` внутри, не кастомный Layout (производительность)
- AI панель — отдельный `ViewModel` с `chatHistory: List<Message>` в памяти

---

## ✅ Приоритет выполнения

| Приоритет | Задача | Файл |
|-----------|--------|------|
| 🔴 P0 | ResizableSplitPane + AiAssistantPanel | `ResizableSplitPane.kt`, `AiAssistantPanel.kt` |
| 🔴 P0 | 3D Book Shelf базовая версия | `BookShelf3D.kt`, `LibraryScreen.kt` |
| 🟠 P1 | Система тем (10 штук) | `Theme.kt`, `ThemeSelector.kt` |
| 🟠 P1 | ReaderScreen: выделение + highlights | `ReaderScreen.kt` |
| 🟡 P2 | Настройки шрифтов, отступов, анимаций | `SettingsScreen.kt` |
| 🟡 P2 | TTS контролы | `TtsController.kt` (новый) |
| 🟢 P3 | Статистика чтения | `StatsScreen.kt` (новый) |
| 🟢 P3 | Onboarding | `OnboardingScreen.kt` (новый) |
| 🔵 P4 | Анимация загиба страницы | `PageCurlEffect.kt` (новый) |
| 🔵 P4 | Облачная синхронизация | `SyncRepository.kt` (новый) |

---

## 🎯 Критерии качества

- [ ] Все анимации плавные 60fps (`spring()` и `tween()` из Compose Animation)
- [ ] Поддержка Dynamic Color (Material You, Android 12+)
- [ ] Полная поддержка тёмной/светлой темы в каждом компоненте
- [ ] Отсутствие лишних recompositions (`remember`, `derivedStateOf`)
- [ ] Accessibility: contentDescription на всех интерактивных элементах
- [ ] Поддержка tablet layout (WindowSizeClass: Compact/Medium/Expanded)
- [ ] Минимальный API: 26 (Android 8.0)

---

*Промпт создан для проекта [AI-Reader / android-bookmind](https://github.com/Totsamuychel/AI-Reader) · June 2026*