package com.bookmind.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookmind.core.ReaderSession
import com.bookmind.core.model.Book
import com.bookmind.core.model.BookID
import com.bookmind.core.model.Chapter
import com.bookmind.core.parser.BookParserFactory
import com.bookmind.persistence.BookStoring
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReaderUiState(
    val book: Book? = null,
    val chapters: List<Chapter> = emptyList(),
    val currentChapter: Chapter? = null,
    val chapterText: String = "",
    val isLoading: Boolean = true
) {
    val currentChapterIndex: Int get() = currentChapter?.index ?: 0
}

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val bookStore: BookStoring,
    private val readerSession: ReaderSession,
    private val parserFactory: BookParserFactory
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

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
