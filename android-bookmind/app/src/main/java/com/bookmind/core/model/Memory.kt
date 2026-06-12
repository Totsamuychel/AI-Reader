package com.bookmind.core.model

/** = iOS `Chunk`. */
data class Chunk(
    val id: ChunkID,
    val bookID: BookID,
    val chapterID: ChapterID,
    val index: Int,
    val text: String,
    val tokenCount: Int,
    val spoilerLevel: Int = 0
)

/** = iOS `Character`. */
data class Character(
    val id: String,
    val bookID: BookID,
    val canonicalName: String,
    val aliases: List<String> = emptyList(),
    val firstChapterIndex: Int,
    val lastSafeChapterIndex: Int,
    val safeSummary: String? = null
)

/** = iOS `Event`. */
data class Event(
    val id: String,
    val bookID: BookID,
    val chapterID: ChapterID,
    val chapterIndex: Int,
    val eventType: String,
    val shortDescription: String,
    val longDescriptionSafe: String? = null,
    val spoilerLevel: Int = 0
)

/** = iOS `Recap`. */
data class Recap(
    val id: String,
    val bookID: BookID,
    val chapterID: ChapterID,
    val chapterIndex: Int,
    val recapText: String,
    val style: String = "safe"
)

/** = iOS `Fact`. */
data class Fact(
    val id: String,
    val bookID: BookID,
    val chapterID: ChapterID,
    val chapterIndex: Int,
    val factType: String,
    val subjectID: String? = null,
    val objectID: String? = null,
    val text: String,
    val spoilerLevel: Int = 0
)

/** = iOS `Relation`. */
data class Relation(
    val id: String,
    val bookID: BookID,
    val sourceCharacterID: String,
    val targetCharacterID: String,
    val relationType: String,
    val confidence: Double,
    val safeUntilChapterIndex: Int
)
