package com.bookmind.core

import com.bookmind.core.model.Book
import com.bookmind.core.model.Chapter
import com.bookmind.core.model.ReadingPosition
import com.bookmind.core.parser.BookParserFactory
import com.bookmind.persistence.BookStoring
import com.bookmind.persistence.ReadingProgressStoring
import javax.inject.Inject

/**
 * = iOS `ReaderSessionController`. Owns the currently-open book, its chapters
 * and reading position. Stateless across instances; ViewModels hold the state.
 */
class ReaderSession @Inject constructor(
    private val bookStore: BookStoring,
    private val progressStore: ReadingProgressStoring,
    private val parserFactory: BookParserFactory
) {
    data class State(
        val book: Book,
        val chapters: List<Chapter>,
        val currentChapter: Chapter?,
        val position: ReadingPosition
    )

    /** Loads chapters (parsing+persisting on first open) and restores position. */
    suspend fun open(book: Book): State {
        var loaded = bookStore.chapters(book.id)
        if (loaded.isEmpty()) {
            loaded = parserFactory.parser(book.format).parseChapters(book)
            bookStore.saveChapters(loaded, book.id)
        }
        val chapters = loaded.sortedBy { it.index }

        val saved = progressStore.loadPosition(book.id)
        return if (saved != null) {
            val current = chapters.firstOrNull { it.id == saved.chapterID } ?: chapters.firstOrNull()
            State(book, chapters, current, saved)
        } else {
            val current = chapters.firstOrNull()
            State(
                book = book,
                chapters = chapters,
                currentChapter = current,
                position = ReadingPosition(book.id, current?.id, 0.0)
            )
        }
    }

    suspend fun saveProgress(book: Book, chapter: Chapter, chapters: List<Chapter>) {
        val fraction = if (chapters.size <= 1) 0.0
        else chapter.index.toDouble() / (chapters.size - 1).toDouble()
        progressStore.savePosition(ReadingPosition(book.id, chapter.id, fraction))
    }
}
