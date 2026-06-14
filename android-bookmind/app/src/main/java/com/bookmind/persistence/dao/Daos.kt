package com.bookmind.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bookmind.persistence.entity.BookEntity
import com.bookmind.persistence.entity.ChapterEntity
import com.bookmind.persistence.entity.ChatMessageEntity
import com.bookmind.persistence.entity.CharacterEntity
import com.bookmind.persistence.entity.ChunkEntity
import com.bookmind.persistence.entity.EventEntity
import com.bookmind.persistence.entity.FactEntity
import com.bookmind.persistence.entity.ReadingProgressEntity
import com.bookmind.persistence.entity.ReadingSessionEntity
import com.bookmind.persistence.entity.RecapEntity
import com.bookmind.persistence.entity.ShelfEntity
import com.bookmind.persistence.entity.UserQuoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY addedAt DESC")
    suspend fun listBooks(): List<BookEntity>

    @Query("SELECT * FROM books WHERE id = :bookId")
    suspend fun book(bookId: String): BookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(book: BookEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<ChapterEntity>)

    @Query("SELECT * FROM chapters WHERE bookId = :bookId ORDER BY idx ASC")
    suspend fun chapters(bookId: String): List<ChapterEntity>

    @Query("UPDATE books SET shelfId = :shelfId WHERE id = :bookId")
    suspend fun setShelf(bookId: String, shelfId: String?)

    @Query("UPDATE books SET coverUri = :coverUri WHERE id = :bookId")
    suspend fun setCover(bookId: String, coverUri: String?)

    @Query("UPDATE books SET title = :title WHERE id = :bookId")
    suspend fun setTitle(bookId: String, title: String)

    @Query("UPDATE books SET shelfId = NULL WHERE shelfId = :shelfId")
    suspend fun clearShelf(shelfId: String)

    @Query("DELETE FROM books WHERE id = :bookId")
    suspend fun deleteBook(bookId: String)

    @Query("DELETE FROM chapters WHERE bookId = :bookId")
    suspend fun deleteChapters(bookId: String)
}

@Dao
interface ShelfDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(shelf: ShelfEntity)

    @Query("SELECT * FROM shelves ORDER BY sortOrder ASC, createdAt ASC")
    fun observeAll(): Flow<List<ShelfEntity>>

    @Query("DELETE FROM shelves WHERE id = :shelfId")
    suspend fun delete(shelfId: String)
}

@Dao
interface ReadingSessionDao {
    @Insert
    suspend fun insert(session: ReadingSessionEntity)

    @Query("SELECT * FROM reading_sessions WHERE startedAt >= :since ORDER BY startedAt ASC")
    suspend fun since(since: Long): List<ReadingSessionEntity>

    @Query("SELECT * FROM reading_sessions ORDER BY startedAt ASC")
    suspend fun all(): List<ReadingSessionEntity>

    @Query("SELECT * FROM reading_sessions WHERE bookId = :bookId ORDER BY startedAt ASC")
    suspend fun forBook(bookId: String): List<ReadingSessionEntity>

    @Query("DELETE FROM reading_sessions WHERE bookId = :bookId")
    suspend fun deleteForBook(bookId: String)
}

@Dao
interface ChatMessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ChatMessageEntity)

    @Query("SELECT * FROM chat_messages WHERE bookId = :bookId ORDER BY timestamp ASC")
    fun observeForBook(bookId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE bookId = :bookId ORDER BY timestamp ASC")
    suspend fun forBook(bookId: String): List<ChatMessageEntity>

    @Query("SELECT DISTINCT bookId FROM chat_messages")
    suspend fun bookIdsWithChats(): List<String>

    @Query("DELETE FROM chat_messages WHERE bookId = :bookId")
    suspend fun deleteForBook(bookId: String)
}

@Dao
interface ProgressDao {
    @Query("SELECT * FROM reading_progress WHERE bookId = :bookId")
    suspend fun load(bookId: String): ReadingProgressEntity?

    @Query("SELECT * FROM reading_progress")
    suspend fun all(): List<ReadingProgressEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(progress: ReadingProgressEntity)

    @Query("DELETE FROM reading_progress WHERE bookId = :bookId")
    suspend fun deleteForBook(bookId: String)
}

@Dao
interface ChunkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(chunks: List<ChunkEntity>)

    /** FTS search bounded by the spoiler horizon. = iOS `SQLiteChunkSearch`. */
    @Query(
        """
        SELECT chunks.* FROM chunks
        JOIN chunks_fts ON chunks.rowid = chunks_fts.rowid
        WHERE chunks.bookId = :bookId
          AND chunks.chapterIndex <= :maxChapterIndex
          AND chunks_fts MATCH :query
        ORDER BY chunks.chapterIndex DESC
        LIMIT 5
        """
    )
    suspend fun searchSafe(query: String, bookId: String, maxChapterIndex: Int): List<ChunkEntity>

    @Query("DELETE FROM chunks WHERE bookId = :bookId")
    suspend fun deleteForBook(bookId: String)
}

@Dao
interface FactDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(facts: List<FactEntity>)

    /** = iOS `SQLiteFactSearch`. */
    @Query(
        """
        SELECT facts.* FROM facts
        JOIN facts_fts ON facts.rowid = facts_fts.rowid
        WHERE facts.bookId = :bookId
          AND facts.chapterIndex <= :maxChapterIndex
          AND facts_fts MATCH :query
        ORDER BY facts.chapterIndex DESC
        LIMIT 10
        """
    )
    suspend fun searchSafe(query: String, bookId: String, maxChapterIndex: Int): List<FactEntity>

    @Query("DELETE FROM facts WHERE bookId = :bookId")
    suspend fun deleteForBook(bookId: String)
}

@Dao
interface CharacterDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(characters: List<CharacterEntity>)

    @Query("SELECT * FROM characters WHERE bookId = :bookId")
    suspend fun loadForBook(bookId: String): List<CharacterEntity>

    @Query("DELETE FROM characters WHERE bookId = :bookId")
    suspend fun deleteForBook(bookId: String)

    @Query(
        """
        SELECT * FROM characters
        WHERE bookId = :bookId
          AND LOWER(canonicalName) = :loweredName
          AND firstChapterIndex <= :maxChapterIndex
        """
    )
    suspend fun findExact(
        bookId: String,
        loweredName: String,
        maxChapterIndex: Int
    ): List<CharacterEntity>

    @Query(
        """
        SELECT * FROM characters
        WHERE bookId = :bookId
          AND firstChapterIndex <= :maxChapterIndex
          AND (LOWER(canonicalName) LIKE :like OR LOWER(aliasesJson) LIKE :like)
        ORDER BY firstChapterIndex DESC
        LIMIT 5
        """
    )
    suspend fun findPartial(
        bookId: String,
        like: String,
        maxChapterIndex: Int
    ): List<CharacterEntity>
}

@Dao
interface RecapDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recap: RecapEntity)

    @Query(
        """
        SELECT * FROM recaps
        WHERE bookId = :bookId AND chapterIndex <= :maxChapterIndex
        ORDER BY chapterIndex DESC
        LIMIT 1
        """
    )
    suspend fun latestRecap(bookId: String, maxChapterIndex: Int): RecapEntity?

    @Query("DELETE FROM recaps WHERE bookId = :bookId")
    suspend fun deleteForBook(bookId: String)
}

@Dao
interface UserQuoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(quote: UserQuoteEntity)

    @Query("SELECT * FROM user_quotes WHERE bookId = :bookId ORDER BY createdAt DESC")
    fun observeForBook(bookId: String): Flow<List<UserQuoteEntity>>

    @Query("SELECT * FROM user_quotes ORDER BY createdAt DESC")
    suspend fun all(): List<UserQuoteEntity>

    @Query("DELETE FROM user_quotes WHERE id = :quoteId")
    suspend fun delete(quoteId: String)

    @Query("DELETE FROM user_quotes WHERE bookId = :bookId")
    suspend fun deleteForBook(bookId: String)
}

@Dao
interface EventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<EventEntity>)

    @Query(
        """
        SELECT * FROM events
        WHERE bookId = :bookId AND chapterIndex <= :maxChapterIndex
        ORDER BY chapterIndex DESC
        LIMIT :limit
        """
    )
    suspend fun recentEvents(bookId: String, maxChapterIndex: Int, limit: Int): List<EventEntity>

    @Query("DELETE FROM events WHERE bookId = :bookId")
    suspend fun deleteForBook(bookId: String)
}
