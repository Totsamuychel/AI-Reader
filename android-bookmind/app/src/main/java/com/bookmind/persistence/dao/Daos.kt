package com.bookmind.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bookmind.persistence.entity.BookEntity
import com.bookmind.persistence.entity.ChapterEntity
import com.bookmind.persistence.entity.CharacterEntity
import com.bookmind.persistence.entity.ChunkEntity
import com.bookmind.persistence.entity.EventEntity
import com.bookmind.persistence.entity.FactEntity
import com.bookmind.persistence.entity.ReadingProgressEntity
import com.bookmind.persistence.entity.RecapEntity

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
}

@Dao
interface ProgressDao {
    @Query("SELECT * FROM reading_progress WHERE bookId = :bookId")
    suspend fun load(bookId: String): ReadingProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(progress: ReadingProgressEntity)
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
}

@Dao
interface CharacterDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(characters: List<CharacterEntity>)

    @Query("SELECT * FROM characters WHERE bookId = :bookId")
    suspend fun loadForBook(bookId: String): List<CharacterEntity>

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
}
