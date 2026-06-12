package com.bookmind.ui.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookmind.core.BookImportService
import com.bookmind.core.model.Book
import com.bookmind.core.parser.BookParserFactory
import com.bookmind.memory.IngestionService
import com.bookmind.persistence.BookStoring
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(
    val books: List<Book> = emptyList(),
    val isImporting: Boolean = false,
    val importStatus: String? = null
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val bookStore: BookStoring,
    private val importService: BookImportService,
    private val parserFactory: BookParserFactory,
    private val ingestionService: IngestionService
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(books = bookStore.listBooks()) }
        }
    }

    /** Import → parse chapters → ingest into spoiler-safe memory. */
    fun importAndIngest(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, importStatus = "Importing…") }
            runCatching {
                val book = importService.importBook(uri)
                val chapters = parserFactory.parser(book.format).parseChapters(book)
                bookStore.saveChapters(chapters, book.id)
                ingestionService.ingest(book, chapters).collect { progress ->
                    val status = if (progress.isComplete) {
                        "Indexed ${progress.totalChapters} chapters"
                    } else {
                        "Indexing ${progress.chapterIndex + 1}/${progress.totalChapters}: ${progress.chapterTitle}"
                    }
                    _uiState.update { it.copy(importStatus = status) }
                }
            }.onFailure { t ->
                _uiState.update { it.copy(importStatus = "Import failed: ${t.message}") }
            }
            _uiState.update { it.copy(isImporting = false) }
            refresh()
        }
    }
}
