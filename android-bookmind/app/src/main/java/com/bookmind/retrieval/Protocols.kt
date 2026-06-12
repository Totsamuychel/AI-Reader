package com.bookmind.retrieval

import com.bookmind.core.model.BookID
import com.bookmind.core.model.Character
import com.bookmind.core.model.Chunk
import com.bookmind.core.model.Event
import com.bookmind.core.model.Fact
import com.bookmind.core.model.Recap

/** = iOS `RetrievalContext`. */
data class RetrievalContext(
    val safeRecap: Recap? = null,
    val characterCards: List<Character> = emptyList(),
    val recentEvents: List<Event> = emptyList(),
    val quotes: List<Chunk> = emptyList(),
    val facts: List<Fact> = emptyList()
) {
    val isEmpty: Boolean
        get() = safeRecap == null && characterCards.isEmpty() &&
            recentEvents.isEmpty() && quotes.isEmpty() && facts.isEmpty()
}

interface ContextRetrieving {
    suspend fun context(
        question: String,
        bookID: BookID,
        currentChapterIndex: Int,
        spoilerBoundary: Int
    ): RetrievalContext
}

interface CharacterLookupService {
    suspend fun findCharacters(query: String, bookID: BookID, currentChapterIndex: Int): List<Character>
}

interface FactSearchService {
    suspend fun searchFacts(query: String, bookID: BookID, maxChapterIndex: Int): List<Fact>
}

interface ChunkSearchService {
    suspend fun searchChunks(query: String, bookID: BookID, maxChapterIndex: Int): List<Chunk>
}

interface RecapLookupService {
    suspend fun latestRecap(bookID: BookID, maxChapterIndex: Int): Recap?
}

interface EventLookupService {
    suspend fun recentEvents(bookID: BookID, maxChapterIndex: Int, limit: Int): List<Event>
}

interface PromptContextAssembling {
    fun makePromptContext(
        retrievalContext: RetrievalContext,
        question: String,
        currentChapterIndex: Int,
        spoilerBoundary: Int
    ): String
}

/**
 * Sanitizes free-text for FTS4 MATCH: keeps alphanumerics/`-`/`_`, splits on
 * whitespace, drops <2-char tokens, joins quoted tokens with `OR`.
 * = iOS `ftsQuery(from:)`.
 */
fun ftsQuery(query: String): String {
    val cleaned = buildString {
        for (ch in query) {
            if (ch.isLetterOrDigit() || ch.isWhitespace() || ch == '-' || ch == '_') append(ch)
        }
    }
    val tokens = cleaned.split(Regex("\\s+")).filter { it.length >= 2 }
    if (tokens.isEmpty()) return ""
    return tokens.joinToString(" OR ") { "\"$it\"" }
}
