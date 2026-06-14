package com.bookmind.ui.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookmind.core.model.BookID
import com.bookmind.llm.AnswerEvent
import com.bookmind.llm.AnswerService
import com.bookmind.persistence.BookStoring
import com.bookmind.persistence.ChatHistoryStore
import com.bookmind.safety.AnswerMode
import com.bookmind.settings.SettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatMessage(val text: String, val fromUser: Boolean, val spoilerFlagged: Boolean = false)

data class AssistantUiState(
    val bookTitle: String = "",
    val currentChapter: Int = 0,
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isGenerating: Boolean = false,
    val streamingText: String = "",
    val mode: AnswerMode = AnswerMode.SAFE,
    val webEnabled: Boolean = false
)

@HiltViewModel
class AssistantViewModel @Inject constructor(
    private val answerService: AnswerService,
    private val bookStore: BookStoring,
    private val chatHistory: ChatHistoryStore,
    private val settingsStore: SettingsStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    private var bookId: String = ""
    private var historyJob: Job? = null

    fun configure(bookId: String, currentChapterIndex: Int) {
        this.bookId = bookId
        _uiState.update { it.copy(currentChapter = currentChapterIndex) }
        viewModelScope.launch {
            val title = bookStore.book(BookID(bookId))?.title ?: "this book"
            _uiState.update { it.copy(bookTitle = title) }
        }
        loadHistory(bookId)
    }

    /** Seeds the conversation from the saved per-book history (once, if empty). */
    private fun loadHistory(bookId: String) {
        historyJob?.cancel()
        historyJob = viewModelScope.launch {
            if (!settingsStore.settings.first().keepChatHistory) return@launch
            val history = chatHistory.forBook(BookID(bookId)).map {
                ChatMessage(it.content, fromUser = it.role == ROLE_USER, spoilerFlagged = it.spoilerFlagged)
            }
            _uiState.update { state ->
                if (state.messages.isEmpty()) state.copy(messages = history) else state
            }
        }
    }

    /** Persists one message if the user keeps chat history. */
    private fun persist(role: String, content: String, spoilerFlagged: Boolean = false) {
        val id = bookId
        if (id.isEmpty()) return
        viewModelScope.launch {
            if (settingsStore.settings.first().keepChatHistory) {
                chatHistory.add(BookID(id), role, content, spoilerFlagged)
            }
        }
    }

    /** Clears the on-screen and persisted history for the current book. */
    fun clearHistory() {
        val id = bookId
        _uiState.update { it.copy(messages = emptyList(), streamingText = "") }
        if (id.isEmpty()) return
        viewModelScope.launch { chatHistory.clear(BookID(id)) }
    }

    fun onInputChange(value: String) = _uiState.update { it.copy(inputText = value) }

    fun setMode(mode: AnswerMode) {
        answerService.mode = mode
        _uiState.update { it.copy(mode = mode) }
    }

    fun setWebEnabled(enabled: Boolean) {
        answerService.webEnabled = enabled
        _uiState.update { it.copy(webEnabled = enabled) }
    }

    /** Pre-fills the input, e.g. when the reader asks about a highlighted passage. */
    fun askAboutPassage(passage: String) {
        val excerpt = passage.trim().take(400)
        if (excerpt.isEmpty()) return
        _uiState.update { it.copy(inputText = "Explain this passage: \"$excerpt\"") }
    }

    /** Quick-action chips: drop in a ready-made prompt and send it immediately. */
    fun quickAsk(prompt: String) {
        if (prompt.isBlank() || _uiState.value.isGenerating) return
        _uiState.update { it.copy(inputText = prompt) }
        sendMessage()
    }

    fun updateChapter(currentChapterIndex: Int) =
        _uiState.update { it.copy(currentChapter = currentChapterIndex) }

    fun sendMessage() {
        val state = _uiState.value
        val question = state.inputText.trim()
        if (question.isEmpty() || state.isGenerating || bookId.isEmpty()) return

        _uiState.update {
            it.copy(
                messages = it.messages + ChatMessage(question, fromUser = true),
                inputText = "",
                isGenerating = true,
                streamingText = ""
            )
        }
        persist(ROLE_USER, question)

        viewModelScope.launch {
            answerService.mode = state.mode
            answerService
                .answerStream(question, BookID(bookId), state.currentChapter)
                .collect { event ->
                    when (event) {
                        is AnswerEvent.ContextRetrieved -> Unit
                        is AnswerEvent.Token ->
                            _uiState.update { it.copy(streamingText = it.streamingText + event.token) }
                        is AnswerEvent.Complete -> {
                            _uiState.update {
                                it.copy(
                                    messages = it.messages + ChatMessage(
                                        event.fullText,
                                        fromUser = false,
                                        spoilerFlagged = event.spoilerFlagged
                                    ),
                                    isGenerating = false,
                                    streamingText = ""
                                )
                            }
                            persist(ROLE_ASSISTANT, event.fullText, event.spoilerFlagged)
                        }
                        is AnswerEvent.Failed -> _uiState.update {
                            it.copy(
                                messages = it.messages + ChatMessage("⚠️ ${event.message}", fromUser = false),
                                isGenerating = false,
                                streamingText = ""
                            )
                        }
                    }
                }
        }
    }

    private companion object {
        const val ROLE_USER = "user"
        const val ROLE_ASSISTANT = "assistant"
    }
}
