package com.bookmind

import com.bookmind.retrieval.ftsQuery
import com.bookmind.safety.AnswerMode
import com.bookmind.safety.HeuristicSpoilerScanner
import com.bookmind.safety.SpoilerBoundaryResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** = iOS `SpoilerBoundaryTests` + `FTSQueryTests`. */
class SafetyAndFtsTest {

    private val resolver = SpoilerBoundaryResolver()
    private val scanner = HeuristicSpoilerScanner()

    @Test
    fun safe_mode_bounds_to_current_chapter() {
        assertEquals(3, resolver.allowedChapterIndex(AnswerMode.SAFE, 3))
    }

    @Test
    fun full_mode_allows_everything() {
        assertEquals(Int.MAX_VALUE, resolver.allowedChapterIndex(AnswerMode.FULL, 3))
    }

    @Test
    fun scanner_flags_spoiler_phrase() {
        assertTrue(scanner.containsObviousSpoiler("In the end he dies in battle.", 2, emptyList()))
    }

    @Test
    fun scanner_flags_future_chapter_reference() {
        assertTrue(scanner.containsObviousSpoiler("As shown in chapter 14, they win.", 2, emptyList()))
    }

    @Test
    fun scanner_passes_safe_text() {
        assertFalse(scanner.containsObviousSpoiler("Alice is curious and brave.", 2, emptyList()))
    }

    @Test
    fun fts_query_sanitizes_and_ors_tokens() {
        assertEquals("\"alice\" OR \"river\"", ftsQuery("alice, river!"))
    }

    @Test
    fun fts_query_drops_short_tokens_and_empty() {
        assertEquals("", ftsQuery("a ! ?"))
    }
}
