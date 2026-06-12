package com.bookmind.persistence

import com.bookmind.core.model.Book
import com.bookmind.core.model.BookID
import com.bookmind.core.model.Chapter
import com.bookmind.core.model.ReadingPosition
import com.bookmind.persistence.dao.BookDao
import com.bookmind.persistence.dao.ProgressDao
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

class RoomProgressStore @Inject constructor(
    private val progressDao: ProgressDao
) : ReadingProgressStoring {
    override suspend fun loadPosition(bookID: BookID): ReadingPosition? =
        progressDao.load(bookID.raw)?.toDomain()
    override suspend fun savePosition(position: ReadingPosition) =
        progressDao.save(position.toEntity())
}
