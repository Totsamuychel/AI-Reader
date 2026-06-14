package com.bookmind.persistence

import com.bookmind.core.model.Book
import com.bookmind.core.model.BookID
import com.bookmind.core.model.Chapter
import com.bookmind.core.model.ReadingPosition
import com.bookmind.core.model.Shelf
import com.bookmind.core.model.UserQuote
import com.bookmind.persistence.dao.BookDao
import com.bookmind.persistence.dao.CharacterDao
import com.bookmind.persistence.dao.ChunkDao
import com.bookmind.persistence.dao.EventDao
import com.bookmind.persistence.dao.FactDao
import com.bookmind.persistence.dao.ProgressDao
import com.bookmind.persistence.dao.ReadingSessionDao
import com.bookmind.persistence.dao.RecapDao
import com.bookmind.persistence.dao.ShelfDao
import com.bookmind.persistence.dao.UserQuoteDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** = iOS `BookStoring`. */
interface BookStoring {
    suspend fun listBooks(): List<Book>
    suspend fun save(book: Book)
    suspend fun saveChapters(chapters: List<Chapter>, bookID: BookID)
    suspend fun chapters(bookID: BookID): List<Chapter>
    suspend fun book(bookID: BookID): Book?
}

/** = iOS `ReadingProgressStoring`. */
interface ReadingProgressStoring {
    suspend fun loadPosition(bookID: BookID): ReadingPosition?
    suspend fun savePosition(position: ReadingPosition)
    suspend fun allPositions(): List<ReadingPosition>
}

class RoomBookStore @Inject constructor(
    private val bookDao: BookDao
) : BookStoring {
    override suspend fun listBooks(): List<Book> = bookDao.listBooks().map { it.toDomain() }
    override suspend fun save(book: Book) = bookDao.upsert(book.toEntity())
    override suspend fun saveChapters(chapters: List<Chapter>, bookID: BookID) =
        bookDao.insertChapters(chapters.map { it.toEntity() })
    override suspend fun chapters(bookID: BookID): List<Chapter> =
        bookDao.chapters(bookID.raw).map { it.toDomain() }
    override suspend fun book(bookID: BookID): Book? = bookDao.book(bookID.raw)?.toDomain()
}

/** Saved reader highlights. */
interface QuoteStoring {
    suspend fun save(quote: UserQuote)
    fun observeQuotes(bookID: BookID): Flow<List<UserQuote>>
    suspend fun delete(quoteId: String)
    suspend fun allQuotes(): List<UserQuote>
}

class RoomQuoteStore @Inject constructor(
    private val quoteDao: UserQuoteDao
) : QuoteStoring {
    override suspend fun save(quote: UserQuote) = quoteDao.insert(quote.toEntity())
    override fun observeQuotes(bookID: BookID): Flow<List<UserQuote>> =
        quoteDao.observeForBook(bookID.raw).map { list -> list.map { it.toDomain() } }
    override suspend fun delete(quoteId: String) = quoteDao.delete(quoteId)
    override suspend fun allQuotes(): List<UserQuote> = quoteDao.all().map { it.toDomain() }
}

class RoomProgressStore @Inject constructor(
    private val progressDao: ProgressDao
) : ReadingProgressStoring {
    override suspend fun loadPosition(bookID: BookID): ReadingPosition? =
        progressDao.load(bookID.raw)?.toDomain()
    override suspend fun savePosition(position: ReadingPosition) =
        progressDao.save(position.toEntity())
    override suspend fun allPositions(): List<ReadingPosition> =
        progressDao.all().map { it.toDomain() }
}

/**
 * Named shelves + book lifecycle that spans multiple tables (moving a book to a
 * shelf, deleting a book and all its derived memory/progress/quotes).
 */
class LibraryRepository @Inject constructor(
    private val shelfDao: ShelfDao,
    private val bookDao: BookDao,
    private val progressDao: ProgressDao,
    private val quoteDao: UserQuoteDao,
    private val chunkDao: ChunkDao,
    private val factDao: FactDao,
    private val characterDao: CharacterDao,
    private val recapDao: RecapDao,
    private val eventDao: EventDao,
    private val sessionDao: ReadingSessionDao
) {
    fun observeShelves(): Flow<List<Shelf>> =
        shelfDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun saveShelf(shelf: Shelf) = shelfDao.upsert(shelf.toEntity())

    suspend fun deleteShelf(shelfId: String) {
        bookDao.clearShelf(shelfId) // unfile its books rather than deleting them
        shelfDao.delete(shelfId)
    }

    suspend fun setBookShelf(bookID: BookID, shelfId: String?) =
        bookDao.setShelf(bookID.raw, shelfId)

    /** Deletes a book and every derived row tied to it. */
    suspend fun deleteBook(bookID: BookID) {
        val id = bookID.raw
        bookDao.deleteChapters(id)
        progressDao.deleteForBook(id)
        quoteDao.deleteForBook(id)
        chunkDao.deleteForBook(id)
        factDao.deleteForBook(id)
        characterDao.deleteForBook(id)
        recapDao.deleteForBook(id)
        eventDao.deleteForBook(id)
        sessionDao.deleteForBook(id)
        bookDao.deleteBook(id)
    }
}

/** Records how long a reader session lasted, for the stats screen. */
class ReadingSessionStore @Inject constructor(
    private val sessionDao: ReadingSessionDao
) {
    suspend fun record(bookID: BookID, startedAt: Long, durationMs: Long, words: Int) {
        if (durationMs < MIN_SESSION_MS) return // ignore accidental opens
        sessionDao.insert(
            com.bookmind.persistence.entity.ReadingSessionEntity(
                bookId = bookID.raw,
                startedAt = startedAt,
                durationMs = durationMs,
                words = words
            )
        )
    }

    suspend fun since(since: Long) = sessionDao.since(since)
    suspend fun all() = sessionDao.all()

    companion object { private const val MIN_SESSION_MS = 3_000L }
}
