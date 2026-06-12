package com.bookmind.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookmind.core.ReaderSession
import com.bookmind.core.model.Book
import com.bookmind.core.model.BookID
import com.bookmind.core.model.Chapter
import com.bookmind.core.model.UserQuote
import com.bookmind.core.parser.BookParserFactory
import com.bookmind.persistence.BookStoring
import com.bookmind.persistence.QuoteStoring
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ReaderUiState(
    val book: Book? = null,
    val chapters: List<Chapter> = emptyList(),
    val currentChapter: Chapter? = null,
    val chapterText: String = "",
    val isLoading: Boolean = true,
    val quotes: List<UserQuote> = emptyList(),
    val quoteSavedToast: Boolean = false
) {
    val currentChapterIndex: Int get() = currentChapter?.index ?: 0
}

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val bookStore: BookStoring,
    private val readerSession: ReaderSession,
    private val parserFactory: BookParserFactory,
    private val quoteStore: QuoteStoring
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    private var quotesJob: Job? = null

    fun open(bookId: String) {
        viewModelScope.launch {
            val book = bookStore.book(BookID(bookId)) ?: run {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }
            val state = readerSession.open(book)
            _uiState.update {
                it.copy(
                    book = state.book,
                    chapters = state.chapters,
                    currentChapter = state.currentChapter,
                    isLoading = false
                )
            }
            state.currentChapter?.let { loadChapterText(state.book, it) }
        }
        observeQuotes(bookId)
    }

    private fun observeQuotes(bookId: String) {
        quotesJob?.cancel()
        quotesJob = viewModelScope.launch {
            quoteStore.observeQuotes(BookID(bookId)).collect { quotes ->
                _uiState.update { it.copy(quotes = quotes) }
            }
        }
    }

    /** Saves a highlighted passage as a quote at the current position. */
    fun saveQuote(text: String) {
        val state = _uiState.value
        val book = state.book ?: return
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            quoteStore.save(
                UserQuote(
                    id = UUID.randomUUID().toString(),
                    bookID = book.id,
                    chapterID = state.currentChapter?.id,
                    chapterIndex = state.currentChapterIndex,
                    text = trimmed,
                    createdAt = System.currentTimeMillis()
                )
            )
            _uiState.update { it.copy(quoteSavedToast = true) }
        }
    }

    fun consumeQuoteSavedToast() = _uiState.update { it.copy(quoteSavedToast = false) }

    fun deleteQuote(quoteId: String) {
        viewModelScope.launch { quoteStore.delete(quoteId) }
    }

    fun goToChapter(chapter: Chapter) {
        val state = _uiState.value
        val book = state.book ?: return
        _uiState.update { it.copy(currentChapter = chapter) }
        viewModelScope.launch {
            readerSession.saveProgress(book, chapter, state.chapters)
            loadChapterText(book, chapter)
        }
    }

    fun nextChapter() = shift(+1)
    fun previousChapter() = shift(-1)

    private fun shift(delta: Int) {
        val state = _uiState.value
        val current = state.currentChapter ?: return
        val idx = state.chapters.indexOfFirst { it.id == current.id }
        val target = state.chapters.getOrNull(idx + delta) ?: return
        goToChapter(target)
    }

    private suspend fun loadChapterText(book: Book, chapter: Chapter) {
        val text = runCatching { parserFactory.parser(book.format).rawText(chapter, book) }
            .getOrDefault("")
        _uiState.update { it.copy(chapterText = text) }
    }
}
