import Foundation
import SharedModels

public struct RetrievalContext: Sendable, Equatable {
    public var safeRecap: Recap?
    public var characterCards: [Character]
    public var recentEvents: [Event]
    public var quotes: [Chunk]
    public var facts: [Fact]

    public init(
        safeRecap: Recap? = nil,
        characterCards: [Character] = [],
        recentEvents: [Event] = [],
        quotes: [Chunk] = [],
        facts: [Fact] = []
    ) {
        self.safeRecap = safeRecap
        self.characterCards = characterCards
        self.recentEvents = recentEvents
        self.quotes = quotes
        self.facts = facts
    }

    public var isEmpty: Bool {
        safeRecap == nil
            && characterCards.isEmpty
            && recentEvents.isEmpty
            && quotes.isEmpty
            && facts.isEmpty
    }
}

public protocol ContextRetrieving: Sendable {
    func context(
        for question: String,
        bookID: BookID,
        currentChapterIndex: Int,
        spoilerBoundary: Int
    ) async throws -> RetrievalContext
}

public protocol CharacterLookupService: Sendable {
    func findCharacters(
        matching query: String,
        bookID: BookID,
        currentChapterIndex: Int
    ) async throws -> [Character]
}

public protocol FactSearchService: Sendable {
    func searchFacts(
        query: String,
        bookID: BookID,
        maxChapterIndex: Int
    ) async throws -> [Fact]
}

public protocol ChunkSearchService: Sendable {
    func searchChunks(
        query: String,
        bookID: BookID,
        maxChapterIndex: Int
    ) async throws -> [Chunk]
}

public protocol RecapLookupService: Sendable {
    func latestRecap(
        bookID: BookID,
        maxChapterIndex: Int
    ) async throws -> Recap?
}

public protocol EventLookupService: Sendable {
    func recentEvents(
        bookID: BookID,
        maxChapterIndex: Int,
        limit: Int
    ) async throws -> [Event]
}

public protocol PromptContextAssembling: Sendable {
    func makePromptContext(
        from retrievalContext: RetrievalContext,
        question: String,
        currentChapterIndex: Int,
        spoilerBoundary: Int
    ) -> String
}
