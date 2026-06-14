package com.bookmind.ui.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookmind.core.BookImportService
import com.bookmind.core.model.Book
import com.bookmind.core.model.BookID
import com.bookmind.core.model.Shelf
import com.bookmind.core.parser.BookParserFactory
import com.bookmind.memory.IngestionService
import com.bookmind.persistence.BookStoring
import com.bookmind.persistence.LibraryRepository
import com.bookmind.persistence.ReadingProgressStoring
import com.bookmind.ui.components.ShelfBook
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

enum class LibraryViewMode { SHELF, GRID, LIST }

enum class BookSort(val displayName: String) {
    RECENT("Recently added"),
    TITLE("Title A–Z"),
    AUTHOR("Author"),
    PROGRESS("Progress")
}

data class LibraryUiState(
    val books: List<ShelfBook> = emptyList(),
    val shelves: List<Shelf> = emptyList(),
    val selectedShelfId: String? = null, // null = All
    val isImporting: Boolean = false,
    val importStatus: String? = null,
    val query: String = "",
    val viewMode: LibraryViewMode = LibraryViewMode.SHELF,
    val sort: BookSort = BookSort.RECENT
) {
    /** Books after shelf filter + search + sort, ready to render. */
    val visibleBooks: List<ShelfBook>
        get() {
            val byShelf = when (selectedShelfId) {
                null -> books
                else -> books.filter { it.book.shelfId == selectedShelfId }
            }
            val filtered = if (query.isBlank()) byShelf else byShelf.filter {
                it.book.title.contains(query, ignoreCase = true) ||
                    (it.book.author?.contains(query, ignoreCase = true) == true)
            }
            return when (sort) {
                BookSort.RECENT -> filtered.sortedByDescending { it.book.addedAt }
                BookSort.TITLE -> filtered.sortedBy { it.book.title.lowercase() }
                BookSort.AUTHOR -> filtered.sortedBy { (it.book.author ?: "").lowercase() }
                BookSort.PROGRESS -> filtered.sortedByDescending { it.progress }
            }
        }
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val bookStore: BookStoring,
    private val progressStore: ReadingProgressStoring,
    private val importService: BookImportService,
    private val parserFactory: BookParserFactory,
    private val ingestionService: IngestionService,
    private val libraryRepository: LibraryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            libraryRepository.observeShelves().collect { shelves ->
                _uiState.update { state ->
                    // Drop the filter if its shelf was deleted.
                    val sel = state.selectedShelfId?.takeIf { id -> shelves.any { it.id == id } }
                    state.copy(shelves = shelves, selectedShelfId = sel)
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val books = bookStore.listBooks().map { book ->
                val fraction = progressStore.loadPosition(book.id)?.progressFraction ?: 0.0
                ShelfBook(book, fraction.toFloat())
            }
            _uiState.update { it.copy(books = books) }
        }
    }

    fun onQueryChange(query: String) = _uiState.update { it.copy(query = query) }
    fun setViewMode(mode: LibraryViewMode) = _uiState.update { it.copy(viewMode = mode) }
    fun setSort(sort: BookSort) = _uiState.update { it.copy(sort = sort) }
    fun selectShelf(shelfId: String?) = _uiState.update { it.copy(selectedShelfId = shelfId) }

    fun createShelf(name: String, colorRgb: Long) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            libraryRepository.saveShelf(
                Shelf(
                    id = UUID.randomUUID().toString(),
                    name = trimmed,
                    colorRgb = colorRgb,
                    sortOrder = _uiState.value.shelves.size
                )
            )
        }
    }

    fun deleteShelf(shelfId: String) {
        viewModelScope.launch {
            libraryRepository.deleteShelf(shelfId)
            refresh()
        }
    }

    fun moveBookToShelf(bookId: BookID, shelfId: String?) {
        viewModelScope.launch {
            libraryRepository.setBookShelf(bookId, shelfId)
            refresh()
        }
    }

    fun deleteBook(bookId: BookID) {
        viewModelScope.launch {
            libraryRepository.deleteBook(bookId)
            refresh()
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
