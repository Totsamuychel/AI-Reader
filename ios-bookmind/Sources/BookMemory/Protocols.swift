import Foundation
import SharedModels

public protocol ChapterChunking: Sendable {
    func makeChunks(for chapter: Chapter, rawText: String) -> [Chunk]
}

public protocol SafeRecapBuilding: Sendable {
    func buildRecap(for chapter: Chapter, chunks: [Chunk]) -> Recap
}

public protocol FactIndexing: Sendable {
    func extractFacts(from chapter: Chapter, rawText: String) -> [Fact]
}

public protocol CharacterDetecting: Sendable {
    /// Detects mentioned proper nouns / character names in a chapter.
    func detectCharacters(
        in chapter: Chapter,
        rawText: String,
        existing: [Character]
    ) -> [Character]
}

public protocol MemoryWriting: Sendable {
    func writeChunks(_ chunks: [Chunk], chapterIndex: Int) async throws
    func writeRecap(_ recap: Recap) async throws
    func writeFacts(_ facts: [Fact]) async throws
    func upsertCharacters(_ characters: [Character]) async throws
    func loadCharacters(for bookID: BookID) async throws -> [Character]
}

public protocol BookIngesting {
    func ingest(book: Book, chapters: [Chapter]) async throws
}
