package com.bookmind

import com.bookmind.core.model.BookID
import com.bookmind.core.model.Chapter
import com.bookmind.core.model.ChapterID
import com.bookmind.memory.FixedSizeChunker
import com.bookmind.memory.HeuristicCharacterDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** = iOS `ChunkerAndRecapTests` (subset). */
class ChunkerAndDetectorTest {

    private fun chapter() = Chapter(
        id = ChapterID("book-1#0"),
        bookID = BookID("book-1"),
        index = 0,
        title = "Ch 1"
    )

    @Test
    fun chunker_splits_long_text_into_multiple_chunks() {
        val sentence = "The quiet river carried the small wooden boat past the old stone bridge. "
        val text = sentence.repeat(40)
        val chunks = FixedSizeChunker().makeChunks(chapter(), text)
        assertTrue("expected more than one chunk", chunks.size > 1)
        assertTrue(chunks.all { it.tokenCount > 0 })
    }

    @Test
    fun chunker_returns_empty_for_blank() {
        assertEquals(0, FixedSizeChunker().makeChunks(chapter(), "   ").size)
    }

    @Test
    fun detector_finds_repeated_capitalized_names() {
        val text = "Alice met Bob. Alice smiled at Bob, and Bob nodded to Alice."
        val names = HeuristicCharacterDetector().extractCandidateNames(text)
        assertTrue(names.contains("Alice"))
        assertTrue(names.contains("Bob"))
    }
}
