package com.bookmind.memory

import com.bookmind.core.model.Book
import com.bookmind.core.model.Chapter
import com.bookmind.core.model.Character
import com.bookmind.core.model.Chunk
import com.bookmind.core.model.Fact
import com.bookmind.core.model.Recap

// = iOS `BookMemory/Protocols.swift`.

interface ChapterChunking {
    fun makeChunks(chapter: Chapter, rawText: String): List<Chunk>
}

interface SafeRecapBuilding {
    fun buildRecap(chapter: Chapter, chunks: List<Chunk>): Recap
}

interface FactIndexing {
    fun extractFacts(chapter: Chapter, rawText: String): List<Fact>
}

interface CharacterDetecting {
    fun detectCharacters(chapter: Chapter, rawText: String, existing: List<Character>): List<Character>
}

interface MemoryWriting {
    suspend fun writeChunks(chunks: List<Chunk>, chapterIndex: Int)
    suspend fun writeRecap(recap: Recap)
    suspend fun writeFacts(facts: List<Fact>)
    suspend fun upsertCharacters(characters: List<Character>)
    suspend fun loadCharacters(bookID: com.bookmind.core.model.BookID): List<Character>
}

interface BookIngesting {
    fun ingest(book: Book, chapters: List<Chapter>): kotlinx.coroutines.flow.Flow<IngestionProgress>
}

/** UI progress for ingestion. = inferred from android.md `IngestionProgress`. */
data class IngestionProgress(
    val chapterIndex: Int,
    val totalChapters: Int,
    val chapterTitle: String,
    val isComplete: Boolean = false
) {
    companion object {
        fun complete(total: Int) = IngestionProgress(total, total, "", isComplete = true)
    }
}
