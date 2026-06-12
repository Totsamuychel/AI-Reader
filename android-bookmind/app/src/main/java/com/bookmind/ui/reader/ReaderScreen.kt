package com.bookmind.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bookmind.core.model.UserQuote
import com.bookmind.ui.assistant.AssistantPanel
import com.bookmind.ui.assistant.AssistantViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    bookId: String,
    viewModel: ReaderViewModel = hiltViewModel(),
    assistantViewModel: AssistantViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val assistantState by assistantViewModel.uiState.collectAsStateWithLifecycle()

    var chatOpen by remember { mutableStateOf(false) }
    var quotesOpen by remember { mutableStateOf(false) }
    // Read-only text field: edits are rejected, but the selection is observable,
    // which is what powers "save quote" / "ask AI" on a highlighted passage.
    var fieldValue by remember { mutableStateOf(TextFieldValue("")) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(bookId) { viewModel.open(bookId) }
    LaunchedEffect(uiState.chapterText) { fieldValue = TextFieldValue(uiState.chapterText) }
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

    val selectedText = fieldValue.selection.let { sel ->
        if (sel.collapsed) "" else fieldValue.text.substring(sel.min, sel.max)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
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
                actions = {
                    IconButton(onClick = { quotesOpen = true }) {
                        Icon(Icons.Default.FormatQuote, contentDescription = "Quotes")
                    }
                    IconButton(onClick = { chatOpen = !chatOpen }) {
                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Assistant")
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = viewModel::previousChapter) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous")
                }
                Text(
                    "${uiState.currentChapterIndex + 1} / ${uiState.chapters.size}",
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
                IconButton(onClick = viewModel::nextChapter) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next")
                }
            }
        }
    ) { padding ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(padding)) {
            val sideBySide = maxWidth >= 720.dp
            val panelWidth = if (sideBySide) 360.dp else maxWidth * 0.88f

            Row(modifier = Modifier.fillMaxSize()) {
                ReaderBody(
                    isLoading = uiState.isLoading,
                    fieldValue = fieldValue,
                    onFieldValueChange = { fieldValue = it },
                    selectedText = selectedText,
                    onSaveQuote = { viewModel.saveQuote(selectedText) },
                    onAskAi = {
                        assistantViewModel.askAboutPassage(selectedText)
                        chatOpen = true
                    },
                    modifier = Modifier.weight(1f)
                )
                if (sideBySide) {
                    AnimatedVisibility(
                        visible = chatOpen,
                        enter = slideInHorizontally { it },
                        exit = slideOutHorizontally { it }
                    ) {
                        Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxHeight()) {
                            AssistantPanel(
                                uiState = assistantState,
                                onInputChange = assistantViewModel::onInputChange,
                                onSend = assistantViewModel::sendMessage,
                                onModeChange = assistantViewModel::setMode,
                                onWebToggle = assistantViewModel::setWebEnabled,
                                onClose = { chatOpen = false },
                                modifier = Modifier.width(panelWidth)
                            )
                        }
                    }
                }
            }

            // Narrow screens: the panel slides over the reader from the right edge.
            if (!sideBySide) {
                AnimatedVisibility(
                    visible = chatOpen,
                    modifier = Modifier.align(Alignment.CenterEnd),
                    enter = slideInHorizontally { it },
                    exit = slideOutHorizontally { it }
                ) {
                    Surface(tonalElevation = 4.dp, shadowElevation = 8.dp, modifier = Modifier.fillMaxHeight()) {
                        AssistantPanel(
                            uiState = assistantState,
                            onInputChange = assistantViewModel::onInputChange,
                            onSend = assistantViewModel::sendMessage,
                            onModeChange = assistantViewModel::setMode,
                            onWebToggle = assistantViewModel::setWebEnabled,
                            onClose = { chatOpen = false },
                            modifier = Modifier.width(panelWidth)
                        )
                    }
                }
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
}

@Composable
private fun ReaderBody(
    isLoading: Boolean,
    fieldValue: TextFieldValue,
    onFieldValueChange: (TextFieldValue) -> Unit,
    selectedText: String,
    onSaveQuote: () -> Unit,
    onAskAi: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxHeight()) {
        if (isLoading) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) { CircularProgressIndicator() }
        } else {
            BasicTextField(
                value = fieldValue,
                // readOnly rejects edits but still reports selection changes.
                onValueChange = onFieldValueChange,
                readOnly = true,
                textStyle = TextStyle(
                    fontSize = 17.sp,
                    lineHeight = 26.sp,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState())
            )
        }

        AnimatedVisibility(visible = selectedText.isNotBlank()) {
            Surface(tonalElevation = 2.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onSaveQuote) {
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
                "No quotes yet. Select a passage in the text and tap “Save quote”.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(quotes, key = { it.id }) { quote ->
                    Card {
                        Column(modifier = Modifier.padding(12.dp)) {
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
