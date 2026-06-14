package com.bookmind.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookmind.core.model.BookID
import com.bookmind.persistence.BookStoring
import com.bookmind.persistence.ReadingProgressStoring
import com.bookmind.persistence.ReadingSessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BookStatsUiState(
    val title: String = "",
    val isLoading: Boolean = true,
    val progressFraction: Float = 0f,
    val sessions: Int = 0,
    val totalMinutes: Int = 0,
    val wordsRead: Int = 0,
    val wordsPerMinute: Int = 0,
    val lastReadAt: Long? = null
)

/**
 * Per-book reading statistics, aggregated from the recorded [ReadingSessionStore]
 * sessions plus the saved [ReadingProgressStoring] position. Call [load] with the
 * book id (the reader passes it via navigation).
 */
@HiltViewModel
class BookStatsViewModel @Inject constructor(
    private val bookStore: BookStoring,
    private val progressStore: ReadingProgressStoring,
    private val sessionStore: ReadingSessionStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookStatsUiState())
    val uiState: StateFlow<BookStatsUiState> = _uiState.asStateFlow()

    fun load(bookId: String) {
        val id = BookID(bookId)
        viewModelScope.launch {
            val book = bookStore.book(id)
            val fraction = (progressStore.loadPosition(id)?.progressFraction ?: 0.0).toFloat()
            val sessions = sessionStore.forBook(id)
            val totalMs = sessions.sumOf { it.durationMs }
            val words = sessions.sumOf { it.words }
            val wpm = if (totalMs > 0) (words / (totalMs / 60000.0)).toInt() else 0

            _uiState.update {
                it.copy(
                    title = book?.title ?: "Книга",
                    isLoading = false,
                    progressFraction = fraction.coerceIn(0f, 1f),
                    sessions = sessions.size,
                    totalMinutes = (totalMs / 60000).toInt(),
                    wordsRead = words,
                    wordsPerMinute = wpm,
                    lastReadAt = sessions.maxOfOrNull { s -> s.startedAt }
                )
            }
        }
    }
}
