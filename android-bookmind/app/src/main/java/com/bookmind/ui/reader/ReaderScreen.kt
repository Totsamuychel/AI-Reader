package com.bookmind.ui.reader

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bookmind.core.model.HighlightColor
import com.bookmind.core.model.UserQuote
import com.bookmind.settings.AppSettings
import com.bookmind.settings.ScrollDirection
import com.bookmind.ui.assistant.AssistantPanel
import com.bookmind.ui.assistant.AssistantViewModel
import com.bookmind.ui.components.ResizableSplitPane
import com.bookmind.ui.settings.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    bookId: String,
    onBack: () -> Unit = {},
    onOpenStats: () -> Unit = {},
    viewModel: ReaderViewModel = hiltViewModel(),
    assistantViewModel: AssistantViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val assistantState by assistantViewModel.uiState.collectAsStateWithLifecycle()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()

    var chatOpen by remember { mutableStateOf(false) }
    var quotesOpen by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    // Hoisted so the chapter scroll position can drive the chrome auto-hide.
    val scrollState = rememberScrollState()
    val tts = rememberTtsController()
    // The highlighted passage, reported by whichever reader mode is active. It
    // powers "save quote" / "ask AI".
    var selectedText by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(bookId) { viewModel.open(bookId) }
    LaunchedEffect(uiState.chapterText) { selectedText = "" }
    LaunchedEffect(bookId, chatOpen) {
        if (chatOpen) assistantViewModel.configure(bookId, uiState.currentChapterIndex)
    }
    LaunchedEffect(uiState.currentChapterIndex) {
        assistantViewModel.updateChapter(uiState.currentChapterIndex)
    }
    LaunchedEffect(uiState.quoteSavedToast) {
        if (uiState.quoteSavedToast) {
            viewModel.consumeQuoteSavedToast()
            snackbarHostState.showSnackbar("Quote saved")
        }
    }

    // Auto-open the assistant on selection when the user opted in.
    LaunchedEffect(selectedText) {
        if (settings.autoAnalyzeSelection && selectedText.isNotBlank()) chatOpen = true
    }

    // System back: dismiss the assistant panel first, otherwise leave the reader.
    BackHandler {
        when {
            chatOpen -> chatOpen = false
            else -> onBack()
        }
    }

    val chapterCount = uiState.chapters.size
    val chapterProgress = if (chapterCount <= 1) 0f
    else (uiState.currentChapterIndex + 1).toFloat() / chapterCount

    // In continuous mode the chrome slides away once the reader scrolls into the
    // chapter, giving an immersive full-screen page; it returns near the top.
    val immersiveScroll = settings.scrollDirection == ScrollDirection.VERTICAL
    val chromeVisible by remember(immersiveScroll) {
        derivedStateOf { !immersiveScroll || scrollState.value < 60 }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AnimatedVisibility(
                visible = chromeVisible,
                enter = slideInVertically { -it },
                exit = slideOutVertically { -it }
            ) {
              Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(uiState.book?.title ?: "Reader", style = MaterialTheme.typography.titleMedium)
                            uiState.currentChapter?.let {
                                Text(
                                    it.title ?: "Chapter ${it.index + 1}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            if (tts.isSpeaking) tts.pause() else tts.start(uiState.chapterText)
                        }) {
                            Icon(Icons.Default.RecordVoiceOver, contentDescription = "Read aloud")
                        }
                        IconButton(onClick = { quotesOpen = true }) {
                            Icon(Icons.Default.FormatQuote, contentDescription = "Quotes")
                        }
                        IconButton(onClick = { chatOpen = !chatOpen }) {
                            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Assistant")
                        }
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Меню")
                            }
                            DropdownMenu(
                                expanded = menuOpen,
                                onDismissRequest = { menuOpen = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Оформление") },
                                    leadingIcon = { Icon(Icons.Default.TextFields, contentDescription = null) },
                                    onClick = { menuOpen = false; settingsOpen = true }
                                )
                                DropdownMenuItem(
                                    text = { Text("Статистика") },
                                    leadingIcon = { Icon(Icons.Default.BarChart, contentDescription = null) },
                                    onClick = { menuOpen = false; onOpenStats() }
                                )
                            }
                        }
                    }
                )
                // Chapter progress bar (Medium-style thin line).
                LinearProgressIndicator(
                    progress = { chapterProgress },
                    modifier = Modifier.fillMaxWidth().height(2.dp)
                )
              }
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = chromeVisible,
                enter = slideInVertically { it },
                exit = slideOutVertically { it }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = viewModel::previousChapter) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous")
                    }
                    Text(
                        "${uiState.currentChapterIndex + 1} / $chapterCount",
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                    IconButton(onClick = viewModel::nextChapter) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next")
                    }
                }
            }
        }
    ) { padding ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(padding)) {
            val sideBySide = maxWidth >= 720.dp

            val body: @Composable () -> Unit = {
                ReaderBody(
                    isLoading = uiState.isLoading,
                    chapterText = uiState.chapterText,
                    selectedText = selectedText,
                    settings = settings,
                    scrollState = scrollState,
                    onSelectionChange = { selectedText = it },
                    onSaveQuote = { color -> viewModel.saveQuote(selectedText, color) },
                    onAskAi = {
                        assistantViewModel.askAboutPassage(selectedText)
                        chatOpen = true
                    }
                )
            }

            val panel: @Composable () -> Unit = {
                AssistantPanel(
                    uiState = assistantState,
                    onInputChange = assistantViewModel::onInputChange,
                    onSend = assistantViewModel::sendMessage,
                    onModeChange = assistantViewModel::setMode,
                    onWebToggle = assistantViewModel::setWebEnabled,
                    onClose = { chatOpen = false },
                    selectedText = selectedText.ifBlank { null },
                    onQuickAction = assistantViewModel::quickAsk,
                    onClearHistory = assistantViewModel::clearHistory
                )
            }

            if (sideBySide) {
                // Landscape / tablet: resizable split pane, weight persisted.
                ResizableSplitPane(
                    readerWeight = settings.readerWeight,
                    onWeightChange = settingsViewModel::setReaderWeight,
                    isAiVisible = chatOpen,
                    readerContent = { Box(modifier = Modifier.fillMaxSize()) { body() } },
                    aiContent = { Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxSize()) { panel() } }
                )
            } else {
                // Portrait: reader fills the screen; panel slides over from the right.
                Box(modifier = Modifier.fillMaxSize()) { body() }
                AnimatedVisibility(
                    visible = chatOpen,
                    modifier = Modifier.align(Alignment.CenterEnd),
                    enter = slideInHorizontally { it },
                    exit = slideOutHorizontally { it }
                ) {
                    Surface(
                        tonalElevation = 4.dp,
                        shadowElevation = 8.dp,
                        modifier = Modifier.fillMaxHeight().width(maxWidth * 0.88f)
                    ) { panel() }
                }
            }

            AnimatedVisibility(
                visible = tts.isSpeaking || tts.currentSentence >= 0,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInHorizontally { it / 2 },
                exit = slideOutHorizontally { it / 2 }
            ) {
                TtsControls(
                    isSpeaking = tts.isSpeaking,
                    rate = tts.speechRate,
                    onPrevious = tts::previous,
                    onToggle = { if (tts.isSpeaking) tts.pause() else tts.resume() },
                    onNext = tts::next,
                    onCycleRate = {
                        val rates = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                        val next = rates[(rates.indexOf(tts.speechRate).coerceAtLeast(0) + 1) % rates.size]
                        tts.setRate(next)
                    }
                )
            }
        }
    }

    if (quotesOpen) {
        ModalBottomSheet(onDismissRequest = { quotesOpen = false }) {
            QuotesList(
                quotes = uiState.quotes,
                onDelete = viewModel::deleteQuote,
                onAsk = { quote ->
                    assistantViewModel.askAboutPassage(quote.text)
                    quotesOpen = false
                    chatOpen = true
                }
            )
        }
    }

    if (settingsOpen) {
        ModalBottomSheet(onDismissRequest = { settingsOpen = false }) {
            ReaderSettingsSheet(
                settings = settings,
                onFontChange = settingsViewModel::setFont,
                onFontSizeChange = settingsViewModel::setFontSize,
                onLineSpacingChange = settingsViewModel::setLineSpacing,
                onBackgroundChange = settingsViewModel::setReaderBackground,
                onAnimationChange = settingsViewModel::setPageAnimation,
                onScrollDirectionChange = settingsViewModel::setScrollDirection,
                onWarmthChange = settingsViewModel::setWarmth
            )
        }
    }
}

@Composable
private fun ReaderBody(
    isLoading: Boolean,
    chapterText: String,
    selectedText: String,
    settings: AppSettings,
    scrollState: androidx.compose.foundation.ScrollState,
    onSelectionChange: (String) -> Unit,
    onSaveQuote: (HighlightColor) -> Unit,
    onAskAi: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Page background / text color preset (or fall back to the app theme).
    val bgColor = settings.readerBackground.bgArgb?.let { Color(it) }
        ?: MaterialTheme.colorScheme.background
    val fgColor = settings.readerBackground.fgArgb?.let { Color(it) }
        ?: MaterialTheme.colorScheme.onBackground
    val warmth = settings.warmth.coerceIn(0f, 1f)

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(bgColor)
            // Warm "night" tint painted over the page content.
            .drawWithContent {
                drawContent()
                if (warmth > 0f) {
                    drawRect(Color(1f, 0.78f, 0.43f, warmth * 0.45f))
                }
            }
    ) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                isLoading -> Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) { CircularProgressIndicator() }

                // Continuous scrolling — the proven, selectable reading mode.
                settings.scrollDirection == ScrollDirection.VERTICAL ->
                    ContinuousReader(chapterText, settings, fgColor, scrollState, onSelectionChange)

                // Page-flipping with the chosen turn animation.
                else -> PagedReader(
                    text = chapterText,
                    settings = settings,
                    contentColor = fgColor,
                    onSelectionChange = onSelectionChange
                )
            }
        }

        AnimatedVisibility(visible = selectedText.isNotBlank()) {
            SelectionActionBar(onSaveQuote = onSaveQuote, onAskAi = onAskAi)
        }
    }
}

@Composable
private fun ContinuousReader(
    chapterText: String,
    settings: AppSettings,
    contentColor: Color,
    scrollState: androidx.compose.foundation.ScrollState,
    onSelectionChange: (String) -> Unit
) {
    var fieldValue by remember(chapterText) { mutableStateOf(TextFieldValue(chapterText)) }
    val selected = fieldValue.selection.let { sel ->
        if (sel.collapsed) "" else fieldValue.text.substring(sel.min, sel.max)
    }
    LaunchedEffect(selected) { onSelectionChange(selected) }

    val quoteColor = MaterialTheme.colorScheme.primary
    val transformation = remember(quoteColor) { ReaderVisualTransformation(quoteColor) }

    BasicTextField(
        value = fieldValue,
        onValueChange = { fieldValue = it }, // readOnly rejects edits, keeps selection
        readOnly = true,
        visualTransformation = transformation,
        textStyle = TextStyle(
            fontFamily = settings.readerFont.fontFamily,
            fontSize = settings.fontSizeSp.sp,
            lineHeight = settings.fontSizeSp.times(settings.lineSpacing).sp,
            letterSpacing = 0.em,
            color = contentColor
        ),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(scrollState)
    )
}

@Composable
private fun SelectionActionBar(
    onSaveQuote: (HighlightColor) -> Unit,
    onAskAi: () -> Unit
) {
    Surface(tonalElevation = 2.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Highlight:", style = MaterialTheme.typography.labelMedium)
                HighlightColor.entries.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(color.rgb))
                            .clickable { onSaveQuote(color) }
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onSaveQuote(HighlightColor.YELLOW) }) {
                    Icon(Icons.Default.FormatQuote, contentDescription = null)
                    Text(" Save quote")
                }
                TextButton(onClick = onAskAi) {
                    Icon(Icons.Default.QuestionAnswer, contentDescription = null)
                    Text(" Ask AI")
                }
            }
        }
    }
}

@Composable
private fun TtsControls(
    isSpeaking: Boolean,
    rate: Float,
    onPrevious: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onCycleRate: () -> Unit
) {
    Surface(
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        modifier = Modifier.padding(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(onClick = onPrevious) {
                Icon(Icons.Default.FastRewind, contentDescription = "Previous sentence")
            }
            IconButton(onClick = onToggle) {
                Icon(
                    if (isSpeaking) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isSpeaking) "Pause" else "Play"
                )
            }
            IconButton(onClick = onNext) {
                Icon(Icons.Default.FastForward, contentDescription = "Next sentence")
            }
            TextButton(onClick = onCycleRate) {
                Text(String.format("%.2g×", rate))
            }
        }
    }
}

@Composable
private fun QuotesList(
    quotes: List<UserQuote>,
    onDelete: (String) -> Unit,
    onAsk: (UserQuote) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text("Saved quotes", style = MaterialTheme.typography.titleMedium)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        if (quotes.isEmpty()) {
            Text(
                "No quotes yet. Select a passage in the text and tap a highlight color.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(quotes, key = { it.id }) { quote ->
                    Card {
                        Row(modifier = Modifier.padding(12.dp)) {
                            Box(
                                modifier = Modifier
                                    .width(6.dp)
                                    .fillMaxHeight()
                                    .background(Color(quote.color.rgb))
                            )
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Text(
                                    "Chapter ${quote.chapterIndex + 1}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text("“${quote.text}”", style = MaterialTheme.typography.bodyMedium)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TextButton(onClick = { onAsk(quote) }) { Text("Ask AI") }
                                    IconButton(onClick = { onDelete(quote.id) }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete quote",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
