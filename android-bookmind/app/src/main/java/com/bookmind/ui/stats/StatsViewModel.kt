package com.bookmind.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookmind.persistence.BookStoring
import com.bookmind.persistence.ReadingProgressStoring
import com.bookmind.persistence.ReadingSessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class BookProgress(val title: String, val fraction: Float)

/** One day's reading total for the weekly bar chart. */
data class DayMinutes(val label: String, val minutes: Int, val isToday: Boolean)

data class StatsUiState(
    val totalBooks: Int = 0,
    val inProgress: Int = 0,
    val finished: Int = 0,
    val averageProgress: Float = 0f,
    val perBook: List<BookProgress> = emptyList(),
    val weekMinutes: List<DayMinutes> = emptyList(),
    val streakDays: Int = 0,
    val wordsPerMinute: Int = 0,
    val totalMinutesThisWeek: Int = 0
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val bookStore: BookStoring,
    private val progressStore: ReadingProgressStoring,
    private val sessionStore: ReadingSessionStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            val books = bookStore.listBooks()
            val perBook = books.map { book ->
                val frac = (progressStore.loadPosition(book.id)?.progressFraction ?: 0.0).toFloat()
                BookProgress(book.title, frac)
            }
            val finished = perBook.count { it.fraction >= 0.99f }
            val inProgress = perBook.count { it.fraction in 0.01f..0.98f }
            val avg = if (perBook.isEmpty()) 0f else perBook.map { it.fraction }.average().toFloat()

            val sessions = sessionStore.all()
            val week = weeklyMinutes(sessions.map { it.startedAt to it.durationMs })
            val totalWeekMinutes = week.sumOf { it.minutes }
            val streak = streakDays(sessions.map { it.startedAt })
            val totalMs = sessions.sumOf { it.durationMs }
            val totalWords = sessions.sumOf { it.words }
            val wpm = if (totalMs > 0) (totalWords / (totalMs / 60000.0)).toInt() else 0

            _uiState.update {
                it.copy(
                    totalBooks = books.size,
                    inProgress = inProgress,
                    finished = finished,
                    averageProgress = avg,
                    perBook = perBook.sortedByDescending { p -> p.fraction },
                    weekMinutes = week,
                    streakDays = streak,
                    wordsPerMinute = wpm,
                    totalMinutesThisWeek = totalWeekMinutes
                )
            }
        }
    }

    /** Buckets session durations into the last 7 calendar days (oldest → today). */
    private fun weeklyMinutes(sessions: List<Pair<Long, Long>>): List<DayMinutes> {
        val labels = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val cal = Calendar.getInstance()
        val todayStart = startOfDay(cal.timeInMillis)
        return (6 downTo 0).map { daysAgo ->
            val dayStart = todayStart - daysAgo * DAY_MS
            val dayEnd = dayStart + DAY_MS
            val ms = sessions.filter { it.first in dayStart until dayEnd }.sumOf { it.second }
            val dow = Calendar.getInstance().apply { timeInMillis = dayStart }
                .get(Calendar.DAY_OF_WEEK) - 1
            DayMinutes(labels[dow], (ms / 60000).toInt(), isToday = daysAgo == 0)
        }
    }

    /** Consecutive days ending today (or yesterday) that have at least one session. */
    private fun streakDays(starts: List<Long>): Int {
        if (starts.isEmpty()) return 0
        val days = starts.map { startOfDay(it) }.toHashSet()
        val todayStart = startOfDay(System.currentTimeMillis())
        var streak = 0
        var cursor = todayStart
        // Allow the streak to count even if today hasn't been read yet but yesterday was.
        if (todayStart !in days && (todayStart - DAY_MS) in days) cursor = todayStart - DAY_MS
        while (cursor in days) {
            streak++
            cursor -= DAY_MS
        }
        return streak
    }

    private fun startOfDay(time: Long): Long = Calendar.getInstance().apply {
        timeInMillis = time
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    companion object { private const val DAY_MS = 24L * 60 * 60 * 1000 }
}
