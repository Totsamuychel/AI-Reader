package com.bookmind.persistence

import com.bookmind.core.model.Book
import com.bookmind.core.model.BookFormat
import com.bookmind.core.model.BookID
import com.bookmind.core.model.Chapter
import com.bookmind.core.model.ChapterID
import com.bookmind.core.model.Character
import com.bookmind.core.model.Chunk
import com.bookmind.core.model.ChunkID
import com.bookmind.core.model.Event
import com.bookmind.core.model.Fact
import com.bookmind.core.model.ReadingPosition
import com.bookmind.core.model.Recap
import com.bookmind.persistence.entity.BookEntity
import com.bookmind.persistence.entity.ChapterEntity
import com.bookmind.persistence.entity.CharacterEntity
import com.bookmind.persistence.entity.ChunkEntity
import com.bookmind.persistence.entity.EventEntity
import com.bookmind.persistence.entity.FactEntity
import com.bookmind.persistence.entity.ReadingProgressEntity
import com.bookmind.persistence.entity.RecapEntity
import org.json.JSONArray

// = iOS `Records` toDomain()/init(domain:) conversions.

fun Book.toEntity() = BookEntity(id.raw, title, author, format.raw, fileUri, addedAt)
fun BookEntity.toDomain() = Book(
    id = BookID(id),
    title = title,
    author = author,
    format = BookFormat.fromRaw(format),
    fileUri = fileUri,
    addedAt = addedAt
)

fun Chapter.toEntity() = ChapterEntity(id.raw, bookID.raw, index, title, contentRef)
fun ChapterEntity.toDomain() = Chapter(ChapterID(id), BookID(bookId), idx, title, contentRef)

fun ReadingPosition.toEntity() =
    ReadingProgressEntity(bookID.raw, chapterID?.raw, progressFraction, updatedAt)
fun ReadingProgressEntity.toDomain() = ReadingPosition(
    bookID = BookID(bookId),
    chapterID = chapterId?.let { ChapterID(it) },
    progressFraction = progressFraction,
    updatedAt = updatedAt
)

fun Chunk.toEntity(chapterIndex: Int) = ChunkEntity(
    chunkId = id.raw,
    bookId = bookID.raw,
    chapterId = chapterID.raw,
    idx = index,
    text = text,
    tokenCount = tokenCount,
    spoilerLevel = spoilerLevel,
    chapterIndex = chapterIndex
)
fun ChunkEntity.toDomain() = Chunk(
    id = ChunkID(chunkId),
    bookID = BookID(bookId),
    chapterID = ChapterID(chapterId),
    index = idx,
    text = text,
    tokenCount = tokenCount,
    spoilerLevel = spoilerLevel
)

fun Character.toEntity() = CharacterEntity(
    id = id,
    bookId = bookID.raw,
    canonicalName = canonicalName,
    aliasesJson = JSONArray(aliases).toString(),
    firstChapterIndex = firstChapterIndex,
    lastSafeChapterIndex = lastSafeChapterIndex,
    safeSummary = safeSummary
)
fun CharacterEntity.toDomain(): Character {
    val arr = runCatching { JSONArray(aliasesJson) }.getOrNull()
    val aliases = buildList {
        if (arr != null) for (i in 0 until arr.length()) add(arr.optString(i))
    }
    return Character(
        id = id,
        bookID = BookID(bookId),
        canonicalName = canonicalName,
        aliases = aliases,
        firstChapterIndex = firstChapterIndex,
        lastSafeChapterIndex = lastSafeChapterIndex,
        safeSummary = safeSummary
    )
}

fun Event.toEntity() = EventEntity(
    id, bookID.raw, chapterID.raw, chapterIndex, eventType,
    shortDescription, longDescriptionSafe, spoilerLevel
)
fun EventEntity.toDomain() = Event(
    id = id,
    bookID = BookID(bookId),
    chapterID = ChapterID(chapterId),
    chapterIndex = chapterIndex,
    eventType = eventType,
    shortDescription = shortDescription,
    longDescriptionSafe = longDescriptionSafe,
    spoilerLevel = spoilerLevel
)

fun Recap.toEntity() =
    RecapEntity(id, bookID.raw, chapterID.raw, chapterIndex, recapText, style)
fun RecapEntity.toDomain() = Recap(
    id = id,
    bookID = BookID(bookId),
    chapterID = ChapterID(chapterId),
    chapterIndex = chapterIndex,
    recapText = recapText,
    style = style
)

fun Fact.toEntity() = FactEntity(
    factId = id,
    bookId = bookID.raw,
    chapterId = chapterID.raw,
    chapterIndex = chapterIndex,
    factType = factType,
    subjectId = subjectID,
    objectId = objectID,
    text = text,
    spoilerLevel = spoilerLevel
)
fun FactEntity.toDomain() = Fact(
    id = factId,
    bookID = BookID(bookId),
    chapterID = ChapterID(chapterId),
    chapterIndex = chapterIndex,
    factType = factType,
    subjectID = subjectId,
    objectID = objectId,
    text = text,
    spoilerLevel = spoilerLevel
)
