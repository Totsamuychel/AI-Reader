package com.bookmind.retrieval

import com.bookmind.core.model.BookID
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.min

/** = iOS `RetrievalEngine`. Gathers safe context concurrently, then reranks/filters. */
class RetrievalEngine @Inject constructor(
    private val characters: CharacterLookupService,
    private val facts: FactSearchService,
    private val chunks: ChunkSearchService,
    private val recaps: RecapLookupService,
    private val events: EventLookupService
) : ContextRetrieving {

    override suspend fun context(
        question: String,
        bookID: BookID,
        currentChapterIndex: Int,
        spoilerBoundary: Int
    ): RetrievalContext = coroutineScope {
        val maxChapter = min(spoilerBoundary, currentChapterIndex)

        val recapTask = async { recaps.latestRecap(bookID, maxChapter) }
        val charactersTask = async { characters.findCharacters(question, bookID, maxChapter) }
        val factsTask = async { facts.searchFacts(question, bookID, maxChapter) }
        val chunksTask = async { chunks.searchChunks(question, bookID, maxChapter) }
        val eventsTask = async { events.recentEvents(bookID, maxChapter, 5) }

        rerank(
            RetrievalContext(
                safeRecap = recapTask.await(),
                characterCards = charactersTask.await(),
                recentEvents = eventsTask.await(),
                quotes = chunksTask.await(),
                facts = factsTask.await()
            ),
            currentChapterIndex = currentChapterIndex,
            spoilerBoundary = maxChapter
        )
    }

    /** Drops anything past the spoiler boundary (defense-in-depth) and sorts by proximity. */
    private fun rerank(
        ctx: RetrievalContext,
        currentChapterIndex: Int,
        spoilerBoundary: Int
    ): RetrievalContext {
        val filteredCharacters = ctx.characterCards
            .filter { it.firstChapterIndex <= spoilerBoundary }
            .sortedBy { abs(currentChapterIndex - it.firstChapterIndex) }

        val filteredFacts = ctx.facts
            .filter { it.chapterIndex <= spoilerBoundary && it.spoilerLevel <= 1 }
            .sortedWith(
                compareBy({ it.spoilerLevel }, { abs(currentChapterIndex - it.chapterIndex) })
            )

        val filteredChunks = ctx.quotes
            .filter { it.spoilerLevel <= 1 }
            .sortedBy { abs(currentChapterIndex - it.index) }

        val filteredEvents = ctx.recentEvents
            .filter { it.chapterIndex <= spoilerBoundary && it.spoilerLevel <= 1 }

        return RetrievalContext(
            safeRecap = ctx.safeRecap,
            characterCards = filteredCharacters,
            recentEvents = filteredEvents,
            quotes = filteredChunks,
            facts = filteredFacts
        )
    }
}
