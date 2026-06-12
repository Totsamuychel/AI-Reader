package com.bookmind.memory

import com.bookmind.core.model.Book
import com.bookmind.core.model.Chapter
import com.bookmind.core.parser.BookParserFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * = iOS `BookIngestionService`.
 * Orchestrates chunking, recap, fact extraction and character detection per chapter,
 * persisting everything to memory and emitting [IngestionProgress] for the UI.
 */
class IngestionService @Inject constructor(
    private val parserFactory: BookParserFactory,
    private val chunker: FixedSizeChunker,
    private val recapBuilder: HeuristicRecapBuilder,
    private val factIndexer: HeuristicFactIndexer,
    private val characterDetector: HeuristicCharacterDetector,
    private val memory: RoomMemoryWriter
) : BookIngesting {

    override fun ingest(book: Book, chapters: List<Chapter>): Flow<IngestionProgress> = flow {
        val textProvider = parserFactory.parser(book.format)
        var knownCharacters = memory.loadCharacters(book.id)
        val ordered = chapters.sortedBy { it.index }

        for (chapter in ordered) {
            emit(IngestionProgress(chapter.index, ordered.size, chapter.title ?: "Chapter ${chapter.index + 1}"))

            val rawText = textProvider.rawText(chapter, book)
            if (rawText.isEmpty()) continue

            // 1. Chunk the chapter.
            val chunks = chunker.makeChunks(chapter, rawText)
            memory.writeChunks(chunks, chapter.index)

            // 2. Safe recap (no spoilers).
            val recap = recapBuilder.buildRecap(chapter, chunks)
            memory.writeRecap(recap)

            // 3. Facts.
            val facts = factIndexer.extractFacts(chapter, rawText)
            memory.writeFacts(facts)

            // 4. Characters (cumulative across chapters).
            val detected = characterDetector.detectCharacters(chapter, rawText, knownCharacters)
            memory.upsertCharacters(detected)
            knownCharacters = memory.loadCharacters(book.id)
        }

        emit(IngestionProgress.complete(ordered.size))
    }
}
