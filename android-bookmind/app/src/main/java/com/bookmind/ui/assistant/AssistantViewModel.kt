package com.bookmind.ui.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookmind.core.model.BookID
import com.bookmind.llm.AnswerEvent
import com.bookmind.llm.AnswerService
import com.bookmind.persistence.BookStoring
import com.bookmind.safety.AnswerMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val mode: AnswerMode = AnswerMode.SAFE
)

@HiltViewModel
class AssistantViewModel @Inject constructor(
    private val answerService: AnswerService,
    private val bookStore: BookStoring
) : ViewModel() {

    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    private var bookId: String = ""

    fun configure(bookId: String, currentChapterIndex: Int) {
        this.bookId = bookId
        _uiState.update { it.copy(currentChapter = currentChapterIndex) }
        viewModelScope.launch {
            val title = bookStore.book(BookID(bookId))?.title ?: "this book"
            _uiState.update { it.copy(bookTitle = title) }
        }
    }

    fun onInputChange(value: String) = _uiState.update { it.copy(inputText = value) }

    fun setMode(mode: AnswerMode) {
        answerService.mode = mode
        _uiState.update { it.copy(mode = mode) }
    }

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

        viewModelScope.launch {
            answerService.mode = state.mode
            answerService
                .answerStream(question, BookID(bookId), state.currentChapter)
                .collect { event ->
                    when (event) {
                        is AnswerEvent.ContextRetrieved -> Unit
                        is AnswerEvent.Token ->
                            _uiState.update { it.copy(streamingText = it.streamingText + event.token) }
                        is AnswerEvent.Complete -> _uiState.update {
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
}
