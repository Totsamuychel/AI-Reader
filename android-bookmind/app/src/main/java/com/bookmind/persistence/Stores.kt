package com.bookmind.persistence

import com.bookmind.core.model.Book
import com.bookmind.core.model.BookID
import com.bookmind.core.model.Chapter
import com.bookmind.core.model.ReadingPosition
import com.bookmind.core.model.UserQuote
import com.bookmind.persistence.dao.BookDao
import com.bookmind.persistence.dao.ProgressDao
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
}

class RoomQuoteStore @Inject constructor(
    private val quoteDao: UserQuoteDao
) : QuoteStoring {
    override suspend fun save(quote: UserQuote) = quoteDao.insert(quote.toEntity())
    override fun observeQuotes(bookID: BookID): Flow<List<UserQuote>> =
        quoteDao.observeForBook(bookID.raw).map { list -> list.map { it.toDomain() } }
    override suspend fun delete(quoteId: String) = quoteDao.delete(quoteId)
}

class RoomProgressStore @Inject constructor(
    private val progressDao: ProgressDao
) : ReadingProgressStoring {
    override suspend fun loadPosition(bookID: BookID): ReadingPosition? =
        progressDao.load(bookID.raw)?.toDomain()
    override suspend fun savePosition(position: ReadingPosition) =
        progressDao.save(position.toEntity())
}
