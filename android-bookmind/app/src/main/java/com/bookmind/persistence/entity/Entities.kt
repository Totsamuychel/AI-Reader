package com.bookmind.persistence.entity

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey

// = iOS Persistence `Records` + `Migrator` schema.

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey val id: String,
    val title: String,
    val author: String?,
    val format: String,
    val fileUri: String,
    val addedAt: Long
)

@Entity(
    tableName = "chapters",
    indices = [Index(value = ["bookId", "idx"], unique = true)]
)
data class ChapterEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val idx: Int,
    val title: String?,
    val contentRef: String?
)

@Entity(tableName = "reading_progress")
data class ReadingProgressEntity(
    @PrimaryKey val bookId: String,
    val chapterId: String?,
    val progressFraction: Double,
    val updatedAt: Long
)

@Entity(
    tableName = "chunks",
    indices = [Index(value = ["bookId", "chapterIndex"])]
)
data class ChunkEntity(
    @PrimaryKey(autoGenerate = true) val rowid: Long = 0,
    val chunkId: String,
    val bookId: String,
    val chapterId: String,
    val idx: Int,
    val text: String,
    val tokenCount: Int,
    val spoilerLevel: Int = 0,
    val chapterIndex: Int = 0
)

/** FTS shadow table for [ChunkEntity]. = SQLite FTS5 `chunks_fts` on iOS. */
@Fts4(contentEntity = ChunkEntity::class)
@Entity(tableName = "chunks_fts")
data class ChunkFts(val text: String)

@Entity(
    tableName = "characters",
    indices = [Index(value = ["bookId", "canonicalName"])]
)
data class CharacterEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val canonicalName: String,
    val aliasesJson: String,
    val firstChapterIndex: Int,
    val lastSafeChapterIndex: Int,
    val safeSummary: String?
)

@Entity(
    tableName = "events",
    indices = [Index(value = ["bookId", "chapterIndex"])]
)
data class EventEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val chapterId: String,
    val chapterIndex: Int,
    val eventType: String,
    val shortDescription: String,
    val longDescriptionSafe: String?,
    val spoilerLevel: Int = 0
)

@Entity(
    tableName = "recaps",
    indices = [Index(value = ["bookId", "chapterIndex"])]
)
data class RecapEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val chapterId: String,
    val chapterIndex: Int,
    val recapText: String,
    val style: String = "safe"
)

@Entity(
    tableName = "facts",
    indices = [Index(value = ["bookId", "chapterIndex"])]
)
data class FactEntity(
    @PrimaryKey(autoGenerate = true) val rowid: Long = 0,
    val factId: String,
    val bookId: String,
    val chapterId: String,
    val chapterIndex: Int,
    val factType: String,
    val subjectId: String?,
    val objectId: String?,
    val text: String,
    val spoilerLevel: Int = 0
)

/** FTS shadow table for [FactEntity]. = SQLite FTS5 `facts_fts` on iOS. */
@Fts4(contentEntity = FactEntity::class)
@Entity(tableName = "facts_fts")
data class FactFts(val text: String)

@Entity(tableName = "relations")
data class RelationEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val sourceCharacterId: String,
    val targetCharacterId: String,
    val relationType: String,
    val confidence: Double,
    val safeUntilChapterIndex: Int
)
