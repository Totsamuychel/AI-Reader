package com.bookmind.memory

import com.bookmind.core.model.Chapter
import com.bookmind.core.model.Chunk
import com.bookmind.core.model.ChunkID
import javax.inject.Inject
import kotlin.math.max

/** = iOS `FixedSizeChunker`. Splits chapter text into ~fixed-size chunks at sentence boundaries. */
class FixedSizeChunker @Inject constructor() : ChapterChunking {

    private val targetTokenCount: Int = 350
    private val overlapTokenCount: Int = 40

    override fun makeChunks(chapter: Chapter, rawText: String): List<Chunk> {
        val sentences = Sentences.split(rawText)
        if (sentences.isEmpty()) return emptyList()

        val chunks = mutableListOf<Chunk>()
        var current = mutableListOf<String>()
        var currentTokens = 0
        var chunkIdx = 0

        for (sentence in sentences) {
            val tokens = approxTokenCount(sentence)
            if (currentTokens + tokens > targetTokenCount && current.isNotEmpty()) {
                chunks.add(makeChunk(chapter, chunkIdx, current, currentTokens))
                chunkIdx += 1
                // Overlap: keep trailing sentences worth ~overlapTokenCount.
                val overlap = mutableListOf<String>()
                var overlapTokens = 0
                for (s in current.asReversed()) {
                    overlap.add(0, s)
                    overlapTokens += approxTokenCount(s)
                    if (overlapTokens >= overlapTokenCount) break
                }
                current = overlap
                currentTokens = overlapTokens
            }
            current.add(sentence)
            currentTokens += tokens
        }
        if (current.isNotEmpty()) {
            chunks.add(makeChunk(chapter, chunkIdx, current, currentTokens))
        }
        return chunks
    }

    private fun makeChunk(chapter: Chapter, idx: Int, sentences: List<String>, tokens: Int) = Chunk(
        id = ChunkID("${chapter.id.raw}#chunk-$idx"),
        bookID = chapter.bookID,
        chapterID = chapter.id,
        index = idx,
        text = sentences.joinToString(" ").trim(),
        tokenCount = tokens,
        spoilerLevel = 0
    )

    /** Rough heuristic: one token ≈ 4 chars. */
    private fun approxTokenCount(text: String): Int = max(1, text.length / 4)
}
