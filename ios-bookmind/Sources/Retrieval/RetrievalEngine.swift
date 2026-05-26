import Foundation
import SharedModels

public final class RetrievalEngine: ContextRetrieving {
    private let characters: CharacterLookupService
    private let facts: FactSearchService
    private let chunks: ChunkSearchService
    private let recaps: RecapLookupService
    private let events: EventLookupService

    public init(
        characters: CharacterLookupService,
        facts: FactSearchService,
        chunks: ChunkSearchService,
        recaps: RecapLookupService,
        events: EventLookupService
    ) {
        self.characters = characters
        self.facts = facts
        self.chunks = chunks
        self.recaps = recaps
        self.events = events
    }

    public func context(
        for question: String,
        bookID: BookID,
        currentChapterIndex: Int,
        spoilerBoundary: Int
    ) async throws -> RetrievalContext {
        let maxChapter = min(spoilerBoundary, currentChapterIndex)

        async let recapTask = recaps.latestRecap(bookID: bookID, maxChapterIndex: maxChapter)
        async let charactersTask = characters.findCharacters(
            matching: question,
            bookID: bookID,
            currentChapterIndex: maxChapter
        )
        async let factsTask = facts.searchFacts(
            query: question,
            bookID: bookID,
            maxChapterIndex: maxChapter
        )
        async let chunksTask = chunks.searchChunks(
            query: question,
            bookID: bookID,
            maxChapterIndex: maxChapter
        )
        async let eventsTask = events.recentEvents(
            bookID: bookID,
            maxChapterIndex: maxChapter,
            limit: 5
        )

        let (recap, chars, fts, chnks, evs) = try await (
            recapTask, charactersTask, factsTask, chunksTask, eventsTask
        )

        return rerank(
            RetrievalContext(
                safeRecap: recap,
                characterCards: chars,
                recentEvents: evs,
                quotes: chnks,
                facts: fts
            ),
            currentChapterIndex: currentChapterIndex,
            spoilerBoundary: maxChapter
        )
    }

    /// Drops anything beyond spoiler boundary (defense-in-depth) and
    /// sorts by proximity to current chapter / spoiler level.
    private func rerank(
        _ ctx: RetrievalContext,
        currentChapterIndex: Int,
        spoilerBoundary: Int
    ) -> RetrievalContext {
        let filteredCharacters = ctx.characterCards
            .filter { $0.firstChapterIndex <= spoilerBoundary }
            .sorted { lhs, rhs in
                let lDistance = abs(currentChapterIndex - lhs.firstChapterIndex)
                let rDistance = abs(currentChapterIndex - rhs.firstChapterIndex)
                return lDistance < rDistance
            }

        let filteredFacts = ctx.facts
            .filter { $0.chapterIndex <= spoilerBoundary && $0.spoilerLevel <= 1 }
            .sorted { lhs, rhs in
                if lhs.spoilerLevel != rhs.spoilerLevel { return lhs.spoilerLevel < rhs.spoilerLevel }
                return abs(currentChapterIndex - lhs.chapterIndex)
                    < abs(currentChapterIndex - rhs.chapterIndex)
            }

        let filteredChunks = ctx.quotes
            .filter { $0.spoilerLevel <= 1 }
            .sorted { abs(currentChapterIndex - $0.index) < abs(currentChapterIndex - $1.index) }

        let filteredEvents = ctx.recentEvents
            .filter { $0.chapterIndex <= spoilerBoundary && $0.spoilerLevel <= 1 }

        return RetrievalContext(
            safeRecap: ctx.safeRecap,
            characterCards: filteredCharacters,
            recentEvents: filteredEvents,
            quotes: filteredChunks,
            facts: filteredFacts
        )
    }
}
